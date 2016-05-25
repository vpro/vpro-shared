package nl.vpro.ektorp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.impl.NameConventions;
import org.ektorp.support.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.util.ThreadPools;

/**
 * @author Michiel Meeuwissen
 * @since 0.43
 */
public class ViewRefresher implements Runnable {

    private final Logger LOG = LoggerFactory.getLogger(ViewRefresher.class);

    final List<String> views = new ArrayList<>();
    final CouchDbConnector couchDbConnector;
    final int scheduleRate;
    String designDocumentId;


    public ViewRefresher(CouchDbConnector masterDb, int scheduleRate, String designDocumentId, Class<?>... classNames) {
        this.couchDbConnector = masterDb;
        this.scheduleRate = scheduleRate;
        this.designDocumentId = designDocumentId;
        List<Class<?>> classes = Arrays.stream(classNames).collect(Collectors.toList());
        init(classes);
    }

    public ViewRefresher(CouchDbConnector masterDb, int scheduleRate, Class<?> documents, Class<?>... clas) {
        this.couchDbConnector = masterDb;
        this.scheduleRate = scheduleRate;
        this.designDocumentId = NameConventions.designDocName(documents);
        List<Class<?>> classes = Arrays.stream(clas).collect(Collectors.toList());
        init(classes);
    }
    public void init(List<Class<?>> classes) {
        ThreadPools.backgroundExecutor.scheduleAtFixedRate(this, 0, scheduleRate, TimeUnit.MINUTES);
        for (Class<?> c : classes) {
            for (Method m : c.getMethods()) {
                View view = m.getAnnotation(View.class);
                if (view != null) {
                    String name = view.name();
                    if (name.length() > 0) {
                        views.add(name);
                    }
                }
            }
            try {
                Field field = c.getDeclaredField(designDocumentId);
                field.setAccessible(true);
                Class<?> t = field.getType();
                if (t == String.class) {
                    LOG.info("{} is a constant in {}, taking its value {}", designDocumentId, c, field.get(null));
                    designDocumentId = (String) field.get(null);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        LOG.info("Refreshing views {}: {}", couchDbConnector.getDatabaseName(), views);
    }

    @Override
    public void run() {
        LOG.info("Now refreshing views {} {}", couchDbConnector.getDatabaseName(), views);
        for (String view : views) {
            try {
                ViewQuery unstale = new ViewQuery()
                    .viewName(view)
                    .designDocId(designDocumentId)
                    .staleOkUpdateAfter();
                couchDbConnector.queryView(unstale);
                LOG.info("Refreshed {}", view);
                ViewQuery unstaleReduce = new ViewQuery()
                    .viewName(view)
                    .designDocId(designDocumentId)
                    .reduce(true)
                    .staleOkUpdateAfter();
                LOG.info("Refreshed reduces {}", view);
                couchDbConnector.queryView(unstaleReduce);
            } catch (Exception e) {
                LOG.warn(e.getClass().getName() + " " + e.getMessage());
            }
        }
    }

}

package nl.vpro.test.jupiter;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.*;
import org.meeuw.math.statistics.StatisticalLong;
import org.meeuw.math.temporal.UncertainTemporal;
import org.opentest4j.TestAbortedException;

import nl.vpro.logging.Log4j2Helper;


/**
 * Simple extension that times every individual test in the class.
 * <p >
 * It will be reported after each test, and again after all tests of the class have run. There is also a duration reported for all tests together then.
 * @since 5.2
 */
@SuppressWarnings("unchecked")
@Log4j2
public class TimingExtension implements
    BeforeTestExecutionCallback, BeforeAllCallback,
    AfterTestExecutionCallback, AfterAllCallback {


    private static final String START_All_TIME = "ALL";
    private static final String START_TIMES = "START_TIMES";
    private static final String DURATIONS = "DURATIONS";
    private static final String REPETITIONS = "REPETITIONS";



    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = getStore(context);
        store.get(START_TIMES, Map.class).put(getKey(context), System.nanoTime());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = getStore(context);
        Optional<RepeatedTest> repeated = isRepeatedTest(context);
        if (context.getExecutionException().isPresent()) {
            if (context.getExecutionException().get() instanceof TestAbortedException) {
                log.debug("Ignored aborted test");
                return;
            }
        }

        String key = getKey(context);

        long startTime = (long) store.get(START_TIMES, Map.class).remove(key);
        Duration duration = Duration.ofNanos(System.nanoTime() - startTime);

        if (repeated.isEmpty() || log.isDebugEnabled()) {
            Map<String, Object> durationsMap = store.get(DURATIONS, Map.class);
            durationsMap.put(key, duration);
        }
        repeated.ifPresent(annotation -> {
            String rk = getRepetitionKey(context, annotation);
            Map<String, Object> repetitionsMap = store.get(REPETITIONS, Map.class);

            StatisticalLong statisticalLong = (StatisticalLong) repetitionsMap.computeIfAbsent(rk, (kk) -> new StatisticalLong(UncertainTemporal.Mode.DURATION));
            statisticalLong.enter(duration);
        });

        Log4j2Helper.debugOrInfo(log, repeated.isEmpty(), "{} took {}", key, duration.truncatedTo(ChronoUnit.MILLIS));
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = getStore(context);
        store.put(START_All_TIME, System.nanoTime());
        store.put(START_TIMES, Collections.synchronizedMap(new HashMap<>()));
        store.put(DURATIONS, Collections.synchronizedMap(new HashMap<>()));
        store.put(REPETITIONS, Collections.synchronizedMap(new HashMap<>()));

    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = getStore(context);
        long startTime = store.remove(START_All_TIME, long.class);
        Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
        Map<String, Duration> durations = store.get(DURATIONS, Map.class);
        Map<String, Long> startTimes = store.get(START_TIMES, Map.class);


        Map<String, StatisticalLong> repetitions = store.get(REPETITIONS, Map.class);
        for (Map.Entry<String , StatisticalLong> e: repetitions.entrySet()) {
            log.info("{}x:\t{}: {}", e.getValue().getCount(), e.getValue().durationValue(), e.getKey());
            durations.put(e.getKey(), e.getValue().durationValue());
        }

        log.info("All {} took {}.", context.getRequiredTestClass(), duration.truncatedTo(ChronoUnit.SECONDS));
        AtomicInteger number = new AtomicInteger(0);
        durations.entrySet().stream()
            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
            .forEachOrdered(e -> {
                int n = number.getAndIncrement();
                log.info("{}:\t{}: {}", n + (n == 0 ? " (slowest)" : ""), e.getValue(), e.getKey());
            });
    }



    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass(), TimingExtension.class));
    }

    private String getKey(ExtensionContext context) {
        return context.getDisplayName();
    }
    private String getRepetitionKey(ExtensionContext context, RepeatedTest repeatedTest) {
        return "average(" + context.getRequiredTestMethod().getName() + " " + repeatedTest.name() + ")";
    }


    protected Optional<RepeatedTest> isRepeatedTest(ExtensionContext context)  {
        return context.getTestMethod()
            .map(m -> m.getAnnotation(RepeatedTest.class))
            .filter(Objects::nonNull);

    }
}


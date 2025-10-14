package nl.vpro.monitoring.web;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import nl.vpro.monitoring.endpoints.Setup;

/**
 *
 * @since 5.12
 */
@Slf4j
public class MonitoringBootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        // Set parent context if available
        WebApplicationContext rootCtx = (WebApplicationContext)
            sce.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (rootCtx != null) {
            ctx.setParent(rootCtx);
        }
        ctx.setConfigLocation(Setup.class.getName());
        ctx.setServletContext(sce.getServletContext());


        ctx.refresh();
        sce.getServletContext().setAttribute("monitoringContext", ctx);


        log.debug("BEANS {}", List.of(ctx.getBeanDefinitionNames()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        AnnotationConfigWebApplicationContext ctx =
            (AnnotationConfigWebApplicationContext) sce.getServletContext().getAttribute("monitoringContext");
        if (ctx != null) {
            ctx.close();
        }
    }
}

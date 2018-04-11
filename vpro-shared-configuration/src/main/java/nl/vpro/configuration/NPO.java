package nl.vpro.configuration;

import java.io.File;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

/**
 * @author Michiel Meeuwissen
 */
public class NPO implements ServletContextAware {


    private ServletContext sx;

    NPO(ServletContext sx) {
        this.sx = sx;
    }
    NPO() {

    }


    File getDirectory(ServletContext sx) {
        File dir = new File(sx.getRealPath("/"));
        if (!dir.exists()) { // DRS fixed, dir will never be null ofcourse...
            dir = File.listRoots()[0];
        }
        File parent = dir.getParentFile();
        if (parent == null) parent = dir;
        return parent;
    }

    public File getConfigDirectory() {
        return new File(getDirectory(sx), "config");
    }

    public File getDataDirectory() {
        return new File(getDirectory(sx).toString().replace("/ro/", "/rw/"), "data");
    }


    @Override
    public void setServletContext(ServletContext servletContext) {
        this.sx = servletContext;

    }
}

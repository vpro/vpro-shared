module nl.vpro.shared.logging {
    exports nl.vpro.logging.simple;
    exports nl.vpro.logging;
    exports nl.vpro.logging.mdc;
    exports nl.vpro.logging.jmx;
    exports nl.vpro.logging.filter;

    requires org.apache.commons.lang3;

    requires static lombok;
    requires static jakarta.servlet;
    requires static spring.security.core;
    requires static spring.context;
    requires static org.apache.logging.log4j.core;
    requires static org.apache.logging.log4j;
    requires static org.checkerframework.checker.qual;
    requires static java.logging;
    requires static flogger;
    requires static org.slf4j;

}

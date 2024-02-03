module nl.vpro.shared.util {

    exports nl.vpro.jmx;
    exports nl.vpro.util;
    exports nl.vpro.util.locker;
    exports nl.vpro.util.picocli;


    requires nl.vpro.shared.logging;
    requires com.google.common;
    requires org.apache.commons.lang3;
    requires org.meeuw.functional;
    requires org.apache.commons.text;
    requires org.apache.commons.io;

    requires static lombok;
    requires static java.management;
    requires static org.slf4j;
    requires static org.meeuw.math.statistics;
    requires static org.meeuw.math;
    requires static org.checkerframework.checker.qual;
    requires static jakarta.inject;
    requires static jakarta.xml.bind;

    requires static org.apache.logging.log4j;

    requires static java.net.http;
    requires static org.jsoup;
    requires static org.junit.platform.launcher;
    requires static info.picocli;
    requires static java.desktop;
    requires static jakarta.annotation;
    requires static java.sql;


}

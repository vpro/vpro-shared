module nl.vpro.shared.configuration {
    requires nl.vpro.shared.logging;
    requires static spring.context;
    requires org.slf4j;
    requires static lombok;
    requires nl.vpro.shared.util;
    requires spring.core;
    requires org.checkerframework.checker.qual;
    requires spring.beans;
    requires spring.expression;
    requires jakarta.inject;
    requires java.management;
    requires org.apache.groovy;
    requires org.apache.commons.lang3;
    requires commons.configuration;
}

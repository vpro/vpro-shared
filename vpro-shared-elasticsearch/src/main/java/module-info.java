module nl.vpro.shared.elasticsearch {
    requires org.checkerframework.checker.qual;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.io;
    requires nl.vpro.shared.jackson;
    requires nl.vpro.shared.util;
    requires java.management;
    exports nl.vpro.elasticsearch;
}

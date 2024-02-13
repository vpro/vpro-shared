module nl.vpro.shared.validation {
    exports nl.vpro.validation;

    requires nl.vpro.shared.util;
    requires jakarta.validation;
    requires org.apache.commons.lang3;
    requires static lombok;

    requires org.slf4j;
}

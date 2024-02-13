@XmlSchema(namespace = "urn:test:1234",
    xmlns = {
        @XmlNs(prefix = "", namespaceURI = "urn:test:1234")
    },
    elementFormDefault = jakarta.xml.bind.annotation.XmlNsForm.QUALIFIED,
    attributeFormDefault = jakarta.xml.bind.annotation.XmlNsForm.UNQUALIFIED
) package nl.vpro.test.util.jaxb.test;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlSchema;

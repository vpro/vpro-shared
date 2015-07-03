@XmlSchema(namespace = "urn:test:1234",
    xmlns = {
        @XmlNs(prefix = "", namespaceURI = "urn:test:1234")
    },
    elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED,
    attributeFormDefault = javax.xml.bind.annotation.XmlNsForm.UNQUALIFIED
) package nl.vpro.test.util.jaxb.test;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

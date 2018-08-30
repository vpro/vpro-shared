# JAXB related utilities

Mainly several kinds of `javax.xml.bind.annotation.adapters.XmlAdapter`s, mostly related to how we use to want to serialize java.time objects. 
See also https://bugs.openjdk.java.net/browse/JDK-8042456, but sometimes we also predate jaxb support, or want to be a bit more lenient or so.

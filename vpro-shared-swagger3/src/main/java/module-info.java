module nl.vpro.shared.swagger {
    requires jakarta.servlet;
    requires static lombok;
    requires mihxil.json.grep;
    requires nl.vpro.shared.web;
    requires io.swagger.v3.oas.models;
    requires io.swagger.v3.jaxrs2;
    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires io.swagger.v3.oas.integration;
    requires io.swagger.v3.oas.annotations;
    requires jakarta.annotation;
    requires static org.checkerframework.checker.qual;
    requires com.fasterxml.jackson.core;
    requires io.swagger.v3.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires spring.beans;
    requires nl.vpro.shared.util;
    requires spring.context;
    requires spring.core;
    requires nl.vpro.shared.jackson;
    requires com.fasterxml.jackson.module.jakarta.xmlbind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires nl.vpro.shared.rs;
}

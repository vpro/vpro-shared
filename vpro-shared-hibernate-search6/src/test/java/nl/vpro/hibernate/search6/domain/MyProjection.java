package nl.vpro.hibernate.search6.domain;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

import java.time.Instant;

@ProjectionConstructor
public record MyProjection(Instant instant,  Object myEnum) {
}

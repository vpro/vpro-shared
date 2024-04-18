package nl.vpro.hibernate.search6.domain;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

import java.time.Instant;
import java.util.List;

@ProjectionConstructor
public record MyProjection(
    Instant instant,
    MyEnum myEnum,
    Boolean myBoolean,
    @FieldProjection(path = "subObjectJson") SubObject subObject,

    @FieldProjection(path = "subObjectsJson") List<SubObject> subObjects) {
}



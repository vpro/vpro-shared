package nl.vpro.hibernate.search6.domain;

import lombok.*;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@lombok.Builder
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column
    private String text;

    @Enumerated(EnumType.STRING)
    @Column
    private MyEnum myEnum;

    @Column
    private Instant instant;

    @ElementCollection
    private List<String> list;

    @OneToOne
    private SubObject subObject;

    @Column
    private Boolean myBoolean;


}

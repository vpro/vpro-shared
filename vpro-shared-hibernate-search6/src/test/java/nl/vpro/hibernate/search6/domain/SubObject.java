package nl.vpro.hibernate.search6.domain;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
@ToString
@Entity
@Getter
@Setter
public class SubObject {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @jakarta.persistence.Column(name = "id", nullable = false)
    private Long id;


    @Column
    public String a;
}

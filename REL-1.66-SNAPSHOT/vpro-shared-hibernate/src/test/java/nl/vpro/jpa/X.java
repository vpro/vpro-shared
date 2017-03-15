package nl.vpro.jpa;

import javax.persistence.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.55
 */
@Entity
public class X  implements I {
    @Id
    @SequenceGenerator(name = "hibernate_sequences", sequenceName = "hibernate_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequences")
    protected Long id;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public X setId(Long id) {
        this.id = id;
        return this;
    }


}

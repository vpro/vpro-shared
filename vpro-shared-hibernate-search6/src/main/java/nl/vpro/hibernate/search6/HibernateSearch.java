package nl.vpro.hibernate.search6;

/**
 * Just marks a method as using Hibernate Search. Just for documentation purposes.
 */
public @interface HibernateSearch {

    State implemented() default State.TODO;

    enum State {
        TODO,
        READY,
        BUSY
    }
}

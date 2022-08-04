package nl.vpro.rs;

/**
 * Annotating a resteasy method with this, will cause the produced xml to be a 'fragment', i.e. it will lack
 * the {@code <?xml } prefix.
 * @since 2.33.0
 */
public @interface XmlFragment {
}

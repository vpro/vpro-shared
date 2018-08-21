# GUICE utilities

Provides some guice related utilities

- OptionalModule Uses `OptionalBinder` to provide <code>null</code>'s or <code>@DefaultValue</code>'s for <code>@Named</code> parameters of the type <code>Optional</code>
- A DurationConvertor. To be able to inject java.time.Duration

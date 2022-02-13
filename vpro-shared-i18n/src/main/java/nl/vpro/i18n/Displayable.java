/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.i18n;

import java.util.Locale;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A displayable has a {@link #getDisplayName()} method, to display the object as a single string (in {@link Locales#getDefault()}) to the user.
 *
 * This interface contains only default methods, which call each other.
 * Implementations must override at least one, either {@link #getDisplayName()} if no localization is necessary or {@link #getDisplayName(Locale)} if it is.
 *
 * @author Roelof Jan Koekoek
 * @since 2.30
 */
public interface Displayable {

    Displayable NULL = new Displayable() {
        @Override
        public String getDisplayName() {
            return null;
        }
    };

    @NonNull
    static Displayable of(@Nullable Displayable d) {
        return d == null ? NULL : d;
    }

    /**
     * Returns the display value in the default locale.
     *
     */
    default String getDisplayName() {
        return getDisplayName(Locales.getDefault()).getValue();
    }

    /**
     * Returns a displayable name for this item in the given Locale, or the default locale ({@link Locales#getDefault()}) if not available or not implemented
     * @since 5.11
     */
    default LocalizedString getDisplayName(Locale locale) {
        return LocalizedString.of(getDisplayName(), Locales.getDefault());
    }


    /**
     * Returns the plural of the display name, if implemented. Otherwise {@link Optional#empty()}
     * @since 5.11
     */
    @NonNull
    default Optional<LocalizedString> getPluralDisplayName(Locale locale) {
        return Optional.empty();
    }


    /**
     * Returns {@link #getDisplayName(Locale)} for the default locale {@link Locales#getDefault()}
     * @since 5.11
     */
    default Optional<LocalizedString> getPluralDisplayName() {
        return getPluralDisplayName(Locales.getDefault());
    }

    /**
     * An url for an icon associated with this displayable object.
     *
     * It may be that this to be interpreted relative to the current 'context path'.
     */
    default Optional<String> getIcon() {
        return Optional.empty();
    }
    /**
     * An url for an icon associated with this displayable object.
     *
     * It may be that this to be interpreted relative to the current 'context path'.
     */
    default Optional<String> getIconClass() {
        return Optional.empty();
    }

    /**
     * Sometimes displayable values are deprecated or experimental. Their value is possible but they should not be
     * displayed in situations where the value is not yet present. Then this can return false.
     */
    @JsonIgnore
    default boolean display() {
        return true;
    }


}

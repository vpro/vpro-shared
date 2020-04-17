/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * See https://jira.vpro.nl/browse/MSE-1372
 *
 * @author Roelof Jan Koekoek
 * @since 1.5
 */
public class TextUtil {

    Whitelist whitelist = Whitelist.none().addTags("\n");

    /**
     * Reusable pattern for matching text against illegal characters
     */
    public static final Pattern ILLEGAL_PATTERN = Pattern.compile("<.*>|&#\\d{2,4};|&[\\w]{2,8};|\\u2028");

    /**
     * Checks if given text input complies to POMS standard.
     * @see #ILLEGAL_PATTERN for a rough check
     */
    public static boolean isValid(String input) {
        Matcher matcher = ILLEGAL_PATTERN.matcher(input);
        if(!matcher.find()) {
            return true;
        }
        return Objects.equals(normalizeWhiteSpace(input), normalizeWhiteSpace(sanitize(input)));
    }

    public static String normalizeWhiteSpace(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Replaces all line separators with a single white space character. The line separator character (\u2028) is
     * forbidden in most modern browsers. These browsers won't render any text containing this character.
     */
    public static String replaceLineBreaks(String input) {
        return input != null ? input.replace('\u2028', ' ') : null;
    }

    /**
     * Replaces all non breaking space characters (\u00A0) with a normal white space character.
     */
    public static String replaceNonBreakingSpace(String input) {
        return input != null ? input.replace('\u00A0', ' ') : null;
    }

    public static String replaceOdd(String input) {
        if (input == null) {
            return null;
        }
        input = input.replaceAll("\\p{Cc}+", " ");

        return replaceLineBreaks(
            replaceNonBreakingSpace(input)

        );
    }

    /**
     * Replaces all non breaking space characters (\u00A0) with a normal white space character.
     */
    public static String replaceHtmlEscapedNonBreakingSpace(String input) {
        return input != null ? input.replace("&nbsp;", " ") : null;
    }

    /**
     * Un-escapes all html escape characters. For example: Replaces "&amp;amp;" with "&amp;".
     */
    public static String unescapeHtml(String input) {
        return input != null ? StringEscapeUtils.unescapeHtml4(input.replace("&nbsp;", " ")) : null;
    }

    /**
     * Strips html like tags from the input. All content between tags, even non-html content is being removed.
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return null;
        }

        return Jsoup.clean(input, Whitelist.none());

    }

    /**
     * Aggressively removes all tags and escaped HTML characters from the given input and replaces some characters that
     * might lead to problems for end users.
     */
    @Nullable
    public static String sanitize(@Nullable String input) {
        if (input == null) {
            return null;
        }
        // recursive, because sometimes a sanitize operation results new html (see nl.vpro.util.TextUtilTest.testSanitizeIframe())
        String sanitized = _sanitize(input);
        while (! Objects.equals(sanitized, input)) {
            input = sanitized;
            sanitized = _sanitize(input);
        }
        return sanitized;

    }

    private static String _sanitize(String input) {
        return unescapeHtml(
            stripHtml(
                replaceOdd(
                    replaceHtmlEscapedNonBreakingSpace(
                        unescapeHtml(input)
                    )
                )
            )
        );
    }


    private static final Set<Pattern> DUTCH_PARTICLES =
        new HashSet<>(
            Arrays.asList(
                getPattern("de"),
                getPattern("het"),
                getPattern("een")
                /*, "'t", "'n" ?*/
            ));
    private static Pattern getPattern(String particle) {
        return Pattern.compile("(?i)^(" + particle + ")\\b.+");
    }

    public static String getLexico(String title, Locale locale) {
        // Deze code staat ook als javascript in media-server/src/main/webapp/vpro/media/1.0/util/format.js
        if ("nl".equals(locale.getLanguage())) {
            for (Pattern particle : DUTCH_PARTICLES) {
                Matcher matcher = particle.matcher(title);
                if (matcher.matches()) {
                    int matchLength = matcher.group(1).length();
                    String start = title.substring(0, matchLength);
                    boolean uppercase = title.toUpperCase().equals(title);
                    StringBuilder b = new StringBuilder(title.substring(matchLength).trim()).append(", ").append(uppercase ? start.toUpperCase() : start.toLowerCase());
                    if (Character.isUpperCase(start.charAt(0))) {
                        b.setCharAt(0, Character.toTitleCase(b.charAt(0)));
                    }
                    return b.toString();
                }
            }
            return title;
        } else {
            return title;
        }
    }

    public static String select(String... options) {
        for(String option : options) {
            if(option != null) {
                return option;
            }
        }
        return null;
    }

    public static String truncate(String text, int max) {
        return truncate(text, max, false);
    }


    static int lastIndexOfWhiteSpace(CharSequence s) {
        for (int i = s.length() - 1; i > 0; i--) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }


        }
        return -1;
    }
    public static String truncate(String text, int max, boolean ellipses) {
        if (text == null) {
            return null;
        }
        boolean truncated = false;
        while(text.length() > max) {
            {
                int end = text.lastIndexOf('.');
                if (end > 0 && end < text.length() - 1) {
                    text = text.substring(0, end + 1);
                    truncated = true;
                    continue;
                }
            }
            {
                int end = lastIndexOfWhiteSpace(text);
                if (end > 0 && end < text.length() - 1) {
                    text = text.substring(0, end);
                    truncated = true;
                    continue;
                }
            }
            text = text.substring(0, max);
        }
        if (ellipses && truncated) {
            return text + "...";
        } else {
            return text;
        }
    }

    /**
     * Gives a representation of the string which is completely 'stroke through' (using unicode control characters)
     * @since 2.11
     */
    public static String strikeThrough(CharSequence s) {
        return controlEach(s, '\u0336');
    }

    /**
     * Gives a representation of the string which is completely 'underlined' (using unicode control characters)
     * @since 2.11
     */
    public static String underLine(CharSequence s) {
        return controlEach(s, '\u0332');
    }

    /**
     * Gives a representation of the string which is completely 'double underlined' (using unicode control characters)
     * @since 2.11
     */
    public static String underLineDouble(CharSequence s) {
        return controlEach(s, '\u0333');
    }

    /**
     * Gives a representation of the string which is completely 'overlined' (using unicode control characters)
     * @since 2.11
     */
    public static String overLine(CharSequence s) {
        return controlEach(s, '\u0305');
    }

    /**
     * Gives a representation of the string which is completely 'double overlined' (using unicode control characters)
     * @since 2.11
     */
    public static String overLineDouble(CharSequence s) {
        return controlEach(s, '\u033f');
    }

    /**
     * Gives a representation of the string which is completely 'diaeresised under' (using unicode control characters)
     * @since 2.11
     */
    public static String underDiaeresis(CharSequence s) {
        return controlEach(s, '\u0324');
    }

    /**
     * @since 2.11
     */
    public static String controlEach(CharSequence s, Character control) {
        if (s == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <  s.length(); i++) {
            result.append(s.charAt(i));
            result.append(control);
        }
        return result.toString();
    }
}

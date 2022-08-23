/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * See <a href="https://jira.vpro.nl/browse/MSE-1372">JIRA</a>
 *
 * @author Roelof Jan Koekoek
 * @since 1.5
 */
public class TextUtil {

    /**
     * Reusable pattern for matching text against illegal characters
     */
    public static final Pattern ILLEGAL_PATTERN = Pattern.compile("<.*>|&#\\d{2,4};|&[\\w]{2,8};|\\u2028");

    private TextUtil() {
        // utility class
    }

    /**
     * Checks if given text input complies to POMS standard.
     * @see #ILLEGAL_PATTERN for a rough check
     */
    public static boolean isValid(@NonNull String input) {
        Matcher matcher = ILLEGAL_PATTERN.matcher(input);
        if(!matcher.find()) {
            return true;
        }
        return Objects.equals(
            normalizeWhiteSpace(input),
            normalizeWhiteSpace(sanitize(input))
        );
    }

    /**
     * Replaces any occurrences of 1 of more white space characters by one space.
     */
    @PolyNull
    public static String normalizeWhiteSpace(@PolyNull String input) {
        if (input == null) {
            return null;
        }

        return input.trim().replaceAll("[\\s\u00a0]+", " ");
    }

    @PolyNull
    public static String normalizeWhiteSpacePreserveNewlines(@PolyNull String input) {
        if (input == null) {
            return null;
        }
        return input.trim()
            .replaceAll("\\r{3,}", "\n\n")
            // space, line tabulation, tab, formfeed,cariage return
            .replaceAll("[ \\t\\x0B\\f]+", " ")
            ;
    }

    /**
     * Replaces all line separators with a single white space character. The line separator character (\u2028) is
     * forbidden in most modern browsers. These browsers won't render any text containing this character.
     */
    @PolyNull
    public static String replaceLineBreaks(@PolyNull String input) {
        return input != null ? input.replace('\u2028', ' ') : null;
    }

    /**
     * Replaces all non breaking space characters (\u00A0) with a normal white space character.
     */
    @PolyNull
    public static String replaceNonBreakingSpace(@PolyNull String input) {
        return input != null ? input.replace('\u00A0', ' ') : null;
    }

    /**
     * Replaces 'odd' characters with a normal white space character.
     */
    @PolyNull
    public static String replaceOdd(@PolyNull String input) {
        if (input == null) {
            return null;
        }
        input = input.replaceAll("\\p{Cc}+", " ");

        return replaceLineBreaks(
            replaceNonBreakingSpace(input)
        );
    }

    /**
     * Replaces all non breaking space entities(&nbsp;) with a normal white space character.
     */
    @PolyNull
    public static String replaceHtmlEscapedNonBreakingSpace(@PolyNull String input) {
        return input != null ? input.replace("&nbsp;", " ") : null;
    }

    /**
     * Un-escapes all html escape entities. For example: Replaces "&amp;amp;" with "&amp;".
     */
    @PolyNull
    public static String unescapeHtml(@PolyNull String input) {
        return input != null ? StringEscapeUtils.unescapeHtml4(
            input.replace("&nbsp;", "\u00a0")
        ) : null;
    }

    /**
     * Strips html like tags from the input. All content between tags, even non-html content is being removed.
     *
     * @see #unhtml(String)  for multiline interpretation
     * @param input a piece of HTML or text containing some HTML markup
     * @return One line representing only the textual content of the input
     */
    @PolyNull
    public static String stripHtml(@PolyNull String input) {
        if (input == null) {
            return null;
        }

        Document jsoupDoc = Jsoup.parse(input);
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        jsoupDoc.outputSettings(outputSettings);
        jsoupDoc.select("br").before(" ");
        jsoupDoc.select("li").before(" ");
        jsoupDoc.select("p").before(" ");
        String str = jsoupDoc.html();
        String strWithNewLines = Jsoup.clean(str, "", Safelist.none(), outputSettings);
        return strWithNewLines.replaceAll(" +", " ").trim();
    }

    /**
     * @param input A piece of HTML
     * @return A piece of plain text, currently only supporting breaks, paragraphs, and lists. Empty paragraphs
     *         and multiple linebreaks are removed.
     * @since 2.30
     */
    @PolyNull
    public static String unhtml(@PolyNull String input) {
        if (input == null) {
            return null;
        }
        Document jsoupDoc = Jsoup.parse(input);
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        jsoupDoc.outputSettings(outputSettings);
        jsoupDoc.select("br").before("\\n");
        jsoupDoc.select("p").before("\\n\\n");
        jsoupDoc.select("li").before("\\n-");
        String str = jsoupDoc.html().replaceAll("\\\\n", "\n");
        String strWithNewLines = Jsoup.clean(str, "", Safelist.none(), outputSettings);
        return unescapeHtml(
            strWithNewLines.trim()
        )
            .replaceAll(" +", " ")
            .replaceAll("\u00a0+", "\u00a0") // no break space
            .replaceAll("\u2028", "\n") // line seperator
            .replaceAll("\n{3,}", "\n\n");
    }

    /**
     * Aggressively removes all tags and escaped HTML characters from the given input and replaces some characters that
     * might lead to problems for end users.
     *
     * @return A single line of text
     */
    @PolyNull
    public static String sanitize(@PolyNull String input) {
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



    @PolyNull
    private static String _sanitize(@PolyNull String input) {
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
        Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(
                getPattern("de"),
                getPattern("het"),
                getPattern("een")
                /*, "'t", "'n" ?*/
            )));
    private static Pattern getPattern(String particle) {
        return Pattern.compile("(?i)^(" + particle + ")\\b.+");
    }

    /**
     * Returns the 'lexicographic' presentation of a title. This means that articles are stripped and moved to the end of the string. Currently only supported for dutch.
     */
    @PolyNull
    public static String getLexico(@PolyNull String title, Locale locale) {
        if (title == null) {
            return null;
        }
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

    /**
     * Selects first non null of the parameters.
     * @deprecated  Can easily be achieved with stream filter {@link Objects#nonNull(Object)}
     */
    @Deprecated
    public static String select(String... options) {
        return Stream.of(options).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @PolyNull
    public static String truncate(@PolyNull String text, int max) {
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

    @PolyNull
    public static String truncate(@PolyNull String text, int max, boolean ellipses) {
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
    @PolyNull
    public static String strikeThrough(@PolyNull CharSequence s) {
        return controlEach(s, '\u0336');
    }

    /**
     * Gives a representation of the string which is completely 'underlined' (using unicode control characters)
     * @since 2.11
     */
    @PolyNull
    public static String underLine(@PolyNull CharSequence s) {
        return controlEach(s, '\u0332');
    }

    /**
     * Gives a representation of the string which is completely 'double underlined' (using unicode control characters)
     * @since 2.11
     */
    @PolyNull
    public static String underLineDouble(@PolyNull CharSequence s) {
        return controlEach(s, '\u0333');
    }

    /**
     * Gives a representation of the string which is completely 'overlined' (using unicode control characters)
     * @since 2.11
     */
    @PolyNull
    public static String overLine(@PolyNull CharSequence s) {
        return controlEach(s, '\u0305');
    }

    /**
     * Gives a representation of the string which is completely 'double overlined' (using unicode control characters)
     * @since 2.11
     */
    @PolyNull
    public static String overLineDouble(@PolyNull CharSequence s) {
        return controlEach(s, '\u033f');
    }

    /**
     * Gives a representation of the string which is completely 'diaeresised under' (using unicode control characters)
     * @since 2.11
     */
    @PolyNull
    public static String underDiaeresis(@PolyNull CharSequence s) {
        return controlEach(s, '\u0324');
    }

    /**
     * @since 2.11
     */
    @PolyNull
    public static String controlEach(@PolyNull CharSequence s, @NonNull Character control) {
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

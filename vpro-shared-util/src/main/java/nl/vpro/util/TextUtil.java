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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

/**
 * See <a href="https://jira.vpro.nl/browse/MSE-1372">JIRA</a>
 *
 * @author Roelof Jan Koekoek
 * @since 1.5
 */
@SuppressWarnings("UnnecessaryUnicodeEscape")
public class TextUtil {

    /**
     * Reusable pattern for matching text against illegal characters
     */
    public static final Pattern ILLEGAL_PATTERN = Pattern.compile("<.*>|&#\\d{2,4};|&[\\w]{2,8};|\\u2028");

    private TextUtil() {
        // utility class
    }

    private static final Set<String> ALL = Set.of(
        "a",	//Specific a anchor (Hyperlink)
        "abbr",//	Describes an abbreviation (acronyms)
        "acronym",	//Describes an acronyms	REMOVE
        "address",	//Describes an address information
        "applet",	//Embedding an applet in HTML document	REMOVE
        "area",	//Defines an area in an image map
        "article",	//Defines an article	NEW
        "aside",	//Describes contain set(or write) on aside place in page contain	NEW
        "audio",	//Specific audio content	NEW
        "b",	//Specific text weight bold
        "base",	//Define a base URL for all the links with in a web page
        "basefont",	//Describes a default font color, size, face in a document	REMOVE
        "bb",	//Define browser command, that command invoke as per client action	NEW/ REMOVE
        "bdo",	//Specific direction of text display
        "big",	//Defines a big text	REMOVE
        "blockquote",	//Specifies a long quotation
        "body",	//Defines a main section(body) part in HTML document
        "br",	//Specific a single line break
        "button",	//Specifies a press/push button
        "canvas",	//Specifies the display graphics on HTML web documment	NEW
        "caption",	//Define a table caption
        "center",	//Specifies a text is display in center align	REMOVE
        "cite",	//Specifies a text citation
        "code",	//Specifies computer code text
        "col",	//Specifies a each column within a "colgroup", element in table
        "colgroup",	//Defines a group of one or more columns inside table
        "command",	//Define a command button, invoke as per user action	NEW
        "datagrid", //	Define a represent data in datagrid either list wise or tree wise	NEW/ REMOVE
        "datalist", //	Define a list of pre-defined options surrounding "input", // tag	NEW
        "dd", //	Defines a definition description in a definition list
        "del", //	Specific text deleted in web document
        "details", //	Define a additional details hide or show as per user action	NEW
        "dfn", //	Define a definition team
        "dialog", //	Define a chat conversation between one or more person	NEW/ REMOVE
        "dir", //	Define a directory list	REMOVE
        "div", //	Define a division part
        "dl", //	Define a definition list
        "dt", //	Define a definition team
        "em", //	Define a text is emphasize format
        "embed", //	Define a embedding external application using a relative plug-in	NEW
        "eventsource", //	Defines a source of event generates to remote server	NEW/ REMOVE
        "fieldset", //	Defines a grouping of related form elements
        "figcaption", //	Represents a caption text corresponding with a figure element	NEW
        "figure", //	Represents self-contained content corresponding with a "figcaption", // element	NEW
        "font", //	Defines a font size, font face and font color for its text	REMOVE
        "footer", //	Defines a footer section containing details about the author, copyright, contact us, sitemap, or links to related documents.	NEW
        "form", //	Defines a form section that having interactive input controls to submit form information to a server.
        "frame", //	Defines frame window.	REMOVE
        "frameset", //	Used to holds one or more "frame", // elements.	REMOVE
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6", // to "h6", //	Defines a Headings level from 1 to 6 different sizes.
        "head", //	Defines header section of HTML document.
        "header", //	Defines as a container that hold introductory content or navigation links.	NEW
        "hgroup", //	Defines the heading of a section that hold the h1 to h6 tags.	NEW/ REMOVE
        "hr /", //	Represent a thematic break between paragraph-level tags. It is typically draw horizontal line.
        "html", //	Define a document is a HTML markup language
        "i", //	Defines a italic format text
        "iframe", //	Defines a inline frame that embedded external content into current web document.
        "img", //	Used to insert image into a web document.
        "input", //	Define a get information in selected input
        "ins", //	Used to indicate text that is inserted into a page and indicates changes to a document.
        "isindex", //	Used to create a single line search prompt for querying the contents of the document.	REMOVE
        "kbd", //	Used to identify text that are represents keyboard input.
        "keygen", //	Used to generate signed certificate, which is used to authenticate to services.	NEW/ REMOVE
        "label", //	Used to caption a text label with a form "input", // element.
        "legend", //	Used to add a caption (title) to a group of related form elements that are grouped together into the "fieldset", // tag.
        "li", //	Define a list item either ordered list or unordered list.
        "link", //	Used to load an external stylesheets into HTML document.
        "map", //	Defines an clickable image map.
        "mark", //	Used to highlighted (marked) specific text.	NEW
        "menu", //	Used to display a unordered list of items/menu of commands.
        "meta", //	Used to provide structured metadata about a web page.
        "meter", //	Used to measure data within a given range.	NEW
        "nav", //	Used to defines group of navigation links.	NEW
        "noframes", //	Used to provide a fallback content to the browser that does not support the "frame", // element.	REMOVE
        "noscript", //	Used to provide an fall-back content to the browser that does not support the JavaScript.
        "object", //	Used to embedded objects such as images, audio, videos, Java applets, and Flash animations.
        "ol", //	Defines an ordered list of items.
        "optgroup", //	Used to create a grouping of options, the related options are grouped under specific headings.
        "option", //	Represents option items within a "select", //, "optgroup", // or "datalist", // element.
        "output", //	Used for representing the result of a calculation.	NEW
        "p", //	Used to represents a paragraph text.
        "param", //	Provides parameters for embedded object element.
        "pre", //	Used to represents preformatted text.
        "progress", //	Represents the progress of a task.	NEW
        "q", //	Represents the short quotation.
        "rp", //	Used to provide parentheses around fall-back content to the browser that does not support the ruby annotations.	NEW
        "rt", //	Specifies the ruby text of ruby annotation.	NEW
        "ruby", //	Used to represents a ruby annotation.	NEW
        "s", //	Text display in strikethrough style.
        "samp", //	Represents text that should be interpreted as sample output from a computer program.
        "script", //	Defines client-side JavaScript.
        "section", //	Used to divide a document into number of different generic section.	NEW
        "select", //	Used to create a drop-down list.
        "small", //	Used to makes the text one size smaller.
        "source", //	Used to specifies multiple media resources.	NEW
        "span", //	Used to grouping and applying styles to inline elements.
        "strike", //	Represents strikethrough text.	REMOVE
        "strong", //	Represents strong emphasis greater important text.
        "style", //	Used to add CSS style to an HTML document.
        "sub", //	Represents inline subscript text.
        "sup", //	Represents inline superscript text.
        "table", //	Used to defines a table in an HTML document.
        "tbody", //	Used for grouping table rows.
        "td", //	Used for creates standard data cell in HTML table.
        "textarea", //	Create multi-line text input.
        "tfoot", //	Used to adding a footer to a table that containing summary of the table data.
        "th", //	Used for creates header of a group of cell in HTML table.
        "thead", //	Used to adding a header to a table that containing header information of the table.
        "time", //	Represents the date and/or time in an HTML document.	NEW
        "title", //	Represents title to an HTML document.
        "tr", //	Defines a row of cells in a table.
        "track", //	Represents text tracks for both the "audio", // and "video", // tags.	NEW
        "tt", //	Represents teletype text.	REMOVE
        "u", //	Represents underlined text.
        "ul", //	Defines an unordered list of items.
        "var", //	Represents a variable in a computer program or mathematical equation.
        "video", //	Used to embed video content.	NEW
        "wbr" //	Defines a word break opportunity in a long string of text.	NEW

        // TODO XHTML tags too?

    );

    /**
     * Checks if given text input complies to POMS standard.
     * @see #ILLEGAL_PATTERN for a rough check
     */
    public static boolean isValid(@NonNull String input, boolean aggressive) {
        Matcher matcher = ILLEGAL_PATTERN.matcher(input);
        if(!matcher.find()) {
            return true;
        }
        // sanitizing should do nothing (modulo white space)
        if (aggressive) {
            return Objects.equals(
                normalizeWhiteSpace(input),
                normalizeWhiteSpace(sanitize(input))
            );
        } else {
            if (!Parser.unescapeEntities(input, false).equals(input)) {
                return false;
            }

            // we just reject if known tags are encountered after parsing.
            List<Node> parsed = Parser.parseFragment(input, Jsoup.parse("").body(), "http://localhost/");
            for (Node e : parsed) {
                if (! isValid(e)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean isValid(Node n) {
        if (n instanceof Element e) {
            if (ALL.contains(e.tagName().toLowerCase())) {
                return false;
            }
            for (Node child : e.childNodes()) {
                if (child instanceof Element ce) {
                    if (!isValid(ce)) {
                        return false;
                    }
                }
            }
        } else if (n instanceof TextNode t) {
            // also, there should be no html parseable entities.//
            // but at this point the text node is already parsed.
            return true;
        } else if (n instanceof Comment c) {
            return false;
        }
        return true;
    }


    /**
     * Checks if given text input complies to POMS standard.
     * @see #ILLEGAL_PATTERN for a rough check
     */
    public static boolean isValid(@NonNull String input) {
        return isValid(input, true);
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
     * Replaces all non-breaking space characters (\u00A0) with a normal white space character.
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
     * Replaces all non-breaking space entities(&nbsp;) with a normal white space character.
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
            .replaceAll("\n{3,}", "\n\n")
            .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // control characters, (but not newlines and tabs)
            ;
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
        if (input == null) {
            return null;
        }
        String unescaped = replaceHtmlEscapedNonBreakingSpace(unescapeHtml(input));
        return unescapeHtml(
            stripHtml(
                replaceOdd(unescaped)
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
     * Selects first non-null of the parameters.
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

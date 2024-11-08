/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import org.checkerframework.checker.nullness.qual.PolyNull;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;


import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.vpro.util.TextUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Roelof Jan Koekoek
 * @since 1.5
 */
public class TextUtilTest {

    private static final String MSE_2589 = "Uit een grondige analyse in het British Medical Journal van duizenden Amerikaanse auto-ongelukken waarbij een of beide bestuurders omkwamen, blijkt dat te zware bestuurders meer kans hebben om bij zo'n crash het leven te laten dan mensen met een normaal gewicht. Bij mannen loopt het risico evenredig op met de body mass index (BMI). Een ernstig obese man (BMI > 40) heeft bijna twee keer zo veel risico op een fatale afloop als iemand met een normale BMI (tussen 18,5 en 25).  Anderzijds, heel magere mannen (BMI < 18,5) blijken net zo kwetsbaar als de ernstig obesen. Bij vrouwelijke bestuurders is het beeld iets anders. Magere vrouwen zijn net zo crashbestendig als vrouwen met een normaal gewicht, maar daarna loopt ook bij hen het overlijdensrisico op met de BMI. Er is echter weinig verschil in overlijdensrisico tussen ernstig obese (BMI > 40) en 'gewoon' obese (35 <  BMI  < 40) vrouwen: beide categorieën overlijden twee keer zo vaak als slanke vrouwen. Als je simpelweg zou turven hoeveel dikke/normale/magere mensen omkomen bij auto-ongelukken, is er een scala aan confounders (letterlijk: 'verwarrers') die tot valse resultaten kan leiden. Misschien rijden dikke mensen minder vaak zelf, en is de passagiersplaats gevaarlijker dan de bestuurdersplaats. Misschien rijden dikke mensen in kleinere, kwetsbaarder auto's. Misschien doen dikke mensen vaker hun gordel niet om, of zitten ze vaker te bellen of te eten achter het stuur. Misschien rijden ze slordiger of roekelozer. Epidemiologen Thomas Rice en Motao Zhu bekeken daarom alleen ongelukken tussen twee auto's van hetzelfde type en ongeveer dezelfde grootte, en matchten deze paren bestuurders ook nog qua gebruik van de autogordel. Verder zijn twee auto's die met elkaar botsen, automatisch gematched op eigenschappen als de tijd van de dag, de drukte op de weg, de weersomstandigheden en de hevigheid van de botsing. Als je ook één-auto-crashes meeneemt, zijn dat allemaal potentiele confounders. Bijvoorbeeld: als dikke mensen gemiddeld harder zouden rijden dan slanke, dan rijden ze zichzelf vaker dood. Uiteindelijk hielden Rice en Zhu een kleine 3500 autobotsingen over met bijna 7000 paarsgewijs gematchte bestuurders, waaruit het verband tussen BMI en overlijdensrisico volgde. De grote vraag is natuurlijk: hoe komt dat? De onderzoekers speculeren, dat dikke mensen door hun vetlaag rond de heup niet goed vast te snoeren zijn in de heupgordel. Daardoor vliegen ze bij een botsing eerst een stuk naar voren, voordat de heupbotten gestuit worden door de gordel, en dat zou de geïncasseerde klap vergroten. Andere onderzoekers hebben dit effect zelfs getest door auto's te laten crashen in het laboratorium met dikke of slanke lijken op de bestuurdersplaats. Maar waarom dit tot een hogere sterftekans zou leiden blijft de vraag.  Een andere verklaring, net zo speculatief, is dat dikke mensen gemiddeld een zwakkere gezondheid hebben, en dus eerder het loodje leggen als de klap van een auto-botsing daar bovenop komt. Een simpele fysische verklaring die de onderzoekers buiten beschouwing laten, zou je het olifant-effect kunnen noemen. Een auto-botsing is vergelijkbaar met van een zekere hoogte verticaal op de grond vallen. De hoge piekvertraging (in een fractie van een seconde van 30 of 50 km/u naar 0) is in wezen wat de schade aanricht.  Een kat of hond kan probleemloos van anderhalve meter hoog op een stenen vloer springen, maar een olifant die hetzelfde probeert, breekt minstens een paar botten en overleeft het misschien niet eens.  Je kunt je ook zelf het verschil voorstellen tussen enerzijds, van anderhalve meter hoogte van een muurtje springen, en hetzelfde doen met 25 kilo aan halterschijven om je middel.  Hoe meer kilo's je meetorst, hoe harder de klap van een hoge piekvertraging aankomt. Dat geldt zeker als een bestuurder de gordel niet om heeft en door het auto-interieur tot stilstand wordt gebracht bij een botsing. Driver obesity and the risk of fatal injury during traffic collisions, T.Rice, M. Zhu, British Medical journal, 21 januari 2013";



    @Test
    public void testPatternOnTag() {
        assertThat(isValid("Tekst met <bold>html</bold>")).isFalse();
    }

    @Test
    public void testPatternOnLowerThen() {
        assertThat(isValid("a < b")).isTrue();
    }

    @Test
    public void testMSE_2589() {
        assertThat(isValid(MSE_2589)).isTrue();
    }


    @Test
    public void testPatternOnValidAmpersand() {
        assertThat(isValid("Pauw & Witteman;")).isTrue();
    }

    @Test
    public void testPatternOnNumericEscape() {
        assertThat(isValid("&#233;")).isFalse();
    }

    @Test
    public void testPatternOnTextEscape() {
        assertThat(isValid("&eacute;")).isFalse();
    }

    @Test
    public void testPatternOnTextEscapeWithUC() {
        assertThat(isValid("&Ograve;")).isFalse();
    }

    @Test
    public void testPatternOnTextEscapeWithDigits() {
        assertThat(isValid("&frac14;")).isFalse();
    }

    @Test
    public void testPatternOnLineBreak() {
        assertThat(isValid("Text with line\u2028break.")).isFalse();
    }

    @Test
    public void testPatternOnNewLine() {
        assertThat(isValid("Text with newline\nbreak.")).isTrue();
    }

    @Test
    public void testSanitizePreserveInputWithSmallerThen() {
        assertThat(sanitize("a < b")).isEqualTo("a < b");
    }

    @Test
    public void testSanitizePreserveInputWithAmpersand() {
        assertThat(sanitize("a & b")).isEqualTo("a & b");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // There are just by chance containing html like stuff.
        "a < b",
        "a & b",
        "Contact: <sandwich@avrotros.nl>",
        "&bla;"
    })
    public void permissiveValidation(String input) {
        assertThat(isValid(input, false)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // some string that are obviously meant as html, and should be rejected
        "<p>foo</p>",
        "&nbsp;",
        "&lt;",
        "<script>alert('hoi')</script>",
        "<hacky><script>alert('hoi')</script></foobar"
    })
    public void permissiveInvalidation(String input) {
        assertThat(isValid(input, false)).isFalse();
    }

    @Test
    public void testSanitizeOnHtmlInput() {
        assertThat(sanitize("<p>Hello world</p><br>")).isEqualTo("Hello world");
    }

    @Test
    public void testSanitizeOnHtmlFantasy() {
        assertThat(sanitize("<fantasy>Hello world")).isEqualTo("Hello world");
    }

    @Test
    public void testSanitizeOnAmpersands() {
        assertThat(sanitize("Hello&nbsp;world")).isEqualTo("Hello world");
    }

    @Test
    public void testSanitizeOnIllegalLineBreak() {
        assertThat(sanitize("Hello\u2028world")).isEqualTo("Hello world");
    }

    @Test
    public void testSanitizeOnHtmlEscapedNbsp() {
        assertThat(sanitize("Hello&nbsp;world")).isEqualTo("Hello world");
    }

    @Test
    public void testSanitizeOnDoubleEscapedInput() {
        assertThat(sanitize("A &amp;amp; B")).isEqualTo("A & B");
    }

    @Test
    public void testSanitizeOnDoubleEscapedNbsp() {
        assertThat(sanitize("Hello&amp;nbsp;world")).isEqualTo("Hello world");
    }

    @Test
    public void testSanitizeUnicodeChar() {
        String result = sanitize("KRO De Re&#252;nie");
        assertThat(result.getBytes(UTF_8)).isEqualTo("KRO De Reünie".getBytes(UTF_8));
    }

    @Test
    public void testSanitizeMSE_2589() {
        String result = sanitize(MSE_2589);
        assertThat(result.replaceAll("\\s", "")).isEqualTo(MSE_2589.replaceAll("\\s", ""));
    }

    @Test
    public void testSanitizeMSE_5216() {
        String result = sanitize("<p>foo</p><p>bar</p>");
        assertThat(result).isEqualTo("foo bar"); // Hmm. Doesn't fail. The description of MSE-5216 is not quite accurate.
        String example = "<p>Hokus pokus</p><p>bla  <em>asdfasdf afsfd</em></p><p>Simsalabim</p><ol><li>één</li><li><strong>twee</strong></li><li>drie</li><li>vier</li></ol><ul><li>bol</li><li>kubus</li><li>kegel</li></ul><p>nog <u>een vijf</u></p><p>verdrietjes 123456789</p>";
        String result2 = sanitize(example);
        assertThat(result2).isEqualTo("Hokus pokus bla asdfasdf afsfd Simsalabim één twee drie vier bol kubus kegel nog een vijf verdrietjes 123456789");
    }

    @Test
    public void testLexico() {
        assertThat(TextUtil.getLexico("Het grote huis", new Locale("nl", "NL"))).isEqualTo("Grote huis, het");
    }

    @Test
    public void testLexicoLowercase() {
        assertThat(TextUtil.getLexico("het grote huis", new Locale("nl", "NL"))).isEqualTo("grote huis, het");
    }

    @Test
    public void testLexicoUppercase() {
        assertThat(TextUtil.getLexico("HET GROTE HUIS", new Locale("nl", "NL"))).isEqualTo("GROTE HUIS, HET");
    }

    @Test
    public void testLexicoOtherwise() {
        assertThat(TextUtil.getLexico("Daar gaat ie weer", new Locale("nl", "NL"))).isEqualTo("Daar gaat ie weer");
    }

    @Test
    public void testLexicoNoWordboundary() {
        assertThat(TextUtil.getLexico("Hete broodjes", new Locale("nl", "NL"))).isEqualTo("Hete broodjes");
    }

    @Test
    public void testLexicoParticleOnly() {
        assertThat(TextUtil.getLexico("Het", new Locale("nl", "NL"))).isEqualTo("Het");
    }

    @Test
    public void testTruncateLong() {
        assertThat(TextUtil.truncate("Bla bla. Bloe bloe", 10)).isEqualTo("Bla bla.");
    }

    @Test
    public void testTruncateShorter() {
        assertThat(TextUtil.truncate("Bla bla", 3)).isEqualTo("Bla");
    }

    @Test
    public void testTruncateShort() {
        assertThat(TextUtil.truncate("Bla bla. Bloe bloe", 3)).isEqualTo("Bla");
    }

    @Test
    public void testTruncateShort2() {
        assertThat(TextUtil.truncate("Bla bla. Bloe bloe", 5)).isEqualTo("Bla");
    }


    @Test
    public void testTruncateShortWithEllipses() {
        assertThat(TextUtil.truncate("Bla bla. Bloe bloe", 5, true)).isEqualTo("Bla...");
    }


    @Test
    public void testSanitizeIframe() {
        assertThat(Jsoup.clean("<iframe><a href=\"http://fistsoftime.bandcamp.com/album/i-will-survive\">I Will Survive by Fists Of Time</a></iframe>", Safelist.none()))
            .isEqualTo("&lt;a href=\"http://fistsoftime.bandcamp.com/album/i-will-survive\"&gt;I Will Survive by Fists Of Time&lt;/a&gt;");

        assertThat(sanitize("<iframe><a href=\"http://fistsoftime.bandcamp.com/album/i-will-survive\">I Will Survive by Fists Of Time</a></iframe>")).isEqualTo("I Will Survive by Fists Of Time");
        assertThat(sanitize("<iframe style=\"border: 0; width: 100%; height: 42px;\" src=\"https://bandcamp.com/EmbeddedPlayer/album=2972369232/size=small/bgcol=ffffff/linkcol=0687f5/transparent=true/\" seamless><a href=\"http://fistsoftime.bandcamp.com/album/i-will-survive\">I Will Survive by Fists Of Time</a></iframe>")).isEqualTo("I Will Survive by Fists Of Time");
    }

    @Test
    public void testSaniziteOddchars() {
        assertThat(sanitize("a\bc")).isEqualTo("a c");
        assertThat(sanitize("a\b\u0007c")).isEqualTo("a c");


        // this was an actual occurence:
        assertThat(sanitize("Zanger Van Boven is er duidelijk over: “Je kan deze cd zien als een retrospectief van toen tot nu.” \bZo staan er nieuwe nummers op het album die enkele weken voor de opnamesessies zijn geschreven, maar ook oude nummers die al meegaan sinds dat de band enkele jaren geleden haar eerste album uitbracht. In sommige nummers zijn de inspiratiebronnen van de band goed te herkennen. Zo doet de gitaar in ‘Voor Het Fatsoen’ denken aan \u0007‘Norwegian Wood’ (The Beatles) \ben verraadt het connecties met de Engelse beatmuziek uit de jaren 60. ‘Zo is het Maar Net’ heeft iets van ‘Another Brick In The Wall’ (Pink Floyd) en laat zien dat de band zich ook heeft laten vormen door de psychedelische rock uit de jaren 70. \u0007Zo is de cd niet alleen een kijkje in de keuken van de band zelf, maar is het ook een greep uit 60 jaar popmuziek."))
            .isEqualTo("Zanger Van Boven is er duidelijk over: “Je kan deze cd zien als een retrospectief van toen tot nu.” Zo staan er nieuwe nummers op het album die enkele weken voor de opnamesessies zijn geschreven, maar ook oude nummers die al meegaan sinds dat de band enkele jaren geleden haar eerste album uitbracht. In sommige nummers zijn de inspiratiebronnen van de band goed te herkennen. Zo doet de gitaar in ‘Voor Het Fatsoen’ denken aan ‘Norwegian Wood’ (The Beatles) en verraadt het connecties met de Engelse beatmuziek uit de jaren 60. ‘Zo is het Maar Net’ heeft iets van ‘Another Brick In The Wall’ (Pink Floyd) en laat zien dat de band zich ook heeft laten vormen door de psychedelische rock uit de jaren 70. Zo is de cd niet alleen een kijkje in de keuken van de band zelf, maar is het ook een greep uit 60 jaar popmuziek.");

        assertThat(sanitize("‘Ik hoop dat mensen na mijn film de liefde bedrijven’\u0000.")).isEqualTo("‘Ik hoop dat mensen na mijn film de liefde bedrijven’ .");
    }

    @Test
    public void testSanitizeTouchingPs() {
        assertThat(sanitize("<p>foo</p><p>bar</b>")).isEqualTo("foo bar");
    }

    @Test
    public void strikeThrough() {
        assertThat(TextUtil.strikeThrough("foo bar 123")).isEqualTo("f̶o̶o̶ ̶b̶a̶r̶ ̶1̶2̶3̶");
    }

    @Test
    public void underLine() {
        assertThat(TextUtil.underLine("foo bar 123")).isEqualTo("f̲o̲o̲ ̲b̲a̲r̲ ̲1̲2̲3̲");
    }

    @Test
    public void underLineDouble() {
        assertThat(TextUtil.underLineDouble("foo bar 123")).isEqualTo("f̳o̳o̳ ̳b̳a̳r̳ ̳1̳2̳3̳");
    }

    @Test
    public void overLine() {
        assertThat(TextUtil.overLine("foo bar 123")).isEqualTo("f̅o̅o̅ ̅b̅a̅r̅ ̅1̅2̅3̅");
    }

    @Test
    public void overLineDouble() {
        assertThat(TextUtil.overLineDouble("foo bar 123")).isEqualTo("f̿o̿o̿ ̿b̿a̿r̿ ̿1̿2̿3̿");
    }

    @Test
    public void underDiaeresis() {
        assertThat(TextUtil.underDiaeresis("foo bar 123")).isEqualTo("f̤o̤o̤ ̤b̤a̤r̤ ̤1̤2̤3̤");
    }

    @Test
    public void unhtml() {
        String example = "<p>Hokus p&ograve;kus</p><p>bla&nbsp; asdf\u0002asdf</p><p>Simsalabim</p><ol><li>1</li><li>2</li></ol><p>adsfasdf</p><p /><p>asdfsdf!asdf</p>";
        assertThat(TextUtil.unhtml(example)).isEqualTo("""
            Hokus pòkus

            bla  asdfasdf

            Simsalabim
            -1
            -2

            adsfasdf

            asdfsdf!asdf""");
    }

    @Test
    public void withLSEPAndNBSP() {
        String example = "nbsp:\u00a0line separator:\u2028foobar";
        String unhtmled = TextUtil.unhtml(example);
        assertThat(isValid(unhtmled)).isTrue();
        assertThat(normalizeWhiteSpace(unhtmled)).isEqualTo(normalizeWhiteSpace(sanitize(unhtmled)));
    }


    @Test
    public void withLSEP() {
        String example = "<p>Dit jaar is alweer de achtste editie van <em>An Evening of Today,</em> een showcase voor jonge nieuwe-muziekensembles en componisten. De avond biedt een zeer divers beeld van de muziek van nu. De verbeelding staat centraal: in eerdere edities verrasten de componisten met ongebruikelijke opstellingen, ballonnen, tafeltennis, video, nieuwe speeltechnieken, gerafelde nachtclubsferen, elektronica, nepnieuws, een rockband en 'visuele muziek' zonder geluid.</p><p><br></p><p><strong>An Evening of Today</strong></p><p>Chongliang Yu - Digital Doppelgänger\n" +
            "</p><p><em>Not the center: Cèlia Tort Pujol, Leonie Strecker, Myrthe Bokelmann, Roma Gavryliuk</em></p><p>Creating ‘on the spot’\n" +
            "</p><p><em>Ensemble Resilience: Paolo Gorini (piano & Seaboard (MIDI-controller)), Marco Danesi (klarinet), Natalie Kulina (viool), Tomek Szczepaniak (Mater (hybride percussie-instrument) en Chimes tree (percussie-instrument)), Gerardo Gozzi (live compositie), Rubens Askenar (live compositie)</em></p><p>David Ko 너랑나 — You and Me Song</p><p><em>Frieda Gustavs (zang), Lautaro Hochman, Julek Warszawski, Siebren Smink (gitaar samples)</em></p><p>David Ko 너랑나 —  Psalm Song</p><p><em>Pietro Elia Barcellona (contrabas), Michele Mazzini (basklarinet), Kavid Do (keyboard)</em></p><p>Apollonio Maiello - Typing…\n" +
            "</p><p><em>Katherine Weber (sopraan), Juho Myllylä (blokfluit & elektrisch gitaar), Hessel Moeselaar (altviool & MIDI-pedals), \u2028Francisco Martí Hernández (piano)</em></p><p>Alberto Granados - Earwash\n" +
            "</p><p><em>Jesse Debille, Noemi Calzavara, Alberto Granados Reguilón, Irene Comesaña Aguilar (productie & artistiek regisseur)</em></p><p>Rechtstreeks vanuit Muziekgebouw aan 't IJ, Amsterdam</p>";

        String unhtmled = TextUtil.unhtml(example);
        assertThat(isValid(unhtmled)).isTrue();
        assertThat(normalizeWhiteSpace(unhtmled)).isEqualTo(normalizeWhiteSpace(sanitize(unhtmled)));

        assertThat(unhtmled).isEqualTo("""
            Dit jaar is alweer de achtste editie van An Evening of Today, een showcase voor jonge nieuwe-muziekensembles en componisten. De avond biedt een zeer divers beeld van de muziek van nu. De verbeelding staat centraal: in eerdere edities verrasten de componisten met ongebruikelijke opstellingen, ballonnen, tafeltennis, video, nieuwe speeltechnieken, gerafelde nachtclubsferen, elektronica, nepnieuws, een rockband en 'visuele muziek' zonder geluid.

            An Evening of Today

            Chongliang Yu - Digital Doppelgänger

            Not the center: Cèlia Tort Pujol, Leonie Strecker, Myrthe Bokelmann, Roma Gavryliuk

            Creating ‘on the spot’

            Ensemble Resilience: Paolo Gorini (piano & Seaboard (MIDI-controller)), Marco Danesi (klarinet), Natalie Kulina (viool), Tomek Szczepaniak (Mater (hybride percussie-instrument) en Chimes tree (percussie-instrument)), Gerardo Gozzi (live compositie), Rubens Askenar (live compositie)

            David Ko 너랑나 — You and Me Song

            Frieda Gustavs (zang), Lautaro Hochman, Julek Warszawski, Siebren Smink (gitaar samples)

            David Ko 너랑나 —  Psalm Song

            Pietro Elia Barcellona (contrabas), Michele Mazzini (basklarinet), Kavid Do (keyboard)

            Apollonio Maiello - Typing…

            Katherine Weber (sopraan), Juho Myllylä (blokfluit & elektrisch gitaar), Hessel Moeselaar (altviool & MIDI-pedals),\s
            Francisco Martí Hernández (piano)

            Alberto Granados - Earwash

            Jesse Debille, Noemi Calzavara, Alberto Granados Reguilón, Irene Comesaña Aguilar (productie & artistiek regisseur)

            Rechtstreeks vanuit Muziekgebouw aan 't IJ, Amsterdam""");
    }

    @Test
    public void stripHtml() {
        String example = "<p>Hokus p&ograve;kus</p><p>bla&nbsp; asdfasdf</p><p>Simsalabim</p><p>asdf</p><p>adsfasdf</p><p>asdfsdf!asdf</p>";
        assertThat(TextUtil.stripHtml(example)).isEqualTo("Hokus pòkus bla&nbsp; asdfasdf Simsalabim asdf adsfasdf asdfsdf!asdf");
    }




    @ParameterizedTest
    @MethodSource("polyNullMethods")
    public void polynull(Method m) throws InvocationTargetException, IllegalAccessException {
        Object[] parameters = new Object[m.getParameterCount()];
        for (int i = 0 ; i < m.getParameterCount(); i++) {
            if (boolean.class.equals(m.getParameterTypes()[i])) {
                parameters[i] = Boolean.FALSE;
            }
            if (int.class.equals(m.getParameterTypes()[i])) {
                parameters[i] = 10;
            }
        }
        Object invoke = m.invoke(null, parameters);
        assertThat(invoke).isNull();
    }

    public static Stream<Arguments> polyNullMethods() {
         return Arrays.stream(TextUtil.class.getDeclaredMethods())
             .filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()))
             .filter(m -> m.getAnnotatedReturnType().getAnnotation(PolyNull.class) != null)
             .map(Arguments::of);
    }

}


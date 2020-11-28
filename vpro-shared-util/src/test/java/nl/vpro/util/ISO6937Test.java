package nl.vpro.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static nl.vpro.util.ISO6937CharsetProvider.ISO6937;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Michiel Meeuwissen
 */
@SuppressWarnings("InjectedReferences")
public class ISO6937Test {

    @Test
    public void registration() {
        Charset charset = Charset.availableCharsets().get("ISO-6937");
        assertNotNull(charset);
        assertEquals(ISO6937, charset);
    }

    @Test

    public void forName() {
        Charset iso6037 = Charset.forName("ISO-6937");
        assertNotNull(iso6037);
    }

    @Test
    public void conversion()  {
        assertEquals(
            "\u00fc",
            new String(
                new byte[] { (byte) 0xc8, (byte) 'u'},
                ISO6937));
        assertEquals(
            "Atat\u00fcrk",
            new String(
                new byte[] { 'A', 't', 'a', 't', (byte) 0xc8, 'u', 'r', 'k'},
                ISO6937));


    }
    @Test
    public void deconversion() {
        assertArrayEquals("Atat\u00fcrk".getBytes(ISO6937), new byte[]{'A', 't', 'a', 't', (byte) 0xc8, 'u', 'r', 'k'});
    }
    @Test
    public void longStrings() throws IOException {
        // Testing whether everything works ok on the buffer boundaries.

        // build a giant string with u-umlauts
        // and also a byte array in iso-6937 representing the exact same string
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder      string = new StringBuilder();
        string.append("b");
        out.write('b');
        Random random = new Random(0);
        for (int i = 1 ; i < 10000; i++) {
            string.append("\u00fc");
            out.write(0xc8);
            out.write('u');
            if (i % (random.nextInt(25) + 1) == 0) {
                // some newlines here and there
                string.append('\n');
                out.write('\n');
            }
        }

        // now lets test whether the byte array actually is the expected string
        assertEquals(
            string.toString(),
            new String(out.toByteArray(), "ISO-6937"));

        assertArrayEquals(string.toString().getBytes("ISO-6937"), out.toByteArray());
    }

    @Test
    public void longDeconversion() {
        String test = "WEBVTT 1 2:00.000 --> 2:01.120 888 2 2:01.170 --> 2:04.160 Zondagmiddag tijd voor het Zapp Weekjournaal. 3 2:04.210 --> 2:06.220 Vandaag met deze onderwerpen. 4 2:07.150 --> 2:11.010 Dit wintersportseizoen zijn lawines extra gevaarlijk. 5 2:11.060 --> 2:14.240 Tobias uit Oostenrijk werd bedolven onder de sneeuw. 6 2:23.110 --> 2:26.000 Vannacht worden de Oscars uitgereikt. 7 2:26.050 --> 2:29.020 De Nederlandse filmmakers Job, Joris en Marieke... 8 2:29.070 --> 2:32.100 maken met 'A Single Life' kans op deze belangrijke filmprijs. 9 2:32.150 --> 2:35.180 Ik denk dat we keihard gaan schreeuwen. 10 2:35.230 --> 2:40.150 En een dier van de week met een enorme bult op zijn rug 11 2:43.240 --> 2:47.100 Bij ons in de studio de populaire YouTuber Kwebbelkop. 12 2:47.150 --> 2:50.150 Leuk dat je er bent. Leuk dat ik er mag zijn. 13 2:50.200 --> 2:53.060 Je echte naam is Jordi van den Bussche. Ja. 14 2:53.110 --> 2:57.100 En zo kwamen we nog meer te weten toen we jou googelden. 15 2:57.150 --> 3:02.000 Jordi is 19 jaar en woont in Amsterdam met huisgenoot en medevlogger Jelly. 16 3:02.050 --> 3:06.090 Verder heeft hij een zus Lauren en 2 katten. 17 3:06.140 --> 3:09.170 This is cat number 1 and that is cat number 2. 18 3:10.070 --> 3:12.100 Jordi's YouTube naam is Kwebbelkop. 19 3:12.150 --> 3:15.060 De naam van een pop die hij als kind had. 20 3:15.110 --> 3:19.050 I got the name Kwebbelkop from one of these little dolls. 21 3:19.100 --> 3:22.070 Kwebbelkop begon pas 2 jaar geleden, maar hij heeft nu al... 22 3:22.120 --> 3:27.130 een miljoen volgers op zijn YouTube-kanalen Kwebbelkop en Kwebbelcop. Met een C dus. 23 3:27.180 --> 3:31.230 Kwebbelkop is vooral bekend van de stunts die hij doet bij Grand Theft Auto. 24 3:32.030 --> 3:33.230 O my God! 25 3:34.030 --> 3:36.180 Oooo my God! 26 3:36.230 --> 3:41.030 Al zijn filmpjes zijn in het Engels, en dat heeft hij zichzelf aangeleerd. 27 3:41.080 --> 3:44.080 Het zorgt ervoor dat hij in het buitenland ook erg bekend is. 28 3:44.130 --> 3:50.000 En aan iedereen die het wil laat hij zien wat hij naast commentaar geven bij games, allemaal nog meer kan. 29 3:54.170 --> 3:59.150 Ik kan best goed zingen. Was dit een populair filmpje? Ja. 30 3:59.200 --> 4:03.030 Die is 100.000 keer bekeken. Oke. 31 4:03.080 --> 4:08.160 Wat je moet doen en kunnen om een bekende YouTubester te worden, daar gaan we het zo over hebben. 32 4:18.000 --> 4:20.140 Wat was er allemaal in het nieuws deze week? 33 4:20.190 --> 4:22.210 Je ziet het in ons nieuwsoverzicht. 34 6:18.030 --> 6:19.210 Het is krokusvakantie. 35 6:20.010 --> 6:23.040 Voor veel Nederlanders het moment om op wintersport te gaan. 36 6:23.090 --> 6:27.120 Maar wintersport kan ook gevaarlijk zijn. Door lawines bijvoorbeeld. 37 6:27.170 --> 6:29.230 Dit jaar is het gevaar extra groot. 38 6:30.030 --> 6:33.190 Daar weet de 16 jarige Tobias uit Oostenrijk alles van. 39 6:33.240 --> 6:36.200 Een paar weken geleden kwam hij in een lawine terecht. 40 6:37.000 --> 6:38.120 Milou zocht hem op. 41 6:43.230 --> 6:48.090 Dit is wat veel mensen geweldig vinden; skien in de Alpen! 42 6:49.100 --> 6:52.170 Ieder jaar gaan bijna een miljoen Nederlanders op wintersportvakantie. 43 6:52.220 --> 6:55.110 Maar hoe leuk het ook is, het gaat weleens mis. 44 6:55.160 --> 6:58.120 Iemand breekt een arm of been, maar soms... 45 6:58.170 --> 7:01.020 vallen er ook slachtoffers door een lawine. 46 7:04.090 --> 7:10.220 Dit wintersportseizoen zijn er meer lawines dan andere jaren en vallen er veel slachtoffers. 47 7:12.140 --> 7:15.220 Het komt vooral door de slechte opbouw van de sneeuwlagen. 48 7:16.020 --> 7:21.210 Aan het begin van het seizoen was de temperatuur vrij hoog, en warmde de zon de eerste sneeuwlagen op. 49 7:22.010 --> 7:24.240 Daardoor hechtten de volgende lagen minder goed. 50 7:25.040 --> 7:28.010 En schuiven de massa's sneeuw makkelijker naar beneden. 51 7:30.060 --> 7:32.060 Soms ontstaan lawines vanzelf. 52 7:32.110 --> 7:35.020 Maar regelmatig komt het door de wintersporters. 53 7:35.070 --> 7:40.220 Ze verlaten de veilige, geprepareerde piste, en gaan de diepe sneeuw in. 54 7:41.020 --> 7:46.040 Ze wrikken de lagen sneeuw los, en dan, wordt het levensgevaarlijk. 55 7:50.240 --> 7:56.210 Dat het dit jaar extra gevaarlijk is, weten Tobias en Luca uit Oostenrijk maar al te goed. 56 7:57.010 --> 8:00.080 Eind december ging het mis, toen ze samen gingen skien. 57 8:39.030 --> 8:43.120 Hoe het was om zo lang onder de sneeuw te liggen, weet Tobias niet meer. 58 8:43.170 --> 8:46.040 Hij was vrijwel meteen bewusteloos. 59 8:46.090 --> 8:49.220 Maar gelukkig was Luka dat niet en kon hij zijn moeder bellen. 60 9:32.220 --> 9:35.130 Maar het lukte, Tobias werd gevonden. 61 9:56.050 --> 10:00.000 De jongens gaan liever niet terug naar de plek waar het is gebeurd. 62 10:00.050 --> 10:02.230 Iemand die ons dat wel kan laten zien is Sepp. 63 10:05.160 --> 10:09.060 Hij is een van de reddingswerkers die de jongens heeft gered. 64 10:46.100 --> 10:50.140 En Sepp laat zien hoe hij Tobias onder de sneeuw heeft kunnen vinden. 65 11:15.210 --> 11:17.110 Ja, een schep meenemen dus. 66 11:17.160 --> 11:20.200 Maar Tobias en Luka hadden geen extra spullen bij zich. 67 11:21.000 --> 11:26.040 Ze zijn erg geschrokken door het ongeluk, toch zijn ze niet van plan om het skien op te geven. 68 11:26.090 --> 11:28.060 Maar ze hebben er wel wat van geleerd. 69 11:48.240 --> 11:54.230 De Nederlandse Skivereniging adviseert wintersporters om dit jaar vooral op de pistes te blijven. 70 11:55.030 --> 11:57.060 Dat is het allerveiligst. 71 11:59.100 --> 12:02.210 Straks, de bultrug die een toeristisch bezoekje aan Zeeland bracht. 72 12:03.010 --> 12:05.190 Het is het nieuwsdier van de week. 73 12:07.080 --> 12:09.240 Kwebbelkop, of zal ik je Jordi noemen? 74 12:10.040 --> 12:13.070 Maakt me niet zoveel uit. Doe maar Kwebbelkop. 75 12:13.120 --> 12:16.240 Als Kwebbelkop zet je twee filmpjes per dag op YouTube. 76 12:17.040 --> 12:19.010 Dat ziet er ongeveer zo uit. 77 12:19.060 --> 12:20.180 Welcome back, cops! 78 12:20.230 --> 12:23.150 RAAR STEMMETJE: I'm running really fast. 79 12:23.200 --> 12:26.020 But as you can see, this is my room. 80 12:26.070 --> 12:28.080 Look at my room, it's so crazy. 81 12:29.190 --> 12:33.010 *This is, the part when I dance 82 12:33.060 --> 12:35.050 Holy shiiiiiiiiiiit! 83 12:36.160 --> 12:38.180 Kwebbelkop in actie. 84 12:38.230 --> 12:43.130 Deze filmpjes lokken reactie uit en veel kinderen hebben er vragen over. 85 12:43.180 --> 12:46.030 We beginnen met eentje van Rhodee. 86 12:46.080 --> 12:49.240 Hoi, waarom doe je soms zo raar met je stem? 87 12:50.040 --> 12:52.140 Waarom ik raar doe met mijn stem... 88 12:52.190 --> 13:00.020 Ik vind het leuk om actief te zijn en als je de hele tijd met een toon praat, wordt het saai. 89 13:00.070 --> 13:04.180 Dus dan doe ik het graag een beetje hoog en dan laag. 90 13:04.230 --> 13:07.210 Soms heel snel hoog en heel snel laag. 91 13:08.010 --> 13:13.140 Ik vind het zelf leuk om te doen. Je hebt er echt over nagedacht. Ja. 92 13:13.190 --> 13:17.050 Ik begon er mee en heel veel mensen vonden het grappig. 93 13:17.100 --> 13:20.010 En ik vind het zelf ook leuk. 94 13:20.060 --> 13:24.180 Een ander ding dat opvalt is dat je al je filmpjes in het Engels doet. 95 13:24.230 --> 13:28.180 Daar hebben we veel reacties op gekregen en vragen van kinderen. 96 13:28.230 --> 13:31.230 Die willen weten: Waarom doe je het in het Engels? 97 13:32.030 --> 13:35.110 Ik doe het in het Engels omdat ik graag mensen wil vermaken. 98 13:35.160 --> 13:38.050 Ik kan meer mensen vermaken als ik het in het Engels doe. 99 13:38.100 --> 13:41.100 Omdat meer mensen Engels praten dan Nederlands. 100 13:41.150 --> 13:46.060 Ik kan ook nog steeds Nederlandse mensen vermaken, want veel Nederlanders spreken Engels. 101 13:46.110 --> 13:50.000 Dat is dus de reden dat ik het in het Engels doe. 102 13:50.050 --> 13:53.140 Je spreekt het goed, maar je hebt het jezelf aangeleerd. 103 13:53.190 --> 13:59.070 Ik heb veel videospelletjes gespeeld in het Engels en met Engelse vrienden gepraat. 104 13:59.120 --> 14:02.180 En Engelse films gekeken, Engelse series gekeken. 105 14:02.230 --> 14:07.030 En in de loop van de jaren leer je dat gewoon. 106 14:07.080 --> 14:09.130 En zo heb ik dus Engels geleerd. 107 14:09.180 --> 14:12.030 Je hebt veel fans over de hele wereld. 108 14:12.080 --> 14:14.010 We hebben het op een rijtje gezet. 109 14:14.060 --> 14:16.170 De meeste fans zitten in Amerika. 110 14:16.220 --> 14:19.050 Daar heb je 13 miljoen kliks per maand. 111 14:19.100 --> 14:21.090 Daarna in Engeland, vier miljoen. 112 14:21.140 --> 14:23.210 Dan pas Nederland, twee miljoen. 113 14:24.010 --> 14:26.040 Meer dan een miljoen in Canada. 114 14:26.090 --> 14:32.080 En 150.000 Japanners die je iedere maand aanklikken. 115 14:32.130 --> 14:35.090 Wat vind je er van? Ik vind het absurd. 116 14:35.140 --> 14:40.050 We gingen door de cijfertjes heen en ik vraag: Hoeveel zitten er in Japan? 117 14:40.100 --> 14:44.070 Ik keek en zag, 150.000. Dat is eigenlijk best heel veel. 118 14:44.120 --> 14:47.170 Dus ik vind het helemaal te gek. Prachtig. 119 14:47.220 --> 14:50.130 Simon wil ook nog iets hierover weten. 120 14:51.140 --> 14:54.190 Hoi Kwebbelkop, wilt u wereldberoemd worden? 121 14:55.210 --> 14:59.170 Wereldberoemd worden. Ik vind het een grote term. 122 14:59.220 --> 15:02.210 Maar het liefst vermaak ik zo veel mogelijk mensen. 123 15:03.010 --> 15:07.050 En als dat inhoudt dat ik wereldberoemd wordt, doe ik het graag. 124 15:07.100 --> 15:11.020 Ik streef er naar en ik vind het hartstikke leuk. 125 15:11.070 --> 15:12.190 Hoe meer, hoe beter. 126 15:12.240 --> 15:14.110 Ja, precies. 127 15:14.160 --> 15:18.040 Voor de duidelijkheid, Kwebbelkop is geen hobby, het is je baan. 128 15:18.090 --> 15:22.080 Je verdient per klik, maar je werkt er ook erg hard voor. 129 15:22.130 --> 15:25.220 En hoe hard, daar gaan we straks verder over praten. 130 15:31.030 --> 15:32.190 Het is op een plein of zo. 131 15:32.240 --> 15:35.020 Het is midden op straat. 132 15:35.070 --> 15:39.080 Een muziekinstrument. Hoe heet dat ding ook al weer. Een orgel? 133 15:39.130 --> 15:41.170 Nee, geen orgel. O nee. 134 15:41.220 --> 15:43.090 Een hele grote pan. 135 15:43.140 --> 15:45.240 Het lijkt net op spaghetti. 136 15:46.040 --> 15:48.020 Italie of zo 137 15:48.070 --> 15:51.010 Ja, het is in China of zo. Denk ik. 138 15:51.060 --> 15:55.210 Een accordeon en een bekertje waar waarschijnlijk geld in moet. 139 16:00.020 --> 16:03.100 Het is een foto van een jongen die in Griekenland op straat leeft. 140 16:03.150 --> 16:05.120 Hij wacht op een bakje eten. 141 16:05.170 --> 16:08.090 De gratis maaltijd wordt uitgedeeld aan arme mensen. 142 16:08.140 --> 16:12.020 De jongen heeft een accordeon, daar verdient hij op straat wat geld mee. 143 16:12.070 --> 16:17.130 Door grote geldproblemen van de regering is er de laatste jaren steeds meer armoede in Griekenland. 144 16:17.180 --> 16:20.020 Maar er is te weinig hulp. 145 16:20.070 --> 16:23.090 Veel kinderen in het land hebben niet genoeg te eten. 146 16:26.140 --> 16:31.030 Om in leven te blijven gaan veel Grieken naar plekken waar voedsel wordt uitgedeeld. 147 16:31.080 --> 16:35.060 Honderdduizenden ouders zijn door de geldcrisis hun baan kwijtgeraakt. 148 16:35.110 --> 16:39.140 Veel Grieken leven nu op straat omdat ze geen huis meer kunnen betalen. 149 16:41.080 --> 16:44.050 De geldzorgen in het land zijn reusachtig. 150 16:44.100 --> 16:47.200 Europa helpt Griekenland door miljarden euro's te lenen. 151 16:49.200 --> 16:52.140 Het land heeft kortgeleden nieuwe leiders gekozen. 152 16:52.190 --> 16:56.110 Die hebben beloofd dat ze het leven voor de inwoners beter zullen maken. 153 16:56.160 --> 16:59.100 Ook al krijgt Griekenland hulp uit het buitenland... 154 16:59.150 --> 17:03.080 voor de bevolking zal de ellende in hun land niet snel voorbij zijn. 155 17:05.170 --> 17:11.030 Dus dit soort voedseluitdeelplekken zullen voorlopig nog wel nodig blijven. 156 17:14.100 --> 17:17.230 Live from the Dolby Theatre at Hollywood and Highland. 157 17:18.030 --> 17:19.230 It's the Oscars. 158 17:20.030 --> 17:24.230 Een zaal vol beroemde filmsterren die hun mooiste jurk of pak hebben aangetrokken. 159 17:25.030 --> 17:29.130 En buiten duizenden fans die een glimp van hun idolen willen opvangen. 160 17:29.180 --> 17:33.190 Zo ziet het er ieder jaar uit bij de uitreiking van de Oscars in Hollywood. 161 17:33.240 --> 17:39.170 Een enorme happening waar iedereen die er een beetje toe doet in de filmwereld aanwezig is. 162 17:39.220 --> 17:43.190 Vannacht worden die belangrijke filmprijzen weer uitgereikt. 163 17:43.240 --> 17:48.230 Dat gebeurt in een grote show die over de hele wereld live op tv te volgen is. 164 17:49.030 --> 17:51.150 En hier is het allemaal om te doen. 165 17:51.200 --> 17:56.130 Een gouden beeld van 35 centimeter hoog en bijna 4 kilo zwaar. 166 17:56.180 --> 18:01.170 Officieel heet 't een Academy Award, maar de bijnaam van het beeld is Oscar. 167 18:01.220 --> 18:05.180 En als je genomineerd bent voor zo'n Oscar, dan wil je 'm winnen ook. 168 18:05.230 --> 18:07.240 Dat kan in verschillende categorieen. 169 18:08.040 --> 18:11.200 Er zijn prijzen voor de film met de mooiste muziek... 170 18:14.030 --> 18:17.160 de mooiste kostuums of de beste special effects. 171 18:17.210 --> 18:20.140 Ook zijn er Oscars voor lange animatiefilms... 172 18:20.190 --> 18:24.010 zoals Frozen, die won vorig jaar, en ook voor... 173 18:25.010 --> 18:26.210 korte animatiefilms. 174 18:27.010 --> 18:30.230 Maar de belangrijkste is toch wel die voor de beste film... 175 18:31.030 --> 18:34.130 beste acteur en beste actrice. 176 18:34.180 --> 18:39.040 En hel soms maken ook Nederlanders kans op een van die Oscars. 177 18:39.090 --> 18:43.060 Zoals vannacht. Drie Nederlandse filmmakers, Job, Joris en Marieke... 178 18:43.110 --> 18:46.160 zijn genomineerd voor een Oscar voor beste korte animatie. 179 18:46.210 --> 18:49.060 Hun film heet A Single Life. 180 18:49.110 --> 18:52.050 In dit korte filmpje ontdekt het meisje Pia... 181 18:52.100 --> 18:56.150 dat ze via een plaat op haar platenspeler grote sprongen door haar leven kan maken. 182 18:56.200 --> 19:00.240 Als de plaat achteruit gaat, wordt ze jonger, als hij vooruit gaat, wordt ze ouder. 183 19:01.040 --> 19:03.180 Totdat de plaat afloopt. 184 19:03.230 --> 19:08.090 Toen de makers hoorden dat hun film genomineerd was... 185 19:08.140 --> 19:10.240 konden ze het amper geloven. 186 19:12.010 --> 19:13.130 LUID GEJUICH 187 19:13.180 --> 19:19.020 Een enorme eer en kans dus op de belangrijkste prijs die ze maar kunnen winnen. 188 19:19.070 --> 19:24.090 Wij konden Job, Joris en Marieke vlak voor de Oscar-uitreiking nog even spreken. 189 19:24.140 --> 19:26.230 Ze zijn al behoorlijk zenuwachtig. 190 19:27.030 --> 19:31.010 En dat was filmregisseur Mike van Diem 17 jaar geleden ook. 191 19:31.060 --> 19:32.210 Toen won hij een Oscar. 192 19:33.010 --> 19:37.040 Lucas zocht hem op en mocht het felbegeerde beeldje even vasthouden. 193 19:40.150 --> 19:42.170 TUNE 194 19:55.120 --> 19:57.170 Mike, dit is 'm dan. Ja, dat is 'm. 195 19:57.220 --> 20:00.210 Je mag 'm even vastpakken. Echt? Ja, zeker. 196 20:01.010 --> 20:02.230 Zwaar. Waar staat-ie? 197 20:03.030 --> 20:07.100 Hij staat op een geheime plek, Lucas. Dat ga ik niet vertellen. 198 20:07.150 --> 20:12.080 Als ik een Oscar zou winnen, dan zou ik 'm elke avond een kusje geven. 199 20:12.130 --> 20:16.230 Ik zal je wel een geheim vertellen: Dat doe je in het begin wel. 200 20:17.030 --> 20:22.080 Ja? In het begin laat je 'm bijna nooit los. 201 20:22.130 --> 20:25.080 En sliep die Oscar altijd in hetzelfde bed. 202 20:25.130 --> 20:29.040 Die had een eigen kussentje? Die lag op het andere kussen, ja. 203 20:29.090 --> 20:34.160 Met de film Karakter won Mike van Diem de Oscar voor beste buitenlandse film. 204 20:34.210 --> 20:40.100 Het is ontzettend leuk en spannend om aan die race mee te doen. 205 20:40.150 --> 20:44.170 Maar ook die hele toestand eromheen, die rode loper... 206 20:44.220 --> 20:48.000 en het feit dat er een maatkostuum voor je gemaakt wordt... 207 20:48.050 --> 20:51.000 en dat je een dure auto krijgt om in te rijden... 208 20:51.050 --> 20:53.090 en een mooi horloge om krijgt. 209 20:53.140 --> 20:58.130 Dat hele glamourachtige gedoe, dat is ook hartstikke leuk. 210 20:59.170 --> 21:03.140 Wij zijn Job, Joris en Marieke. We zijn in Hollywood... 211 21:03.190 --> 21:06.030 omdat onze film is genomineerd voor een Oscar. 212 21:06.080 --> 21:09.200 Job, Joris en Marieke maken die glamourwereld nu van dichtbij mee. 213 21:10.000 --> 21:13.100 Al de hele week zitten ze in grote spanning voor de uitreiking. 214 21:13.150 --> 21:16.020 We hebben onze kleren al klaar. 215 21:16.070 --> 21:19.100 Ik heb al een jurk en Job en Joris hebben al een pak. 216 21:19.150 --> 21:22.010 We gaan er straks naartoe in een auto. 217 21:22.060 --> 21:25.230 We gaan niet in een echte limousine, maar wel in een mooie auto. 218 21:26.030 --> 21:28.100 En dan moeten we over de rode loper. 219 21:28.150 --> 21:32.040 Mike weet nog hoe zenuwachtig hij was vlak voor de uitreiking. 220 21:32.090 --> 21:36.140 Voor het eerst in jaren ziet hij de beelden terug van zijn Oscar-winst. 221 21:36.190 --> 21:39.160 En op het moment dat zij nu bekendmaakt... 222 21:39.210 --> 21:42.100 welke films de nominaties hebben... 223 21:42.150 --> 21:45.220 knielt er naast mij echt zo'n cameraman. 224 21:46.020 --> 21:48.180 Echt een halve meter naast mij. 225 21:48.230 --> 21:51.010 En ik denk: o my God! 226 21:51.060 --> 21:53.070 Betekent dit dat ik ga winnen? 227 21:56.070 --> 21:59.180 'The Oscar goes to The Netherlands.' Ja, en dan... 228 21:59.230 --> 22:01.230 Ja, dan ben je er even niet. 229 22:02.030 --> 22:04.170 Nee? Dan zweef je? Ja. 230 22:04.220 --> 22:07.150 Hier zie je die rare vreugdedansjes al. 231 22:07.200 --> 22:10.040 Ja, dit is blijdschap in het kwadraat. 232 22:10.090 --> 22:15.090 Job, Joris en Marieke hopen dit ook mee te maken, maar dan moet je wel op je speech letten. 233 22:18.180 --> 22:22.240 Je moet goed voorbereid hebben wat je precies wil zeggen... 234 22:23.040 --> 22:26.170 want je hebt maar 45 seconden. Anders is het moment weer voorbij. 235 22:26.220 --> 22:29.070 En wat denk jij? Hoe ga je reageren? 236 22:29.120 --> 22:32.120 Ik denk dat we alle drie keihard gaan schreeuwen. 237 22:33.140 --> 22:36.150 Dan gaat de hartslag van dat drietal enorm omhoog. 238 22:36.200 --> 22:38.180 Ik hoop echt dat ze winnen. 239 22:38.230 --> 22:43.010 En zelf zou Mike dat beeldje best nog een keer willen winnen. 240 22:43.060 --> 22:48.000 De Oscars doen iets met je. Het is toch de prijs der prijzen. 241 22:52.010 --> 22:56.090 Misschien wint-ie er weer EEN met z'n nieuwste film: De surprise. 242 22:56.140 --> 23:00.050 Een romantische komedie die dit voorjaar in de bios draait. 243 23:00.100 --> 23:04.000 Het is z'n eerste speelfilm na het winnen van de Oscar. 244 23:04.050 --> 23:08.220 En of Job, Joris en Marieke gewonnen hebben, dat weten we pas morgen. 245 23:12.220 --> 23:15.130 Tijd voor het nieuwsdier van de week. 246 23:15.180 --> 23:19.190 Een reusachtig dier dat zin had om voor toerist te spelen. 247 23:19.240 --> 23:21.210 MUZIEK 248 23:22.010 --> 23:25.150 De bult op z'n rug. Dat is zo'n beetje wat het dier van deze week... 249 23:25.200 --> 23:28.080 van zichzelf liet zien. Niet echt spannend. 250 23:28.130 --> 23:32.000 Maar wel spannend als je bedenkt wat voor een enorm dier het is. 251 23:32.050 --> 23:36.020 Dit is een bultrug. Dit exemplaar wordt geschat op zo'n 10 meter lang. 252 23:36.070 --> 23:39.040 Best klein, want ze kunnen wel 15 meter worden. 253 23:39.090 --> 23:42.030 De plek waar-ie zwom was ook spannend. 254 23:42.080 --> 23:44.240 Hij zwom namelijk in de Oosterschelde. 255 23:45.040 --> 23:47.120 Een zeearm, in de provincie Zeeland. 256 23:47.170 --> 23:49.180 En daar hoort-ie niet thuis. 257 23:49.230 --> 23:52.100 Bultruggen horen op volle zee. 258 23:52.150 --> 23:55.180 Daar kunnen ze lekker jagen op garnaaltjes en vis. 259 23:55.230 --> 23:58.010 MUZIEK 260 23:59.150 --> 24:05.200 Af en toe trekken ze langs de Nederlandse kust. Je moet veel geluk hebben om ze toevallig te zien. 261 24:06.000 --> 24:07.230 Nee, niets te zien hier. 262 24:08.030 --> 24:12.040 Maar heel soms komen ze dichtbij, zoals de Oosterschelde-bultrug. 263 24:12.090 --> 24:14.080 Even zag 't er slecht voor 'm uit. 264 24:14.130 --> 24:20.000 Hij was via de Oosterscheldekering binnengezwommen, en in dit gebied is er nauwelijks voedsel te vinden. 265 24:20.050 --> 24:24.070 Weet-ie de weg naar buiten ook weer? Dat liet-ie duidelijk blijken. 266 24:26.170 --> 24:29.120 Net toen veel mensen dachten dat hij zou verdwalen... 267 24:29.170 --> 24:34.240 zwom hij een paar dagen later helemaal zelf terug naar volle zee. 268 24:35.040 --> 24:38.230 Klaar met z'n toeristische bezoekje aan Zeeland. 269 24:43.050 --> 24:46.150 Kwebbelkop. Veel kinderen denken nu: 270 24:46.200 --> 24:50.030 Dat wil ik ook wel, bekend worden op YouTube. 271 24:50.080 --> 24:55.030 En Bo heeft een vraag die veel kijkers nu wel willen stellen. 272 24:55.080 --> 24:58.070 Hoe ben je begonnen met je YouTube-kanaal? 273 24:58.120 --> 25:01.040 Ik ben begonnen met m'n YouTube-kanaal... 274 25:01.090 --> 25:05.150 iets langer dan twee jaar geleden. Ik ben begonnen met filmpjes maken. 275 25:05.200 --> 25:09.120 Nee, m'n YouTube-kanaal nog veel langer dan twee jaar geleden... 276 25:09.170 --> 25:13.150 want ik maakte grappige filmpjes voor schoolprojecten... 277 25:13.200 --> 25:17.060 en die uploadden m'n vrienden dan... 278 25:17.110 --> 25:20.100 of ik uploadde iets. En zo zijn we begonnen. 279 25:20.150 --> 25:25.100 We hebben een camera gepakt en we hebben gekeken: Wat kunnen we doen? 280 25:25.150 --> 25:28.060 En dan filmen jullie dit soort dingen. 281 25:28.110 --> 25:33.000 Hello. My name is Pippi Langkous. I'm your personal trainer. 282 25:33.050 --> 25:37.020 MUZIEK *Hai Pippi Langstrumpf, die macht was ihr gef�llt* 283 25:37.070 --> 25:39.060 Jordi, wat is dit? 284 25:39.110 --> 25:42.060 Dit was voor een Engels schoolproject. 285 25:42.110 --> 25:44.200 Toen was ik 13, denk ik. 286 25:45.000 --> 25:48.090 En we gingen een grappig filmpje maken. 287 25:48.140 --> 25:52.110 En ik vind het ook nog best wel leuk. Het is ook wel heel grappig. 288 25:52.160 --> 25:55.120 Toen wist jij al: 'Hier wil ik later iets mee gaan doen'? 289 25:55.170 --> 25:59.220 Nee. Op dat moment dacht ik nog: Ik vind het gewoon leuk om te doen. 290 26:00.020 --> 26:02.220 En ik wist niet wat je ermee kon bereiken. 291 26:03.020 --> 26:05.010 Het duurde echt een paar jaar... 292 26:05.060 --> 26:08.020 en een paar van dit soort filmpjes geupload... 293 26:08.070 --> 26:11.210 en toen dacht ik: Misschien wil ik dit wel echt doen. 294 26:12.010 --> 26:14.130 En toen ben ik het dus gewoon gaan doen. 295 26:14.180 --> 26:19.180 Je bent zo heel populair geworden. Noelle heeft daar een vraag over. 296 26:19.230 --> 26:23.190 Hoi. Hoe is het om op straat herkend te worden? Is dat leuk? 297 26:23.240 --> 26:26.100 Op straat herkend worden is altijd leuk. 298 26:26.150 --> 26:30.170 Ik zeg ook in m'n filmpjes: Als je me ziet, roep dan 'Kwebbelkop'... 299 26:30.220 --> 26:33.130 of roep iets wat wij alleen begrijpen. 300 26:33.180 --> 26:36.010 Zodat anderen denken: Wat is dat nou weer? 301 26:36.060 --> 26:39.020 Maar ik vind het altijd leuk om fans te ontmoeten... 302 26:39.070 --> 26:42.110 en om foto's te nemen, handtekeningen uit te delen. 303 26:42.160 --> 26:45.220 Maar soms zijn er ook reacties die niet zo leuk zijn. 304 26:46.020 --> 26:48.120 Je hebt een keer een filmpje gemaakt... 305 26:48.170 --> 26:51.200 waarin je allemaal negatieve reacties voorleest. 306 27:06.180 --> 27:09.080 Je doet net alsof je echt je haar gaat afknippen. 307 27:09.130 --> 27:12.030 Ik heb m'n haar nog, hoor. Ja, ik zie het. 308 27:12.080 --> 27:16.170 Maar raken dit soort dingen je soms toch weleens echt ook? 309 27:16.220 --> 27:18.230 Ehm, nee, niet meer. 310 27:19.030 --> 27:22.040 Ik krijg zo veel slechte comments... 311 27:22.090 --> 27:25.220 maar ik krijg ook zo veel goede comments, dat het... 312 27:26.020 --> 27:30.000 Het maakt mij niet zo veel uit. Er worden wel hele nare dingen gezegd. 313 27:30.050 --> 27:33.120 Ja. Ik heb ook wel een paar echt nare uitgekozen. 314 27:33.170 --> 27:35.210 Maar zo is het gewoon. 315 27:36.010 --> 27:39.160 En ik... Joh, laat ze lekker praten. Ik doe lekker m'n ding. 316 27:39.210 --> 27:42.130 Ik vind het leuk en al m'n fans vinden het ook leuk. 317 27:42.180 --> 27:46.150 En daar doe ik het voor. Als er dan EEN iemand chagrijnig is, laat 'm. 318 27:46.200 --> 27:49.060 Dan kun je 't makkelijk naast je neerleggen. Ja. 319 27:49.110 --> 27:53.010 Want je bent hartstikke populair. Maar je werkt er ook hard voor, he? 320 27:53.060 --> 27:55.150 Ja. Hoeveel uur per dag werk je hieraan? 321 27:55.200 --> 27:58.200 10 tot 14 uur per dag, elke dag... 322 27:59.000 --> 28:01.220 en ik heb geen vakantie. Ehm... 323 28:02.020 --> 28:04.010 Wat doe je allemaal in die... 324 28:04.060 --> 28:07.210 Ik neem op, ik edit, ik, eh... 325 28:08.010 --> 28:12.170 Ik bedenk: Wat ga ik doen in m'n filmpjes? Voorbereidingen. En, eh... 326 28:12.220 --> 28:16.130 Je bedenkt het echt van tevoren, wat je gaat doen? Ja. 327 28:16.180 --> 28:20.170 Het ziet er heel spontaan uit. Ik wil ook wel dat het spontaan is... 328 28:20.220 --> 28:23.130 en dat niet alles uitgeschreven is... 329 28:23.180 --> 28:26.100 maar ik weet wel wat ik wil bereiken in het filmpje... 330 28:26.150 --> 28:28.220 en waar ik een grapje wil maken... 331 28:29.020 --> 28:33.100 of misschien dat ik iets grappigs juist opzoek in het filmpje. 332 28:33.150 --> 28:36.230 Dat fans zeggen: 'Je moet dit proberen.' En dat ik ga kijken. 333 28:37.030 --> 28:40.140 En dan blijkt het meestal best leuk te zijn. Het is dus echt een baan. 334 28:40.190 --> 28:44.170 Heb je nog tijd voor andere dingen? Ehm, ja, alleen... 335 28:44.220 --> 28:49.020 Ik doe... Ik vind het gewoon altijd heel leuk met m'n YouTube. 336 28:49.070 --> 28:53.210 Al m'n vrije tijd gaat meestal naar nog meer YouTube-dingen doen. 337 28:54.010 --> 28:56.100 Fan meetups vind ik ook heel leuk. 338 28:56.150 --> 29:00.080 Je zit dus veel achter de computer. Kom je nog weleens buiten? 339 29:00.130 --> 29:04.120 Daar maken we altijd grapjes over, maar ik kom wel buiten. 340 29:04.170 --> 29:07.080 Lekker als het lekker weer is. Lekker... 341 29:07.130 --> 29:11.030 Ik weet wat jij deze week gaat doen. Je gaat 14 filmpjes uploaden. 342 29:11.080 --> 29:15.170 Je gaat Grand Theft Auto spelen. Misschien kom je nog even buiten. 343 29:15.220 --> 29:18.140 Maar er gebeuren nog veel meer dingen deze week. 344 29:18.190 --> 29:21.010 Kijk mee naar onze nieuwsagenda. 345 29:21.060 --> 29:23.220 Veel Nederlanders hebben voorjaarsvakantie. 346 29:24.020 --> 29:27.120 De koninklijke familie gaat dan altijd skien in Oostenrijk. 347 29:27.170 --> 29:32.000 Morgen zijn veel persfotografen erbij om foto's te maken. 348 29:35.010 --> 29:39.040 Al sinds 1949 rijden ze rond: Vrachtwagens van het merk DAF. 349 29:39.090 --> 29:43.180 Woensdag presenteert het Nederlandse bedrijf de een miljoenste DAF-truck. 350 29:43.230 --> 29:46.180 Veel Nederlanders zijn deze week in de skigebieden... 351 29:46.230 --> 29:52.110 en de allerbeste jeugdskiers strijden in Oostenrijk om het Nederlands kampioenschap. 352 29:52.160 --> 29:56.000 Vrijdag wordt een recordpoging gedaan om kleding in te zamelen. 353 29:56.050 --> 30:00.100 Binnen 24 uur meer dan 30.000 kilo. Het gebeurt op de Huishoudbeurs... 354 30:00.150 --> 30:02.210 en de kleding gaat naar een goed doel. 355 30:03.010 --> 30:04.150 GEZANG EN GETROMMEL 356 30:04.200 --> 30:08.040 Bij voetbalwedstrijden hoor je vaak trommelende supporters. 357 30:08.090 --> 30:10.210 Maar soms doen die trommelaars maar wat. 358 30:11.010 --> 30:13.050 Vitesse wil daar verandering in brengen. 359 30:13.100 --> 30:16.240 De voetbalclub uit Arnhem gaat trommelles geven. 360 30:17.040 --> 30:19.200 En Ajax kan wel wat trommelsupport gebruiken... 361 30:20.000 --> 30:25.210 want om nog een kansje op het kampioenschap te houden, moet Ajax zondag winnen van nummer EEN PSV. 362 30:26.010 --> 30:28.010 Jordi, dank je wel dat je hier was. 363 30:28.060 --> 30:31.100 Voor filmpjes van Kwebbelkop kun je terecht op internet. 364 30:31.150 --> 30:36.000 En filmpjes over het nieuws vind je op schooltv.nl. 365 30:36.050 --> 30:40.090 En het laatste nieuws hoor je straks bij het Jeugdjournaal, hier op Zapp. 366 30:40.140 --> 30:43.000 Een hele fijne avond nog. Doeg. 367 30:44.240 --> 30:48.230 NPO ONDERTITELING TT888, 2015 tt888reacties(a)npo.nl";
        test.getBytes(ISO6937CharsetProvider.ISO6937);
    }

    @Test
    public void allChars() {
        StringBuilder example = new StringBuilder(
            "AEIOUaeiou	ÀÈÌÒÙàèìòù\n" +
                "ACEGILNORSUYZacegilnorsuyz	ÁĆÉÍĹŃÓŔŚÚÝŹáćéíĺńóŕśúýź\n" + // TODO I actually thing G actute, g actue should have been supported too?
                "ACEGHIJOSUWYaceghijosuwy	ÂĈÊĜĤÎĴÔŜÛŴŶâĉêĝĥîĵôŝûŵŷ\n" +
                "AINOUainou	ÃĨÑÕŨãĩñõũ\n" +
                "AEIOUaeiou	ĀĒĪŌŪāēīōū\n" +
                "AGUagu	ĂĞŬăğŭ\n" +
                "CEGIZcegz	ĊĖĠİŻċėġż\n" +
                "EIOUYaeiouy	ÄËÏÖÜŸäëïöüÿ\n" +
                "AUau	ÅŮåů\n" +
                "CGKLNRSTcklnrst	ÇĢĶĻŅŖŞŢçķļņŗşţ\n" +
                "OUou	ŐŰőű\n" +
                "AEIUaeiu	ĄĘĮŲąęįų\n" +
                "CDELNRSTZcdelnrstz	ČĎĚĽŇŘŠŤŽčďěľňřšťž\n");

        example.append("¡\n" +
            "¢\n" +
            "£\n" +
            "¤\n" +
            "¥\n" +
            //"¦\n" + // commented out ones somewhy don't work
            "§\n" +
            "¨\n" +
            //"©\n" +
            "ª\n" +
            "«\n" +
            //"¬\n" +
            "\u00AD\n" +
            //"®\n" +
            //"¯\n" +
            "°\n" +
            "±\n" +
            "²\n" +
            "³\n" +
            "´\n" +
            "µ\n" +
            "¶\n" +
            "·\n" +
            "¸\n" +
            //"¹\n" +
            "º\n" +
            "»\n" +
            "¼\n" +
            "½\n" +
            "¾\n");

        byte[] bytes = example.toString().getBytes(ISO6937);
        String rounded = new String(bytes, ISO6937);
        Assertions.assertThat(rounded).isEqualTo(example.toString());


    }
}

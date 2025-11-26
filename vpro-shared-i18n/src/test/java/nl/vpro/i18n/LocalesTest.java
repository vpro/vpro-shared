package nl.vpro.i18n;

import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.meeuw.i18n.countries.Country;
import org.meeuw.i18n.regions.RegionService;

import static java.time.DayOfWeek.MONDAY;
import static java.util.Locale.US;
import static nl.vpro.i18n.Locales.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.meeuw.i18n.languages.ISO_639.iso639;

/**
 * @author Michiel Meeuwissen
 */
class LocalesTest {

    @Test
    public void ofString() {
        assertThat(Locales.ofString("nl-NL-informal")).isEqualTo(new Locale("nl", "NL", "informal"));
        assertThat(Locales.ofString(null)).isNull();
    }


    @SuppressWarnings("DataFlowIssue")
    @Test
    public void ofAlpha3() {
        assertThat(Locales.of(iso639("dut"), RegionService.getInstance().getByCode("NL", Country.class).orElse(null))).isEqualTo(new Locale("nl", "NL"));

        assertThat(Locales.of(iso639("afh"), RegionService.getInstance().getByCode("KE", Country.class).orElse(null))).isEqualTo(new Locale("afh", "KE"));
    }

    @Test
    public void getCountryName() {
        assertThat(Locales.getCountryName(
            RegionService.getInstance().getByCode("NL", Country.class).orElseThrow(IllegalStateException::new), Locales.of(iso639("nld")))).isEqualTo("Nederland");
    }


    @Test
    public void findBestMatchDutch() {
        assertThat(Locales.findBestMatch(DUTCH, Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
        ))).contains(new Locale("nl"));
    }

    @Test
    @Deprecated
    public void findBestMatchDutchU() {
         assertThat(Locales.findBestMatch(DUTCH_U, Stream.of(
             new Locale("en"),
             new Locale("nl", "BE"),
             new Locale("nl", "NL"),
             new Locale("nl")
         ))).contains(new Locale("nl"));
     }

    @Test
    public void findBestMatchFlemish() {
        assertThat(Locales.findBestMatch(FLEMISH, Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
        ))).contains(new Locale("nl", "BE"));
    }

    @Test
    public void findBestMatchNoMatch() {

        assertThat(Locales.findBestMatch(new Locale("fr"), Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl"),
            null
        ))).isNotPresent();

        assertThat(Locales.findBestMatch(null, Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
        ))).isNotPresent();

    }

    @Test
    public void findBestMatchInformalDutch() {

          assertThat(Locales.findBestMatch(new Locale("nl", "NL", "informal"), Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
            ))).contains(NETHERLANDISH);


        assertThat(Locales.findBestMatch(new Locale("nl", "NL", "informal"), Stream.of(
            new Locale("en"),
            null,
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl", "BE", "informal"),
            new Locale("nl", "NL", "informal"),
            new Locale("nl", "NL", "formal"),
            new Locale("nl")
            ))).contains(new Locale("nl", "NL", "informal"));

    }

    @Test
    public void simplify() {
        assertThat(Locales.simplify(null)).isNull();
        assertThat(Locales.simplify(new Locale("nl"))).isEqualTo(new Locale("nl"));
        assertThat(Locales.simplify(new Locale("nl", "BE"))).isEqualTo(new Locale("nl"));
        assertThat(Locales.simplify(new Locale("nl", "BE", "youth"))).isEqualTo(new Locale("nl", "BE"));
    }

    @Test
    public void simplifiable() {
        assertThat(Locales.simplifyable(null)).isFalse();
        assertThat(Locales.simplifyable(new Locale("nl"))).isFalse();
        assertThat(Locales.simplifyable(new Locale("nl", "BE"))).isTrue();
        assertThat(Locales.simplifyable(new Locale("nl", "BE", "youth"))).isTrue();
        assertThat(Locales.simplifyable(new Locale("nl", "", ""))).isFalse();
        assertThat(Locales.simplifyable(new Locale("nl", "", "youth"))).isTrue();
    }

    @Test
    public void setDefault() {
        Locale.setDefault(DUTCH);
        Locales.resetDefault();
        assertThat(Locales.getDefault()).isEqualTo(DUTCH);
        Locales.setDefault(US);
        assertThat(Locales.getDefault()).isEqualTo(US);
        Locales.resetDefault();
        Locales.setDefault(DUTCH);

        try (Locales.RestoreDefaultLocale ac = Locales.with(FLEMISH)) {
            assertThat(Locales.getDefault()).isEqualTo(FLEMISH);
        }
        Locales.setDefault(DUTCH);

    }


    @Test
    public void dutch() {
        assertThat(WeekFields.of(NETHERLANDISH).getFirstDayOfWeek()).isEqualTo(MONDAY);

        //FAILS in java >=11
        //assertThat(WeekFields.of(Locales.DUTCH).getFirstDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(DUTCH.toLanguageTag()).isEqualTo("nl");
        assertThat(DUTCH.toString()).isEqualTo("nl");
    }

    @Test
    @Deprecated
    public void dutchU() {

        // this may be used.
        assertThat(WeekFields.of(DUTCH_U).getFirstDayOfWeek()).isEqualTo(MONDAY);



        assertThat(DUTCH_U.toLanguageTag()).isEqualTo("nl");
        assertThat(DUTCH_U.toString()).isEqualTo("nl_UNDEFINED");
    }

    @Test
    public void undefined() {
        assertThat(UNDETERMINED.toString()).isEqualTo("und");
    }
}

package nl.vpro.i18n;

import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.meeuw.i18n.countries.Country;
import org.meeuw.i18n.regions.RegionService;

import com.neovisionaries.i18n.LanguageAlpha3Code;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
class LocalesTest {

    @Test
    public void ofString() {
        assertThat(Locales.ofString("nl-NL-informal")).isEqualTo(new Locale("nl", "NL", "informal"));
        assertThat(Locales.ofString(null)).isNull();
    }


    @Test
    public void ofAlpha3() {
        assertThat(Locales.of(LanguageAlpha3Code.dut, RegionService.getInstance().getByCode("NL", Country.class).orElse(null))).isEqualTo(new Locale("nl", "NL"));

        assertThat(Locales.of(LanguageAlpha3Code.afh, RegionService.getInstance().getByCode("KE", Country.class).orElse(null))).isEqualTo(new Locale("afh", "KE"));
    }

    @Test
    public void getCountryName() {
        assertThat(Locales.getCountryName(
            RegionService.getInstance().getByCode("NL", Country.class).orElseThrow(IllegalStateException::new), Locales.of(LanguageAlpha3Code.nld))).isEqualTo("Nederland");
    }


    @Test
    public void findBestMatch() {
        assertThat(Locales.findBestMatch(Locales.DUTCH, Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
            ))).contains(new Locale("nl"));

        assertThat(Locales.findBestMatch(Locales.FLEMISH, Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
            ))).contains(new Locale("nl", "BE"));

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


          assertThat(Locales.findBestMatch(new Locale("nl", "NL", "informal"), Stream.of(
            new Locale("en"),
            new Locale("nl", "BE"),
            new Locale("nl", "NL"),
            new Locale("nl")
            ))).contains(Locales.NETHERLANDISH);


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
        Locale.setDefault(Locales.DUTCH);
        Locales.resetDefault();
        assertThat(Locales.getDefault()).isEqualTo(Locales.DUTCH);
        Locales.setDefault(Locale.US);
        assertThat(Locales.getDefault()).isEqualTo(Locale.US);
        Locales.resetDefault();
        Locales.setDefault(Locales.DUTCH);

        try (Locales.RestoreDefaultLocale ac = Locales.with(Locales.FLEMISH)) {
            assertThat(Locales.getDefault()).isEqualTo(Locales.FLEMISH);
        }
        Locales.setDefault(Locales.DUTCH);

    }


    @Test
    public void dutch() {
        assertThat(WeekFields.of(Locales.NETHERLANDISH).getFirstDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        //FAILS in java >=11
        //assertThat(WeekFields.of(Locales.DUTCH).getFirstDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }
}

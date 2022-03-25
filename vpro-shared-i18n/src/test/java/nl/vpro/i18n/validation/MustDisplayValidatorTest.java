package nl.vpro.i18n.validation;

import org.junit.jupiter.api.Test;

import nl.vpro.i18n.Displayable;

import static org.assertj.core.api.Assertions.assertThat;

class MustDisplayValidatorTest {

    private final DisplayableValidator validator = new DisplayableValidator();

    enum TestEnum implements Displayable {
        a,
        b,
        c,
        d() {
            @Override
            public boolean display() {
                return false;
            }
        };

        @Override
        public String getDisplayName() {
            return name();
        }

    }


    @Test
    void isValid() {

        assertThat(validator.isValid(TestEnum.a,null)).isTrue();
        assertThat(validator.isValid(TestEnum.d,null)).isFalse();
    }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class BlockSurrenderTypeFormValidatorTest {

  private static final String SELECT_SURRENDER_TYPE = "Select the type of surrender for this block";

  private final BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator =
      new BlockSurrenderTypeFormValidator();

  private BlockSurrenderTypeForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    form = new BlockSurrenderTypeForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "NOT_A_TYPE"})
  void hasErrors_whenSurrenderTypeBlankOrUnrecognised_thenErrorWithMessage(String surrenderType) {
    form.setSurrenderType(surrenderType);

    var result = blockSurrenderTypeFormValidator.hasErrors(form, errors);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("surrenderType", Collections.singletonList(SELECT_SURRENDER_TYPE)));
  }

  @Test
  void hasErrors_whenSurrenderTypeIsValid_thenNoErrors() {
    form.setSurrenderType(BlockSurrenderType.FULL_SURRENDER.name());

    var result = blockSurrenderTypeFormValidator.hasErrors(form, errors);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}

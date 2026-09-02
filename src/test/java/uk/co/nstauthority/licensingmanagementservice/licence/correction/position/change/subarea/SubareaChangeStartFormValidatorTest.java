package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class SubareaChangeStartFormValidatorTest {

  private static final Feature FIRST_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature SECOND_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
  private static final List<Feature> BLOCK_FEATURES = List.of(FIRST_BLOCK, SECOND_BLOCK);
  private static final String SELECT_BLOCK = "Select the licence block to change";

  private final SubareaChangeStartFormValidator subareaChangeStartFormValidator =
      new SubareaChangeStartFormValidator();

  private SubareaChangeStartForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    form = new SubareaChangeStartForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoBlockSelected_thenErrorWithMessage() {
    var result = subareaChangeStartFormValidator.hasErrors(form, errors, BLOCK_FEATURES);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("featureId", Collections.singletonList(SELECT_BLOCK)));
  }

  @Test
  void hasErrors_whenBlockNotOnPosition_thenErrorWithMessage() {
    form.setFeatureId(UUID.randomUUID().toString());

    var result = subareaChangeStartFormValidator.hasErrors(form, errors, BLOCK_FEATURES);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("featureId", Collections.singletonList(SELECT_BLOCK)));
  }

  @Test
  void hasErrors_whenSelectedBlockOnPosition_thenNoErrors() {
    form.setFeatureId(FIRST_BLOCK.getId().toString());

    var result = subareaChangeStartFormValidator.hasErrors(form, errors, BLOCK_FEATURES);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class PartialSurrenderDetailsFormValidatorTest {

  private static final Feature FIRST_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature SECOND_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
  private static final UUID FIRST_FEATURE_ID = FIRST_BLOCK.getId();
  private static final UUID SECOND_FEATURE_ID = SECOND_BLOCK.getId();
  private static final UUID UNLINKED_FEATURE_ID = UUID.randomUUID();
  private static final List<Feature> SURRENDERABLE_BLOCKS = List.of(FIRST_BLOCK, SECOND_BLOCK);
  private static final String SELECT_BLOCKS = "Select the licence blocks being surrendered";

  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator =
      new PartialSurrenderDetailsFormValidator();

  private PartialSurrenderDetailsForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    form = new PartialSurrenderDetailsForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoBlocksSelected_thenErrorWithMessage() {
    var result = partialSurrenderDetailsFormValidator.hasErrors(form, errors, SURRENDERABLE_BLOCKS);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("featureIds", Collections.singletonList(SELECT_BLOCKS)));
  }

  @Test
  void hasErrors_whenBlockNotOnPosition_thenErrorWithMessage() {
    form.setFeatureIds(new LinkedHashSet<>(List.of(FIRST_FEATURE_ID, UNLINKED_FEATURE_ID)));

    var result = partialSurrenderDetailsFormValidator.hasErrors(form, errors, SURRENDERABLE_BLOCKS);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("featureIds", Collections.singletonList(SELECT_BLOCKS)));
  }

  @Test
  void hasErrors_whenNoBlocksOnPosition_thenErrorWithMessage() {
    form.setFeatureIds(new LinkedHashSet<>(List.of(FIRST_FEATURE_ID)));

    var result = partialSurrenderDetailsFormValidator.hasErrors(form, errors, List.of());

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry("featureIds", Collections.singletonList(SELECT_BLOCKS)));
  }

  @Test
  void hasErrors_whenAllSelectedBlocksOnPosition_thenNoErrors() {
    form.setFeatureIds(new LinkedHashSet<>(List.of(FIRST_FEATURE_ID, SECOND_FEATURE_ID)));

    var result = partialSurrenderDetailsFormValidator.hasErrors(form, errors, SURRENDERABLE_BLOCKS);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }
}

package uk.co.nstauthority.licensingmanagementservice.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class FeedbackFormValidatorTest {

  private FeedbackFormValidator feedbackFormValidator;

  private Errors errors;

  private Map<String, List<String>> errorMap;

  private FeedbackForm feedbackForm;


  @BeforeEach
  void setUp() {
    feedbackForm = new FeedbackForm();
    feedbackFormValidator = new FeedbackFormValidator();
    errors = new BeanPropertyBindingResult(feedbackForm, "form");
  }

  @Test
  void validate_whenValidForm_thenNoValidationErrors() {
    feedbackForm.setServiceRating(ServiceFeedbackRating.SATISFIED.name());

    errorMap = ValidatorTestingUtil.getErrorsFieldsAndMessages(errors);

    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void validate_whenEmptyForm_thenError() {
    ValidationUtils.invokeValidator(feedbackFormValidator, feedbackForm, errors);

    errorMap = ValidatorTestingUtil.getErrorsFieldsAndMessages(errors);

    assertThat(errorMap)
        .containsOnly(
            entry("serviceRating",
                Collections.singletonList(FeedbackFormValidator.SERVICE_RATING_REQUIRED_ERROR_MESSAGE))
        );
  }

  @Test
  void validate_whenInvalidRating_thenError() {
    feedbackForm.setServiceRating("invalid rating");

    ValidationUtils.invokeValidator(feedbackFormValidator, feedbackForm, errors);

    errorMap = ValidatorTestingUtil.getErrorsFieldsAndMessages(errors);

    assertThat(errorMap)
        .containsOnly(
            entry("serviceRating",
                Collections.singletonList(FeedbackFormValidator.SERVICE_RATING_REQUIRED_ERROR_MESSAGE))
        );
  }

  @Test
  void validate_feedbackInput_characterCountOverMax() {
    var overCharacterLimit = StringUtils.repeat("a", 2000 + 1);

    feedbackForm.setServiceRating(ServiceFeedbackRating.SATISFIED.name());
    feedbackForm.getFeedback().setInputValue(overCharacterLimit);

    ValidationUtils.invokeValidator(feedbackFormValidator, feedbackForm, errors);
    errorMap = ValidatorTestingUtil.getErrorsFieldsAndMessages(errors);

    assertThat(errorMap)
        .containsOnly(
            entry("feedback.inputValue",
                Collections.singletonList("Feedback must be %s characters or less".formatted(2000))
            )
        );
  }

  @Test
  void validate_feedbackInput_characterCountUnderMaxWithNewLine() {
    var exactCharacterLimit = StringUtils.repeat("a", 2000);

    feedbackForm.setServiceRating(ServiceFeedbackRating.SATISFIED.name());
    feedbackForm.getFeedback().setInputValue(exactCharacterLimit);

    ValidationUtils.invokeValidator(feedbackFormValidator, feedbackForm, errors);
    errorMap = ValidatorTestingUtil.getErrorsFieldsAndMessages(errors);

    assertThat(errors.hasErrors()).isFalse();
  }
}

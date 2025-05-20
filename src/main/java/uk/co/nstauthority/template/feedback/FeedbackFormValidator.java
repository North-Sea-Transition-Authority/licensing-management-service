package uk.co.nstauthority.template.feedback;

import jakarta.validation.constraints.NotNull;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
public class FeedbackFormValidator implements Validator {

  public static final String SERVICE_RATING_REQUIRED_ERROR_MESSAGE = "Select how you felt about this service";

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return FeedbackForm.class.equals(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    var form = (FeedbackForm) target;

    ValidationUtils.rejectIfEmpty(
        errors,
        "serviceRating",
        "serviceRating.required",
        SERVICE_RATING_REQUIRED_ERROR_MESSAGE);

    if (StringUtils.isNotBlank(form.getServiceRating()) && !isValidServiceFeedbackRatingValue(form.getServiceRating())) {
      errors.rejectValue("serviceRating",
          "serviceRating.required",
          SERVICE_RATING_REQUIRED_ERROR_MESSAGE);
    }

    StringInputValidator.builder()
        .isOptional()
        .mustHaveCharacterCountAtMost(2000)
        .validate(form.getFeedback(), errors);
  }

  private static boolean isValidServiceFeedbackRatingValue(String serviceRating) {
    return Stream.of(ServiceFeedbackRating.class.getEnumConstants())
        .map(Enum::name)
        .toList()
        .contains(serviceRating);
  }
}

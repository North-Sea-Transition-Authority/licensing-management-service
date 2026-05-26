package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class EventCommentValidator {

  boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "comment",
        "comment.required",
        "Enter a comment"
    );

    return !errors.hasErrors();
  }
}

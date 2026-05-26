package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class EventCommentValidatorTest {

  @InjectMocks
  private EventCommentValidator validator;

  @Test
  void isValid_validForm_returnsTrue() {
    var form = new EventCommentForm();
    form.setComment("This is a comment");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isTrue();
  }

  @Test
  void isValid_nullComment_returnsFalse() {
    var form = new EventCommentForm();
    form.setComment(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrors(bindingResult)).containsKey("comment");
  }

  @Test
  void isValid_emptyComment_returnsFalse() {
    var form = new EventCommentForm();
    form.setComment("");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrors(bindingResult)).containsKey("comment");
  }
}

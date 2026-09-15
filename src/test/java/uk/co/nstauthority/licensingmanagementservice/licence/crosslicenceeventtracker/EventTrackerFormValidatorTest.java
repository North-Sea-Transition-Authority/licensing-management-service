package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class EventTrackerFormValidatorTest {

  @InjectMocks
  private EventTrackerFormValidator validator;

  @Test
  void isValid_whenDatesBlank_thenHasNoErrors() {
    var form = new EventTrackerForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult);

    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenDatesValidAndInOrder_thenHasNoErrors() {
    var form = new EventTrackerForm();
    form.setFromDate("01/01/2030");
    form.setToDate("31/12/2030");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult);

    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenFromDateNotARealDate_thenRejectsFromDate() {
    var form = new EventTrackerForm();
    form.setFromDate("31/02/2030");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult);

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "fromDate", "fromDate.invalid", "Event from must be a real date in the format dd/mm/yyyy");
  }

  @Test
  void isValid_whenToDateNotParseable_thenRejectsToDate() {
    var form = new EventTrackerForm();
    form.setToDate("not-a-date");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult);

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "toDate", "toDate.invalid", "Event to must be a real date in the format dd/mm/yyyy");
  }

  @Test
  void isValid_whenFromDateAfterToDate_thenRejectsFromDate() {
    var form = new EventTrackerForm();
    form.setFromDate("31/12/2030");
    form.setToDate("01/01/2030");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    validator.isValid(form, bindingResult);

    ValidatorTestingUtil.assertErrorExists(
        bindingResult, "fromDate", "fromDate.afterToDate", "Event from date must not be after the event to date");
  }
}

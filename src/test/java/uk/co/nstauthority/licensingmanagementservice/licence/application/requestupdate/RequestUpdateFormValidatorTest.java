package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class RequestUpdateFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.MARCH, 15);

  @Mock
  private Clock clock;

  @InjectMocks
  private RequestUpdateFormValidator validator;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  private RequestUpdateForm buildValidForm() {
    var form = new RequestUpdateForm();
    form.getUpdateDeadlineDate().setDate(TODAY.plusDays(1));
    form.setUpdateReason("Further information is needed");
    return form;
  }

  @Test
  void isValid_whenValidForm_returnsTrue() {
    var form = buildValidForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenDateEmpty_returnsFalse() {
    var form = buildValidForm();
    form.setUpdateDeadlineDate(new ThreeFieldDateInput("updateDeadlineDate", "update deadline date"));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("updateDeadlineDate.dayInput.inputValue")).isTrue();
  }

  @Test
  void isValid_whenDateIsToday_returnsFalse() {
    var form = buildValidForm();
    form.getUpdateDeadlineDate().setDate(TODAY);
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("updateDeadlineDate.dayInput.inputValue")).isTrue();
  }

  @Test
  void isValid_whenReasonBlank_returnsFalse() {
    var form = buildValidForm();
    form.setUpdateReason("  ");
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
    assertThat(bindingResult.hasFieldErrors("updateReason")).isTrue();
  }
}

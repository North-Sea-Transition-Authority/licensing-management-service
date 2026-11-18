package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermFormValidatorTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  @InjectMocks
  private LicenceScheduleTermFormValidator licenceScheduleTermFormValidator;

  @Test
  void isValid_historicTermTypeTheScheduleCannotHave_isInvalid() {
    givenTheProductionTermsAreSelectable();
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.FOURTH);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult))
        .containsEntry("termType", Set.of("Select a term type"));
  }

  @Test
  void isValid_historicTermTypeTheActiveVersionHas_isValid() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    when(licenceScheduleTermService.getSelectableTermTypes(licenceScheduleDetail))
        .thenReturn(Set.of(TermType.INITIAL, TermType.SECOND, TermType.THIRD, TermType.FOURTH));

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.FOURTH);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid() {
    givenTheProductionTermsAreSelectable();
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isTrue();
  }

  @Test
  void isValid_invalid_termTypeNotSelected() {
    var form = new LicenceScheduleTermForm();
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalid_termTypeAlreadyExists() {
    givenTheProductionTermsAreSelectable();
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalid_invalidDuration() {
    givenTheProductionTermsAreSelectable();
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(new ThreeFieldDurationInput("termDuration", "term"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValidUpdate() {
    givenTheProductionTermsAreSelectable();
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceScheduleTerm)).isTrue();
  }

  @Test
  void isValidUpdate_invalid_termShorteningCausesValidationError() {
    givenTheProductionTermsAreSelectable();
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(licenceScheduleTerm));

    doAnswer(invocation -> {
      Errors errorsArg = invocation.getArgument(2);
      errorsArg.rejectValue(
          "termDuration.years",
          ".invalid",
          "The term duration cannot be reduced as this would cause events to occur after the end of the final term"
      );
      return null;
    }).when(scheduleRelativeDateValidationService).validateTermLengthUpdate(any(), any(), any());

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceScheduleTerm)).isFalse();
  }

  @Test
  void isValidUpdate_invalid_termTypeAlreadyExists() {
    givenTheProductionTermsAreSelectable();
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    var licenceScheduleTerm2 = new LicenceScheduleTerm();
    licenceScheduleTerm2.setId(UUID.randomUUID());
    licenceScheduleTerm2.setTermType(TermType.SECOND);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm, licenceScheduleTerm2));

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.SECOND);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceScheduleTerm)).isFalse();
  }

  private void givenTheProductionTermsAreSelectable() {
    when(licenceScheduleTermService.getSelectableTermTypes(any()))
        .thenReturn(Set.of(TermType.INITIAL, TermType.SECOND, TermType.THIRD));
  }

  private ThreeFieldDurationInput getValidDuration() {
    var durationInput = new ThreeFieldDurationInput("termDuration", "term");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");

    return durationInput;
  }

}
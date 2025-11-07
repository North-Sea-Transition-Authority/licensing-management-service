package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityFormValidatorTest {

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @InjectMocks
  private WorkProgrammeActivityFormValidator validator;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
  }

  @Test
  void isValid_fixedDate() {
    var form = createValidForm();

    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.FIXED_DATE);
    form.getDueDateInput().setDate(LocalDate.of(2025, 1, 1));

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2024, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid_withinATerm() {
    var form = createValidForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isTrue();
  }

  @Test
  void isValid_withinAPhase() {
    var form = createValidForm();

    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);
    form.setLicenceSchedulePhaseId("licenceSchedulePhaseId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isTrue();
  }

  @Test
  void isValid_invalidForm_missingCategory() {
    var form = createValidForm();
    form.setWorkProgrammeActivityCategory(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalidForm_missingOtherCategoryName() {
    var form = createValidForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.OTHER_ACTIVITY);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDescription() {
    var form = createValidForm();
    form.setDescription(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalidForm_missingCommitment() {
    var form = createValidForm();
    form.setWorkProgrammeActivityCommitment(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDateOption() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalidForm_fixedDateOption_missingDueDate() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.FIXED_DATE);

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2024, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult,licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_fixedDateOption_dueDateBeforeLicenceStartDate() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.FIXED_DATE);
    form.getDueDateInput().setDate(LocalDate.of(2020, 1, 1));

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2024, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_termOption_missingTermId() {
    var form = createValidForm();
    form.setLicenceScheduleTermId(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalidForm_phaseOption_missingPhaseId() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  private WorkProgrammeActivityForm createValidForm() {
    WorkProgrammeActivityForm form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM);
    form.setLicenceScheduleTermId("licenceScheduleTermId");
    return form;
  }

}
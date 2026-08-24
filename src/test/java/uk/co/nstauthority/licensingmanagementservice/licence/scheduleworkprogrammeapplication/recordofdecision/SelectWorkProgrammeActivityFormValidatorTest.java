package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@ExtendWith(MockitoExtension.class)
class SelectWorkProgrammeActivityFormValidatorTest {

  private static final String ACTIVITY_ID = UUID.randomUUID().toString();

  @Mock
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  @InjectMocks
  private SelectWorkProgrammeActivityFormValidator selectWorkProgrammeActivityFormValidator;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void isValid_whenActivitySelected_assertNoErrors() {
    mockSelectableActivities();
    var form = new SelectWorkProgrammeActivityForm();
    form.setWorkProgrammeActivityId(ACTIVITY_ID);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = selectWorkProgrammeActivityFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenNothingSelected_assertError() {
    mockSelectableActivities();
    var form = new SelectWorkProgrammeActivityForm();
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = selectWorkProgrammeActivityFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "workProgrammeActivityId",
            SelectWorkProgrammeActivityFormValidator.REQUIRED_ERROR_MESSAGE));
  }

  @Test
  void isValid_whenSelectedActivityIsNotOffered_assertError() {
    mockSelectableActivities();
    var unknownActivityId = UUID.randomUUID().toString();
    var form = new SelectWorkProgrammeActivityForm();
    form.setWorkProgrammeActivityId(unknownActivityId);
    when(recordWorkProgrammeAmendmentDetailsService
        .isActivityAlreadyDecided(applicationDetail, unknownActivityId))
        .thenReturn(false);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = selectWorkProgrammeActivityFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "workProgrammeActivityId",
            SelectWorkProgrammeActivityFormValidator.REQUIRED_ERROR_MESSAGE));
  }

  @Test
  void isValid_whenSelectedActivityAlreadyHasADecision_assertAlreadyDecidedError() {
    mockSelectableActivities();
    var decidedActivityId = UUID.randomUUID().toString();
    var form = new SelectWorkProgrammeActivityForm();
    form.setWorkProgrammeActivityId(decidedActivityId);
    when(recordWorkProgrammeAmendmentDetailsService
        .isActivityAlreadyDecided(applicationDetail, decidedActivityId))
        .thenReturn(true);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = selectWorkProgrammeActivityFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "workProgrammeActivityId",
            SelectWorkProgrammeActivityFormValidator.ALREADY_DECIDED_ERROR_MESSAGE));
  }

  private void mockSelectableActivities() {
    when(recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail))
        .thenReturn(List.of(new WorkProgrammeActivityView(
            ACTIVITY_ID,
            "27 July 2026",
            WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
            "Drill well to 3,000m",
            "%s due by 27 July 2026".formatted(WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName()),
            WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
            WorkProgrammeStatus.OPEN)));
  }
}

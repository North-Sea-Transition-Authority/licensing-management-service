package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityFormServiceTest {

  @Mock
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @Mock
  private LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @InjectMocks
  private WorkProgrammeActivityFormService workProgrammeActivityFormService;

  @Captor
  private ArgumentCaptor<WorkProgrammeActivity> workProgrammeActivityArgumentCaptor;

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
  void getDateOptions() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);


    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(
        Map.of(phase.getId().toString(),
        phase.getPhaseType().getDisplayName())
    );

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);

    assertThat(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityDateOption.class));
  }

  @Test
  void getDateOptions_licenceTypeDoesntHavePhases() {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));
    options.remove(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(false);

    assertThat(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(options));
  }

  @Test
  void getDateOptions_licenceDoesntHavePhases() {
    var options = new ArrayList<>(Arrays.asList(WorkProgrammeActivityDateOption.values()));
    options.remove(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    when(licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail)).thenReturn(Map.of());
    when(licenceTypeRulesResolver.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);

    assertThat(workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .isEqualTo(DisplayableEnumOptionUtil.getDisplayableOptions(options));
  }

  @Test
  void saveActivityFromForm_relativeDate_relatedToTerm() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);

    var termId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail, new WorkProgrammeActivity());

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getRelativeDuration,
            WorkProgrammeActivity::getStatus
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            null,
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            term,
            null,
            testDuration,
            LicenceScheduleEventStatus.ACTIVE
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveActivityFromForm_relativeDate_relatedToTerm_existingActivity_doesntOverwriteEventReference() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);

    var termId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();
    term.setId(termId);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    var activity = new WorkProgrammeActivity();
    activity.setEventReference(UUID.randomUUID());

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail, activity);

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getRelativeDuration,
            WorkProgrammeActivity::getStatus,
            WorkProgrammeActivity::getEventReference
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            null,
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            term,
            null,
            testDuration,
            LicenceScheduleEventStatus.ACTIVE,
            activity.getEventReference()
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveActivityFromForm_relativeDate_relatedToPhase() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);

    var phaseId = UUID.randomUUID();

    form.setRelativeEventId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);
    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    var testDuration = new ThreeFieldDuration(1,0,0);

    form.getRelativeDuration().setFromThreeFieldDuration(testDuration);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail, new WorkProgrammeActivity());

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getRelativeDuration,
            WorkProgrammeActivity::getStatus
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            null,
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            null,
            phase,
            testDuration,
            LicenceScheduleEventStatus.ACTIVE
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveActivityFromForm_termOption() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM);

    var termId = UUID.randomUUID();

    form.setLicenceScheduleTermId(String.valueOf(termId));

    var term = new LicenceScheduleTerm();

    when(licenceScheduleTermService.getTermByIdOrThrow(termId)).thenReturn(term);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail, new WorkProgrammeActivity());

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getStatus
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            form.getOtherCategoryName(),
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            term,
            null,
            LicenceScheduleEventStatus.ACTIVE
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveActivityFromForm_phaseOption() {
    var form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("otherCategoryName");
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    var phaseId = UUID.randomUUID();

    form.setLicenceSchedulePhaseId(String.valueOf(phaseId));

    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseService.getPhaseByIdOrThrow(phaseId)).thenReturn(phase);

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail, new WorkProgrammeActivity());

    verify(workProgrammeActivityRepository).save(workProgrammeActivityArgumentCaptor.capture());

    assertThat(workProgrammeActivityArgumentCaptor.getValue())
        .extracting(
            WorkProgrammeActivity::getLicenceScheduleDetail,
            WorkProgrammeActivity::getCategory,
            WorkProgrammeActivity::getOtherCategoryName,
            WorkProgrammeActivity::getDescription,
            WorkProgrammeActivity::getCommitment,
            WorkProgrammeActivity::getDateOption,
            WorkProgrammeActivity::getDueDate,
            WorkProgrammeActivity::getLicenceScheduleTerm,
            WorkProgrammeActivity::getLicenceSchedulePhase,
            WorkProgrammeActivity::getStatus
        )
        .containsExactly(
            licenceScheduleDetail,
            form.getWorkProgrammeActivityCategory(),
            form.getOtherCategoryName(),
            form.getDescription(),
            form.getWorkProgrammeActivityCommitment(),
            form.getWorkProgrammeActivityDateOption(),
            null,
            null,
            phase,
            LicenceScheduleEventStatus.ACTIVE
        );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void getActivityForm_termOption() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());

    var workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    workProgrammeActivity.setOtherCategoryName("otherCategoryName");
    workProgrammeActivity.setDescription("description");
    workProgrammeActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    workProgrammeActivity.setDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM);
    workProgrammeActivity.setLicenceScheduleTerm(term);
    workProgrammeActivity.setComments("comments");

    assertThat(workProgrammeActivityFormService.getActivityForm(workProgrammeActivity))
        .extracting(
            WorkProgrammeActivityForm::getWorkProgrammeActivityCategory,
            WorkProgrammeActivityForm::getOtherCategoryName,
            WorkProgrammeActivityForm::getDescription,
            WorkProgrammeActivityForm::getWorkProgrammeActivityCommitment,
            WorkProgrammeActivityForm::getWorkProgrammeActivityDateOption,
            WorkProgrammeActivityForm::getLicenceScheduleTermId,
            WorkProgrammeActivityForm::getLicenceSchedulePhaseId,
            WorkProgrammeActivityForm::getRelativeEventId,
            WorkProgrammeActivityForm::getComments
        )
        .containsExactly(
            workProgrammeActivity.getCategory(),
            workProgrammeActivity.getOtherCategoryName(),
            workProgrammeActivity.getDescription(),
            workProgrammeActivity.getCommitment(),
            workProgrammeActivity.getDateOption(),
            String.valueOf(workProgrammeActivity.getLicenceScheduleTerm().getId()),
            null,
            null,
            workProgrammeActivity.getComments()
    );
  }

  @Test
  void getActivityForm_phaseOption() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    workProgrammeActivity.setOtherCategoryName("otherCategoryName");
    workProgrammeActivity.setDescription("description");
    workProgrammeActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    workProgrammeActivity.setDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);
    workProgrammeActivity.setLicenceSchedulePhase(phase);
    workProgrammeActivity.setComments("comments");

    assertThat(workProgrammeActivityFormService.getActivityForm(workProgrammeActivity))
        .extracting(
            WorkProgrammeActivityForm::getWorkProgrammeActivityCategory,
            WorkProgrammeActivityForm::getOtherCategoryName,
            WorkProgrammeActivityForm::getDescription,
            WorkProgrammeActivityForm::getWorkProgrammeActivityCommitment,
            WorkProgrammeActivityForm::getWorkProgrammeActivityDateOption,
            WorkProgrammeActivityForm::getLicenceScheduleTermId,
            WorkProgrammeActivityForm::getLicenceSchedulePhaseId,
            WorkProgrammeActivityForm::getRelativeEventId,
            WorkProgrammeActivityForm::getComments
        )
        .containsExactly(
            workProgrammeActivity.getCategory(),
            workProgrammeActivity.getOtherCategoryName(),
            workProgrammeActivity.getDescription(),
            workProgrammeActivity.getCommitment(),
            workProgrammeActivity.getDateOption(),
            null,
            String.valueOf(workProgrammeActivity.getLicenceSchedulePhase().getId()),
            null,
            workProgrammeActivity.getComments()
        );
  }

  @Test
  void getActivityForm_relativeOption() {
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());

    var workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    workProgrammeActivity.setOtherCategoryName("otherCategoryName");
    workProgrammeActivity.setDescription("description");
    workProgrammeActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    workProgrammeActivity.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    workProgrammeActivity.setRelativeDuration(new ThreeFieldDuration(1, 2, 3));
    workProgrammeActivity.setLicenceSchedulePhase(phase);
    workProgrammeActivity.setComments("comments");

    var result = workProgrammeActivityFormService.getActivityForm(workProgrammeActivity);

    assertThat(result)
        .extracting(
            WorkProgrammeActivityForm::getWorkProgrammeActivityCategory,
            WorkProgrammeActivityForm::getOtherCategoryName,
            WorkProgrammeActivityForm::getDescription,
            WorkProgrammeActivityForm::getWorkProgrammeActivityCommitment,
            WorkProgrammeActivityForm::getWorkProgrammeActivityDateOption,
            WorkProgrammeActivityForm::getLicenceScheduleTermId,
            WorkProgrammeActivityForm::getLicenceSchedulePhaseId,
            WorkProgrammeActivityForm::getRelativeEventId,
            WorkProgrammeActivityForm::getComments
        )
        .containsExactly(
            workProgrammeActivity.getCategory(),
            workProgrammeActivity.getOtherCategoryName(),
            workProgrammeActivity.getDescription(),
            workProgrammeActivity.getCommitment(),
            workProgrammeActivity.getDateOption(),
            null,
            null,
            String.valueOf(workProgrammeActivity.getLicenceSchedulePhase().getId()),
            workProgrammeActivity.getComments()
        );

    var duration = result.getRelativeDuration().toThreeFieldDuration();

    assertThat(duration.days()).isEqualTo(workProgrammeActivity.getRelativeDuration().days());
    assertThat(duration.months()).isEqualTo(workProgrammeActivity.getRelativeDuration().months());
    assertThat(duration.years()).isEqualTo(workProgrammeActivity.getRelativeDuration().years());
  }
}
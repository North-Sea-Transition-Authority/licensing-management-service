package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceJson;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@ExtendWith(MockitoExtension.class)
class RecordWorkProgrammeAmendmentDetailsServiceTest {

  @Mock
  private RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository;

  @Mock
  private RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceScheduleDetail licenceScheduleDetail;

  @InjectMocks
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;
  private WorkProgrammeActivity workProgrammeActivity;
  private WorkProgrammeActivityView activityView;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    workProgrammeActivity = buildActivity(UUID.randomUUID(), "Drill well to 3,000m");
    activityView = buildActivityView(workProgrammeActivity.getId().toString(), "Drill well to 3,000m");
  }

  @Test
  void hasAmendmentDetails_whenDecisionRecorded_returnsTrue() {
    when(recordOfDecisionWorkProgrammeRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(true);

    assertThat(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).isTrue();
  }

  @Test
  void hasAmendmentDetails_whenNoDecisionRecorded_returnsFalse() {
    when(recordOfDecisionWorkProgrammeRepository.existsByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(false);

    assertThat(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).isFalse();
  }

  @Test
  void isActivityAlreadyDecided_whenDecisionRecordedForThatActivity_returnsTrue() {
    var activityId = UUID.randomUUID();
    when(recordOfDecisionWorkProgrammeRepository
        .existsByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivityId(applicationDetail, activityId))
        .thenReturn(true);

    assertThat(recordWorkProgrammeAmendmentDetailsService
        .isActivityAlreadyDecided(applicationDetail, activityId.toString())).isTrue();
  }

  @Test
  void isActivityAlreadyDecided_whenNoDecisionRecordedForThatActivity_returnsFalse() {
    var activityId = UUID.randomUUID();
    when(recordOfDecisionWorkProgrammeRepository
        .existsByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivityId(applicationDetail, activityId))
        .thenReturn(false);

    assertThat(recordWorkProgrammeAmendmentDetailsService
        .isActivityAlreadyDecided(applicationDetail, activityId.toString())).isFalse();
  }

  @Test
  void isActivityAlreadyDecided_whenIdIsNull_returnsFalse() {
    assertThat(recordWorkProgrammeAmendmentDetailsService
        .isActivityAlreadyDecided(applicationDetail, null)).isFalse();
  }

  @Test
  void isActivityAlreadyDecided_whenIdIsNotAUuid_returnsFalse() {
    assertThat(recordWorkProgrammeAmendmentDetailsService
        .isActivityAlreadyDecided(applicationDetail, "not-a-uuid")).isFalse();
  }

  @Test
  void getSelectableActivityViews_whenNoDecisionsRecorded_returnsAllActivitiesOnTheSchedule() {
    var otherActivityView = buildActivityView(UUID.randomUUID().toString(), "Acquire 3D seismic data");
    mockRecordedWorkProgrammes();
    mockScheduleActivityViews(activityView, otherActivityView);

    var views = recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail);

    assertThat(views)
        .extracting(WorkProgrammeActivityView::id, WorkProgrammeActivityView::description)
        .containsExactly(
            tuple(activityView.id(), "Drill well to 3,000m"),
            tuple(otherActivityView.id(), "Acquire 3D seismic data"));
  }

  @Test
  void getSelectableActivityViews_whenActivityAlreadyDecided_excludesThatActivity() {
    var otherActivityView = buildActivityView(UUID.randomUUID().toString(), "Acquire 3D seismic data");
    mockRecordedWorkProgrammes(workProgrammeWithActivity(workProgrammeActivity));
    mockScheduleActivityViews(activityView, otherActivityView);

    var views = recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail);

    assertThat(views)
        .extracting(WorkProgrammeActivityView::id)
        .containsExactly(otherActivityView.id());
  }

  @Test
  void getSelectableActivityViews_whenScheduleCorrectedAfterSubmission_readsCurrentScheduleNotSubmittedOne() {
    var correctedActivityView = buildActivityView(UUID.randomUUID().toString(), "Drill well to 3,500m");
    mockRecordedWorkProgrammes();
    when(scheduleWorkProgrammeApplicationService.getCurrentScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
    when(workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(licenceScheduleDetail))
        .thenReturn(List.of(correctedActivityView));

    var views = recordWorkProgrammeAmendmentDetailsService.getSelectableActivityViews(applicationDetail);

    assertThat(views)
        .extracting(WorkProgrammeActivityView::id, WorkProgrammeActivityView::description)
        .containsExactly(tuple(correctedActivityView.id(), "Drill well to 3,500m"));
    verify(scheduleWorkProgrammeApplicationService, never()).getScheduleDetailFromApplicationDetail(applicationDetail);
  }

  @Test
  void getFilledForm_whenNoDecisionRecorded_returnsEmptyForm() {
    mockNoExistingWorkProgramme();

    var form = recordWorkProgrammeAmendmentDetailsService
        .getFilledForm(applicationDetail, workProgrammeActivity);

    assertThat(form.getDecision()).isNull();
    assertThat(form.getAmendDuration()).isNull();
    assertThat(form.getAmendText()).isNull();
    assertThat(form.getAmendedText()).isNull();
    assertThat(form.getTargetLicenceIds()).isEmpty();
  }

  @Test
  void getFilledForm_whenAmendmentRecorded_returnsFilledForm() {
    var workProgramme = workProgrammeWithActivity(workProgrammeActivity);
    workProgramme.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    workProgramme.setAmendDuration(true);
    workProgramme.setAmendText(true);
    workProgramme.setAmendedDuration(new ThreeFieldDuration(1, 6, 0));
    workProgramme.setAmendedText("The licensee shall drill to 3,500m");
    mockExistingWorkProgramme(workProgramme);
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(workProgramme))
        .thenReturn(List.of());

    var form = recordWorkProgrammeAmendmentDetailsService
        .getFilledForm(applicationDetail, workProgrammeActivity);

    assertThat(form.getDecision()).isEqualTo(WorkProgrammeAmendmentDecision.AMEND);
    assertThat(form.getAmendDuration()).isTrue();
    assertThat(form.getAmendText()).isTrue();
    assertThat(form.getAmendedText()).isEqualTo("The licensee shall drill to 3,500m");
    assertThat(form.getAmendedDuration().getYears()).isEqualTo("1");
    assertThat(form.getAmendedDuration().getMonths()).isEqualTo("6");
    assertThat(form.getAmendedDuration().getDays()).isEqualTo("0");
  }

  @Test
  void getFilledForm_whenCompletedOnAnotherLicence_returnsTargetLicenceIds() {
    var workProgramme = workProgrammeWithActivity(workProgrammeActivity);
    workProgramme.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    mockExistingWorkProgramme(workProgramme);
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(workProgramme))
        .thenReturn(List.of(workProgrammeLicence(workProgramme, buildLicence(21, "P123"))));

    var form = recordWorkProgrammeAmendmentDetailsService
        .getFilledForm(applicationDetail, workProgrammeActivity);

    assertThat(form.getTargetLicenceIds()).containsExactly("21");
  }

  @Test
  void getTargetLicenceSelections_whenNoIds_returnsEmptyList() {
    assertThat(recordWorkProgrammeAmendmentDetailsService.getTargetLicenceSelections(List.of())).isEmpty();
  }

  @Test
  void getTargetLicenceSelections_whenIdsGiven_returnsLicenceSelections() {
    var licence = buildLicence(21, "P123");
    when(licenceService.getLicencesByIds(List.of(21))).thenReturn(List.of(licence));

    var selections = recordWorkProgrammeAmendmentDetailsService.getTargetLicenceSelections(List.of("21"));

    assertThat(selections)
        .extracting(LicenceJson::licenceId, LicenceJson::licenceReference)
        .containsExactly(tuple(21, "P123"));
  }

  @Test
  void saveAmendmentDetails_whenAmendDurationAndText_savesBothAnswers() {
    mockNoExistingWorkProgramme();
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendDuration(true);
    form.setAmendText(true);
    form.getAmendedDuration().setYears("1");
    form.getAmendedDuration().setMonths("6");
    form.getAmendedDuration().setDays("0");
    form.setAmendedText("The licensee shall drill to 3,500m");

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionWorkProgramme.class);
    verify(recordOfDecisionWorkProgrammeRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getScheduleWorkProgrammeApplicationDetail()).isEqualTo(applicationDetail);
    assertThat(saved.getWorkProgrammeActivity()).isEqualTo(workProgrammeActivity);
    assertThat(saved.getDecision()).isEqualTo(WorkProgrammeAmendmentDecision.AMEND);
    assertThat(saved.getAmendDuration()).isTrue();
    assertThat(saved.getAmendText()).isTrue();
    assertThat(saved.getAmendedDuration()).isEqualTo(new ThreeFieldDuration(1, 6, 0));
    assertThat(saved.getAmendedText()).isEqualTo("The licensee shall drill to 3,500m");
  }

  @Test
  void saveAmendmentDetails_whenAmendDurationOnly_doesNotSaveText() {
    mockNoExistingWorkProgramme();
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendDuration(true);
    form.getAmendedDuration().setYears("0");
    form.getAmendedDuration().setMonths("3");
    form.getAmendedDuration().setDays("0");
    form.setAmendedText("text the user typed then unticked");

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionWorkProgramme.class);
    verify(recordOfDecisionWorkProgrammeRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getAmendText()).isFalse();
    assertThat(saved.getAmendedText()).isNull();
    assertThat(saved.getAmendedDuration()).isEqualTo(new ThreeFieldDuration(0, 3, 0));
  }

  @Test
  void saveAmendmentDetails_whenWaived_clearsAmendmentAnswers() {
    mockNoExistingWorkProgramme();
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.WAIVE);
    form.setAmendDuration(true);
    form.setAmendText(true);
    form.setAmendedText("text from a previous answer");

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionWorkProgramme.class);
    verify(recordOfDecisionWorkProgrammeRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getDecision()).isEqualTo(WorkProgrammeAmendmentDecision.WAIVE);
    assertThat(saved.getAmendDuration()).isNull();
    assertThat(saved.getAmendText()).isNull();
    assertThat(saved.getAmendedDuration()).isNull();
    assertThat(saved.getAmendedText()).isNull();
  }

  @Test
  void saveAmendmentDetails_whenCompletedOnAnotherLicence_savesTargetLicences() {
    mockNoExistingWorkProgramme();
    var licence = buildLicence(21, "P123");
    when(licenceService.getLicencesByIds(List.of(21))).thenReturn(List.of(licence));

    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    form.setTargetLicenceIds(List.of("21"));

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    var captor = ArgumentCaptor.forClass(List.class);
    verify(recordOfDecisionWorkProgrammeLicenceRepository).saveAll(captor.capture());
    assertThat((List<RecordOfDecisionWorkProgrammeLicence>) captor.getValue())
        .extracting(RecordOfDecisionWorkProgrammeLicence::getLicence)
        .containsExactly(licence);
  }

  @Test
  void saveAmendmentDetails_whenNotCompletedOnAnotherLicence_doesNotSaveTargetLicences() {
    mockNoExistingWorkProgramme();
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.ACKNOWLEDGE);
    form.setTargetLicenceIds(List.of("21"));

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    verify(recordOfDecisionWorkProgrammeLicenceRepository).saveAll(List.of());
  }

  @Test
  void saveAmendmentDetails_whenDecisionAlreadyRecorded_updatesTheExistingRecord() {
    var existing = workProgrammeWithActivity(workProgrammeActivity);
    existing.setDecision(WorkProgrammeAmendmentDecision.WAIVE);
    mockExistingWorkProgramme(existing);

    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.ACKNOWLEDGE);

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    var captor = ArgumentCaptor.forClass(RecordOfDecisionWorkProgramme.class);
    verify(recordOfDecisionWorkProgrammeRepository).save(captor.capture());
    assertThat(captor.getValue()).isSameAs(existing);
    assertThat(captor.getValue().getDecision()).isEqualTo(WorkProgrammeAmendmentDecision.ACKNOWLEDGE);
  }

  @Test
  void saveAmendmentDetails_whenDecisionChangedAwayFromAnotherLicence_deletesTheSavedTargetLicences() {
    var existing = workProgrammeWithActivity(workProgrammeActivity);
    existing.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    mockExistingWorkProgramme(existing);
    var savedLicence = workProgrammeLicence(existing, buildLicence(21, "P123"));
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(existing))
        .thenReturn(List.of(savedLicence));

    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.ACKNOWLEDGE);

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    verify(recordOfDecisionWorkProgrammeLicenceRepository).delete(savedLicence);
    verify(recordOfDecisionWorkProgrammeLicenceRepository).saveAll(List.of());
  }

  @Test
  void saveAmendmentDetails_whenTargetLicenceRemoved_deletesOnlyThatLicence() {
    var existing = workProgrammeWithActivity(workProgrammeActivity);
    existing.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    mockExistingWorkProgramme(existing);

    var keptLicence = buildLicence(21, "P123");
    var removedWorkProgrammeLicence = workProgrammeLicence(existing, buildLicence(22, "P124"));
    var keptWorkProgrammeLicence = workProgrammeLicence(existing, keptLicence);
    when(recordOfDecisionWorkProgrammeLicenceRepository.findAllByRecordOfDecisionWorkProgramme(existing))
        .thenReturn(List.of(keptWorkProgrammeLicence, removedWorkProgrammeLicence));
    when(licenceService.getLicencesByIds(List.of(21))).thenReturn(List.of(keptLicence));

    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    form.setTargetLicenceIds(List.of("21"));

    recordWorkProgrammeAmendmentDetailsService
        .saveAmendmentDetails(form, applicationDetail, workProgrammeActivity);

    verify(recordOfDecisionWorkProgrammeLicenceRepository).delete(removedWorkProgrammeLicence);
    verify(recordOfDecisionWorkProgrammeLicenceRepository, never()).delete(keptWorkProgrammeLicence);
    verify(recordOfDecisionWorkProgrammeLicenceRepository).saveAll(List.of());
  }

  private void mockScheduleActivityViews(WorkProgrammeActivityView... views) {
    when(scheduleWorkProgrammeApplicationService.getCurrentScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(licenceScheduleDetail);
    when(workProgrammeActivityService.getLicenceWorkProgramActivitiesViews(licenceScheduleDetail))
        .thenReturn(List.of(views));
  }

  private void mockRecordedWorkProgrammes(RecordOfDecisionWorkProgramme... workProgrammes) {
    when(recordOfDecisionWorkProgrammeRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .thenReturn(List.of(workProgrammes));
  }

  private void mockExistingWorkProgramme(RecordOfDecisionWorkProgramme workProgramme) {
    when(recordOfDecisionWorkProgrammeRepository
        .findByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivity(
            applicationDetail, workProgrammeActivity))
        .thenReturn(Optional.of(workProgramme));
  }

  private void mockNoExistingWorkProgramme() {
    when(recordOfDecisionWorkProgrammeRepository
        .findByScheduleWorkProgrammeApplicationDetailAndWorkProgrammeActivity(
            applicationDetail, workProgrammeActivity))
        .thenReturn(Optional.empty());
  }

  private RecordOfDecisionWorkProgramme workProgrammeWithActivity(WorkProgrammeActivity activity) {
    var workProgramme = new RecordOfDecisionWorkProgramme();
    workProgramme.setId(UUID.randomUUID());
    workProgramme.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
    workProgramme.setWorkProgrammeActivity(activity);
    return workProgramme;
  }

  private RecordOfDecisionWorkProgrammeLicence workProgrammeLicence(
      RecordOfDecisionWorkProgramme workProgramme,
      Licence licence
  ) {
    var workProgrammeLicence = new RecordOfDecisionWorkProgrammeLicence();
    workProgrammeLicence.setId(UUID.randomUUID());
    workProgrammeLicence.setRecordOfDecisionWorkProgramme(workProgramme);
    workProgrammeLicence.setLicence(licence);
    return workProgrammeLicence;
  }

  private WorkProgrammeActivityView buildActivityView(String id, String description) {
    return new WorkProgrammeActivityView(
        id,
        "27 July 2026",
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        description,
        "%s due by 27 July 2026".formatted(WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName()),
        WorkProgrammeActivityCommitment.FIRM.getDisplayName(),
        WorkProgrammeStatus.OPEN);
  }

  private WorkProgrammeActivity buildActivity(UUID id, String description) {
    var activity = new WorkProgrammeActivity();
    activity.setId(id);
    activity.setDescription(description);
    activity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    activity.setDueDate(LocalDate.of(2026, 7, 27));
    return activity;
  }

  private Licence buildLicence(Integer id, String licenceReference) {
    return LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference(licenceReference)
        .build();
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class RecordWorkProgrammeAmendmentDetailsServiceIntegrationTest {

  @Autowired
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  @Autowired
  private RecordOfDecisionService recordOfDecisionService;

  @Autowired
  private RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository;

  @Autowired
  private RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository;

  @Autowired
  private EntityManager em;

  @Test
  void removeAmendmentDetails_whenCompletedOnAnotherLicence_deletesTheDecisionAndItsLicences() {
    var licence = persistLicence(1, "P001");
    var targetLicence = persistLicence(2, "P002");
    var licenceScheduleDetail = persistLicenceScheduleDetail(licence);
    var applicationDetail = persistApplicationDetail(licenceScheduleDetail);
    var removedActivity = persistActivity(licenceScheduleDetail, "Drill well to 3,000m");
    var keptActivity = persistActivity(licenceScheduleDetail, "Acquire 3D seismic data");

    var completeOnAnotherLicenceForm = new RecordWorkProgrammeAmendmentDetailsForm();
    completeOnAnotherLicenceForm.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    completeOnAnotherLicenceForm.setTargetLicenceIds(List.of(targetLicence.getId().toString()));
    recordWorkProgrammeAmendmentDetailsService.saveAmendmentDetails(
        completeOnAnotherLicenceForm, applicationDetail, removedActivity);

    var waiveForm = new RecordWorkProgrammeAmendmentDetailsForm();
    waiveForm.setDecision(WorkProgrammeAmendmentDecision.WAIVE);
    recordWorkProgrammeAmendmentDetailsService.saveAmendmentDetails(waiveForm, applicationDetail, keptActivity);
    em.flush();
    em.clear();

    recordWorkProgrammeAmendmentDetailsService.removeAmendmentDetails(
        em.find(ScheduleWorkProgrammeApplicationDetail.class, applicationDetail.getId()),
        em.find(WorkProgrammeActivity.class, removedActivity.getId()));
    em.flush();
    em.clear();

    assertThat(recordOfDecisionWorkProgrammeRepository.findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail))
        .extracting(workProgramme -> workProgramme.getWorkProgrammeActivity().getId())
        .containsExactly(keptActivity.getId());
    assertThat(recordOfDecisionWorkProgrammeLicenceRepository.findAll()).isEmpty();
  }

  @Test
  void removeAmendmentDetails_whenLastDecisionRemoved_taskIsNoLongerComplete() {
    var licence = persistLicence(1, "P001");
    var licenceScheduleDetail = persistLicenceScheduleDetail(licence);
    var applicationDetail = persistApplicationDetail(licenceScheduleDetail);
    var activity = persistActivity(licenceScheduleDetail, "Drill well to 3,000m");

    var waiveForm = new RecordWorkProgrammeAmendmentDetailsForm();
    waiveForm.setDecision(WorkProgrammeAmendmentDecision.WAIVE);
    recordWorkProgrammeAmendmentDetailsService.saveAmendmentDetails(waiveForm, applicationDetail, activity);

    var summaryForm = new RecordWorkProgrammeAmendmentSummaryForm();
    summaryForm.setRecordWorkProgrammeAmendmentSummaryOptions(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
    recordOfDecisionService.saveWorkProgrammeSummaryOption(applicationDetail, summaryForm);
    em.flush();
    em.clear();

    recordWorkProgrammeAmendmentDetailsService.removeAmendmentDetails(
        em.find(ScheduleWorkProgrammeApplicationDetail.class, applicationDetail.getId()),
        em.find(WorkProgrammeActivity.class, activity.getId()));
    em.flush();
    em.clear();

    assertThat(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).isFalse();
    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentDetailsComplete(applicationDetail)).isFalse();
  }

  private Licence persistLicence(int id, String reference) {
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceReference(reference)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);
    return licence;
  }

  private LicenceScheduleDetail persistLicenceScheduleDetail(Licence licence) {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    em.persist(licenceSchedule);

    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();
    em.persist(licenceScheduleDetail);
    return licenceScheduleDetail;
  }

  private ScheduleWorkProgrammeApplicationDetail persistApplicationDetail(LicenceScheduleDetail licenceScheduleDetail) {
    var application = new ScheduleWorkProgrammeApplication();
    application.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    em.persist(application);

    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setScheduleWorkProgrammeApplication(application);
    applicationDetail.setVersionNumber(1);
    applicationDetail.setStatus(ApplicationStatus.DRAFT_DECISION);
    applicationDetail.setCreatedDatetime(Instant.now());
    em.persist(applicationDetail);
    return applicationDetail;
  }

  private WorkProgrammeActivity persistActivity(LicenceScheduleDetail licenceScheduleDetail, String description) {
    var activity = new WorkProgrammeActivity();
    activity.setLicenceScheduleDetail(licenceScheduleDetail);
    activity.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    activity.setDescription(description);
    activity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    activity.setDueDate(LocalDate.of(2026, Month.JULY, 27));
    em.persist(activity);
    return activity;
  }
}

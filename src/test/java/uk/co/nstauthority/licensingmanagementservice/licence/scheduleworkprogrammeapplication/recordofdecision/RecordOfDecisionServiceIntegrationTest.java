package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class RecordOfDecisionServiceIntegrationTest {

  @Autowired
  private RecordOfDecisionService recordOfDecisionService;

  @Autowired
  private EntityManager em;

  @Test
  void saveWorkProgrammeSummaryOption_persistsAndReadsBackTheOption() {
    var applicationDetail = persistApplicationDetail();
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    form.setRecordWorkProgrammeAmendmentSummaryOptions(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);

    recordOfDecisionService.saveWorkProgrammeSummaryOption(applicationDetail, form);
    em.flush();
    em.clear();

    assertThat(recordOfDecisionService.getFilledWorkProgrammeSummaryForm(applicationDetail))
        .extracting(RecordWorkProgrammeAmendmentSummaryForm::getRecordWorkProgrammeAmendmentSummaryOptions)
        .isEqualTo(RecordWorkProgrammeAmendmentSummaryOptions.NO_ALL_ADDED);
    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentDetailsComplete(applicationDetail)).isTrue();
  }

  @Test
  void saveWorkProgrammeSummaryOption_whenMoreToAdd_doesNotCompleteTheTask() {
    var applicationDetail = persistApplicationDetail();
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    form.setRecordWorkProgrammeAmendmentSummaryOptions(RecordWorkProgrammeAmendmentSummaryOptions.NO_LATER);

    recordOfDecisionService.saveWorkProgrammeSummaryOption(applicationDetail, form);
    em.flush();
    em.clear();

    assertThat(recordOfDecisionService.isWorkProgrammeAmendmentDetailsComplete(applicationDetail)).isFalse();
  }

  private ScheduleWorkProgrammeApplicationDetail persistApplicationDetail() {
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference("P001")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    em.persist(licenceSchedule);

    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();
    em.persist(licenceScheduleDetail);

    var application = new ScheduleWorkProgrammeApplication();
    application.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    em.persist(application);

    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setScheduleWorkProgrammeApplication(application);
    applicationDetail.setVersionNumber(1);
    applicationDetail.setStatus(ApplicationStatus.ISSUE_DECISION);
    applicationDetail.setCreatedDatetime(Instant.now());
    em.persist(applicationDetail);

    return applicationDetail;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceScheduleDetailDuplicationServiceIntegrationTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Autowired
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Autowired
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Autowired
  private WorkProgrammeActivityRepository workProgrammeActivityRepository;

  @Autowired
  private LicenceScheduleRateRepository licenceScheduleRateRepository;

  @Autowired
  private OtherScheduleEventRepository otherScheduleEventRepository;

  @Autowired
  private LicenceStartDateRepository licenceStartDateRepository;

  @Autowired
  private LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  @Autowired
  private LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService;

  private Licence licence;

  private LicenceSchedule licenceSchedule;

  private LicenceScheduleDetail oldLicenceScheduleDetail;

  private LicenceStartDate licenceStartDate;

  private LicenceScheduleExpiry licenceScheduleExpiry;

  private LicenceScheduleTerm licenceScheduleTerm;

  private LicenceSchedulePhase licenceSchedulePhase;

  private WorkProgrammeActivity termLinkedActivity;

  private WorkProgrammeActivity phaseLinkedActivity;

  private LicenceScheduleRate termLinkedRate;

  private LicenceScheduleRate phaseLinkedRate;

  private OtherScheduleEvent termLinkedEvent;

  private OtherScheduleEvent phaseLinkedEvent;

  @Test
  void createNewDraftLicenceScheduleDetailVersion() {
    createDbBaseline();

    licenceScheduleDetailDuplicationService.createNewDraftLicenceScheduleDetailVersion(oldLicenceScheduleDetail);

    em.flush();

    var newLicenceScheduleDetail = licenceScheduleDetailRepository.findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.DRAFT).get();

    assertThat(newLicenceScheduleDetail).extracting(
        LicenceScheduleDetail::getLicenceSchedule,
        LicenceScheduleDetail::getStatus
    ).containsExactly(
        licenceSchedule,
        LicenceScheduleDetailStatus.DRAFT
    );

    var newLicenceStartDate = licenceStartDateRepository.findByLicenceScheduleDetail(newLicenceScheduleDetail).get();

    assertThat(newLicenceStartDate).extracting(
        LicenceStartDate::getLicenceScheduleDetail,
        LicenceStartDate::getStartDate
    ).containsExactly(
        newLicenceScheduleDetail,
        licenceStartDate.getStartDate()
    );

    var newLicenceScheduleExpiry = licenceScheduleExpiryRepository.findByLicenceScheduleDetail(newLicenceScheduleDetail).get();

    assertThat(newLicenceScheduleExpiry).extracting(
        LicenceScheduleExpiry::getLicenceScheduleDetail,
        LicenceScheduleExpiry::getExpiryDate
    ).containsExactly(
        newLicenceScheduleDetail,
        licenceScheduleExpiry.getExpiryDate()
    );

    var newLicenceScheduleTerm = licenceScheduleTermRepository.findAllByLicenceScheduleDetail(newLicenceScheduleDetail).getFirst();

    assertThat(newLicenceScheduleTerm).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration
    ).containsExactly(
        newLicenceScheduleDetail,
        licenceScheduleTerm.getTermType(),
        licenceScheduleTerm.getTermDuration()
    );

    var newLicenceSchedulePhase = licenceSchedulePhaseRepository.findAllByLicenceScheduleDetail(newLicenceScheduleDetail).getFirst();

    assertThat(newLicenceSchedulePhase).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getLicenceScheduleTerm,
        LicenceSchedulePhase::getPhaseType,
        LicenceSchedulePhase::getPhaseDuration
    ).containsExactly(
        newLicenceScheduleDetail,
        newLicenceScheduleTerm,
        licenceSchedulePhase.getPhaseType(),
        licenceSchedulePhase.getPhaseDuration()
    );

    var newActivities = workProgrammeActivityRepository.findAllByLicenceScheduleDetail(newLicenceScheduleDetail);

    var expectedNewTermLinkedActivity = new WorkProgrammeActivity();
    expectedNewTermLinkedActivity.setLicenceScheduleDetail(newLicenceScheduleDetail);
    expectedNewTermLinkedActivity.setLicenceScheduleTerm(newLicenceScheduleTerm);
    expectedNewTermLinkedActivity.setDateOption(termLinkedActivity.getDateOption());
    expectedNewTermLinkedActivity.setRelativeDuration(termLinkedActivity.getRelativeDuration());
    expectedNewTermLinkedActivity.setLicenceSchedule(licenceSchedule);

    var expectedNewPhaseLinkedActivity = new WorkProgrammeActivity();
    expectedNewPhaseLinkedActivity.setLicenceScheduleDetail(newLicenceScheduleDetail);
    expectedNewPhaseLinkedActivity.setLicenceSchedulePhase(newLicenceSchedulePhase);
    expectedNewPhaseLinkedActivity.setDateOption(phaseLinkedActivity.getDateOption());
    expectedNewPhaseLinkedActivity.setRelativeDuration(phaseLinkedActivity.getRelativeDuration());
    expectedNewPhaseLinkedActivity.setLicenceSchedule(licenceSchedule);

    assertThat(newActivities)
        .usingRecursiveComparison()
        .ignoringFields("id", "dueDate", "originalEventId")
        .ignoringCollectionOrder()
        .isEqualTo(List.of(expectedNewTermLinkedActivity, expectedNewPhaseLinkedActivity));

    var newRates = licenceScheduleRateRepository.findAllByLicenceScheduleDetail(newLicenceScheduleDetail);

    var expectedNewTermLinkedRate = new LicenceScheduleRate();
    expectedNewTermLinkedRate.setLicenceScheduleDetail(newLicenceScheduleDetail);
    expectedNewTermLinkedRate.setLicenceScheduleTerm(newLicenceScheduleTerm);
    expectedNewTermLinkedRate.setRateDefinitionOption(termLinkedRate.getRateDefinitionOption());
    expectedNewTermLinkedRate.setLicenceSchedule(licenceSchedule);

    var expectedNewPhaseLinkedRate = new LicenceScheduleRate();
    expectedNewPhaseLinkedRate.setLicenceScheduleDetail(newLicenceScheduleDetail);
    expectedNewPhaseLinkedRate.setLicenceSchedulePhase(newLicenceSchedulePhase);
    expectedNewPhaseLinkedRate.setRateDefinitionOption(phaseLinkedRate.getRateDefinitionOption());
    expectedNewPhaseLinkedRate.setRateRelativeDateOption(phaseLinkedRate.getRateRelativeDateOption());
    expectedNewPhaseLinkedRate.setLicenceSchedule(licenceSchedule);

    assertThat(newRates)
        .usingRecursiveComparison()
        .ignoringFields("id", "startDate", "originalEventId")
        .ignoringCollectionOrder()
        .isEqualTo(List.of(expectedNewTermLinkedRate, expectedNewPhaseLinkedRate));

    var newEvents = otherScheduleEventRepository.findAllByLicenceScheduleDetail(newLicenceScheduleDetail);

    var expectedNewTermLinkedEvent = new OtherScheduleEvent();
    expectedNewTermLinkedEvent.setLicenceScheduleDetail(newLicenceScheduleDetail);
    expectedNewTermLinkedEvent.setLicenceScheduleTerm(newLicenceScheduleTerm);
    expectedNewTermLinkedEvent.setDateOption(termLinkedEvent.getDateOption());
    expectedNewTermLinkedEvent.setRelativeDuration(termLinkedEvent.getRelativeDuration());
    expectedNewTermLinkedEvent.setLicenceSchedule(licenceSchedule);

    var expectedNewPhaseLinkedEvent = new OtherScheduleEvent();
    expectedNewPhaseLinkedEvent.setLicenceScheduleDetail(newLicenceScheduleDetail);
    expectedNewPhaseLinkedEvent.setLicenceSchedulePhase(newLicenceSchedulePhase);
    expectedNewPhaseLinkedEvent.setDateOption(phaseLinkedEvent.getDateOption());
    expectedNewPhaseLinkedEvent.setRelativeDuration(phaseLinkedEvent.getRelativeDuration());
    expectedNewPhaseLinkedEvent.setLicenceSchedule(licenceSchedule);

    assertThat(newEvents)
        .usingRecursiveComparison()
        .ignoringFields("id", "eventDate", "originalEventId")
        .ignoringCollectionOrder()
        .isEqualTo(List.of(expectedNewTermLinkedEvent, expectedNewPhaseLinkedEvent));
  }

  private void createDbBaseline() {
    licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    em.persist(licence);

    licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    em.persist(licenceSchedule);

    oldLicenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();

    em.persist(oldLicenceScheduleDetail);

    licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    em.persist(licenceStartDate);

    licenceScheduleExpiry = new LicenceScheduleExpiry();
    licenceScheduleExpiry.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    licenceScheduleExpiry.setExpiryDate(LocalDate.of(2050, 1, 1));
    licenceScheduleExpiry.setLicenceSchedule(licenceSchedule);

    em.persist(licenceScheduleExpiry);

    licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    licenceScheduleTerm.setLicenceSchedule(licenceSchedule);

    em.persist(licenceScheduleTerm);

    licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    licenceSchedulePhase.setLicenceScheduleTerm(licenceScheduleTerm);
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);
    licenceSchedulePhase.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));
    licenceSchedulePhase.setLicenceSchedule(licenceSchedule);

    em.persist(licenceSchedulePhase);

    termLinkedActivity = new WorkProgrammeActivity();
    termLinkedActivity.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    termLinkedActivity.setLicenceScheduleTerm(licenceScheduleTerm);
    termLinkedActivity.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    termLinkedActivity.setRelativeDuration(new ThreeFieldDuration(1, 0, 0));
    termLinkedActivity.setLicenceSchedule(licenceSchedule);

    em.persist(termLinkedActivity);

    phaseLinkedActivity = new WorkProgrammeActivity();
    phaseLinkedActivity.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    phaseLinkedActivity.setLicenceSchedulePhase(licenceSchedulePhase);
    phaseLinkedActivity.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    phaseLinkedActivity.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));
    phaseLinkedActivity.setLicenceSchedule(licenceSchedule);

    em.persist(phaseLinkedActivity);

    termLinkedRate = new LicenceScheduleRate();
    termLinkedRate.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    termLinkedRate.setLicenceScheduleTerm(licenceScheduleTerm);
    termLinkedRate.setRateDefinitionOption(RateDefinitionOption.TERM);
    termLinkedRate.setLicenceSchedule(licenceSchedule);

    em.persist(termLinkedRate);

    phaseLinkedRate = new LicenceScheduleRate();
    phaseLinkedRate.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    phaseLinkedRate.setLicenceSchedulePhase(licenceSchedulePhase);
    phaseLinkedRate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    phaseLinkedRate.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    phaseLinkedRate.setLicenceSchedule(licenceSchedule);

    em.persist(phaseLinkedRate);

    termLinkedEvent = new OtherScheduleEvent();
    termLinkedEvent.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    termLinkedEvent.setLicenceScheduleTerm(licenceScheduleTerm);
    termLinkedEvent.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    termLinkedEvent.setRelativeDuration(new ThreeFieldDuration(1, 0, 0));
    termLinkedEvent.setLicenceSchedule(licenceSchedule);

    em.persist(termLinkedEvent);

    phaseLinkedEvent = new OtherScheduleEvent();
    phaseLinkedEvent.setLicenceScheduleDetail(oldLicenceScheduleDetail);
    phaseLinkedEvent.setLicenceSchedulePhase(licenceSchedulePhase);
    phaseLinkedEvent.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    phaseLinkedEvent.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));
    phaseLinkedEvent.setLicenceSchedule(licenceSchedule);

    em.persist(phaseLinkedEvent);
    em.flush();
  }
}

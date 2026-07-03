package uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation;

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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityRepository;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceScheduleCalculationServiceIntegrationTest {

  @Autowired
  private EntityManager em;

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
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  private LicenceSchedule licenceSchedule;

  private LicenceScheduleDetail licenceScheduleDetail;

  private LicenceStartDate licenceStartDate;

  private LicenceScheduleTerm licenceScheduleTerm;

  private LicenceScheduleTerm licenceScheduleTerm2;

  private LicenceScheduleTerm licenceScheduleTerm3;

  private LicenceSchedulePhase licenceSchedulePhase;

  private LicenceSchedulePhase licenceSchedulePhase2;

  private LicenceSchedulePhase licenceSchedulePhase3;

  private WorkProgrammeActivity workProgrammeActivity;

  private WorkProgrammeActivity workProgrammeActivity2;

  private LicenceScheduleRate linkedRate;

  private LicenceScheduleRate startDateRate;

  private LicenceScheduleRate relativeRate;

  private OtherScheduleEvent otherScheduleEvent;

  private OtherScheduleEvent otherScheduleEvent2;

  @Test
  void calculateAndSaveLicenceScheduleDates() {
    createDbBaseline();

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);

    licenceScheduleTerm.setStartDate(licenceStartDate.getStartDate());
    licenceScheduleTerm.setEndDate(LocalDate.of(2025, 12, 31));

    licenceScheduleTerm2.setStartDate(licenceScheduleTerm.getEndDate().plusDays(1));
    licenceScheduleTerm2.setEndDate(licenceScheduleTerm.getEndDate().plusYears(1));

    licenceScheduleTerm3.setStartDate(licenceScheduleTerm2.getEndDate().plusDays(1));
    licenceScheduleTerm3.setEndDate(licenceScheduleTerm2.getEndDate().plusYears(1));

    licenceSchedulePhase.setStartDate(licenceStartDate.getStartDate());
    licenceSchedulePhase.setEndDate(LocalDate.of(2025, 1, 31));

    licenceSchedulePhase2.setStartDate(licenceSchedulePhase.getEndDate().plusDays(1));
    licenceSchedulePhase2.setEndDate(licenceSchedulePhase.getEndDate().plusMonths(1));

    licenceSchedulePhase3.setStartDate(licenceSchedulePhase2.getEndDate().plusDays(1));
    licenceSchedulePhase3.setEndDate(licenceSchedulePhase2.getEndDate().plusMonths(1));

    workProgrammeActivity.setDueDate(licenceScheduleTerm.getStartDate().plusYears(1));
    workProgrammeActivity2.setDueDate(licenceSchedulePhase2.getStartDate().plusMonths(1));

    linkedRate.setStartDate(licenceScheduleTerm2.getStartDate());
    startDateRate.setStartDate(licenceSchedulePhase.getStartDate());
    relativeRate.setStartDate(licenceSchedulePhase.getStartDate().plusMonths(1));

    otherScheduleEvent.setEventDate(licenceScheduleTerm.getStartDate().plusYears(1));
    otherScheduleEvent2.setEventDate(licenceSchedulePhase2.getStartDate().plusMonths(1));

    var expectedTermResult = List.of(licenceScheduleTerm, licenceScheduleTerm2, licenceScheduleTerm3);
    var actualTermResult = licenceScheduleTermRepository.findAll();

    assertThat(actualTermResult)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(expectedTermResult);

    var expectedPhaseResult = List.of(licenceSchedulePhase, licenceSchedulePhase2, licenceSchedulePhase3);
    var actualPhaseResult = licenceSchedulePhaseRepository.findAll();

    assertThat(actualPhaseResult)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(expectedPhaseResult);

    var expectedActivityResult = List.of(workProgrammeActivity, workProgrammeActivity2);
    var actualActivityResult = workProgrammeActivityRepository.findAll();

    assertThat(actualActivityResult)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .ignoringCollectionOrder()
        .isEqualTo(expectedActivityResult);

    var expectedRateResult = List.of(linkedRate, startDateRate, relativeRate);
    var actualRateResult = licenceScheduleRateRepository.findAll();

    assertThat(actualRateResult)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .ignoringCollectionOrder()
        .isEqualTo(expectedRateResult);

    var expectedOtherScheduleEventResult = List.of(otherScheduleEvent, otherScheduleEvent2);
    var actualOtherScheduleEventResult = otherScheduleEventRepository.findAll();

    assertThat(actualOtherScheduleEventResult)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .ignoringCollectionOrder()
        .isEqualTo(expectedOtherScheduleEventResult);
  }

  private void createDbBaseline() {
    Licence licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();
    em.persist(licence);

    licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);
    em.persist(licenceSchedule);

    licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    em.persist(licenceScheduleDetail);

    licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    em.persist(licenceStartDate);

    licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setLicenceSchedule(licenceSchedule);
    licenceScheduleTerm.setTermType(TermType.INITIAL);
    licenceScheduleTerm.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(licenceScheduleTerm);

    licenceScheduleTerm2 = new LicenceScheduleTerm();
    licenceScheduleTerm2.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm2.setLicenceSchedule(licenceSchedule);
    licenceScheduleTerm2.setTermType(TermType.SECOND);
    licenceScheduleTerm2.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(licenceScheduleTerm2);

    licenceScheduleTerm3 = new LicenceScheduleTerm();
    licenceScheduleTerm3.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm3.setLicenceSchedule(licenceSchedule);
    licenceScheduleTerm3.setTermType(TermType.THIRD);
    licenceScheduleTerm3.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(licenceScheduleTerm3);

    licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceSchedulePhase.setLicenceSchedule(licenceSchedule);
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);
    licenceSchedulePhase.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    em.persist(licenceSchedulePhase);

    licenceSchedulePhase2 = new LicenceSchedulePhase();
    licenceSchedulePhase2.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceSchedulePhase2.setLicenceSchedule(licenceSchedule);
    licenceSchedulePhase2.setPhaseType(PhaseType.PHASE_B);
    licenceSchedulePhase2.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    em.persist(licenceSchedulePhase2);

    licenceSchedulePhase3 = new LicenceSchedulePhase();
    licenceSchedulePhase3.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceSchedulePhase3.setLicenceSchedule(licenceSchedule);
    licenceSchedulePhase3.setPhaseType(PhaseType.PHASE_C);
    licenceSchedulePhase3.setPhaseDuration(new ThreeFieldDuration(0, 1, 0));

    em.persist(licenceSchedulePhase3);

    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setLicenceScheduleDetail(licenceScheduleDetail);
    workProgrammeActivity.setLicenceSchedule(licenceSchedule);
    workProgrammeActivity.setLicenceScheduleTerm(licenceScheduleTerm);
    workProgrammeActivity.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    workProgrammeActivity.setRelativeDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(workProgrammeActivity);

    workProgrammeActivity2 = new WorkProgrammeActivity();
    workProgrammeActivity2.setLicenceScheduleDetail(licenceScheduleDetail);
    workProgrammeActivity2.setLicenceSchedule(licenceSchedule);
    workProgrammeActivity2.setLicenceSchedulePhase(licenceSchedulePhase2);
    workProgrammeActivity2.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    workProgrammeActivity2.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    em.persist(workProgrammeActivity2);

    linkedRate = new LicenceScheduleRate();
    linkedRate.setLicenceScheduleDetail(licenceScheduleDetail);
    linkedRate.setLicenceSchedule(licenceSchedule);
    linkedRate.setLicenceScheduleTerm(licenceScheduleTerm2);
    linkedRate.setRateDefinitionOption(RateDefinitionOption.TERM);

    em.persist(linkedRate);

    startDateRate = new LicenceScheduleRate();
    startDateRate.setLicenceScheduleDetail(licenceScheduleDetail);
    startDateRate.setLicenceSchedule(licenceSchedule);
    startDateRate.setLicenceSchedulePhase(licenceSchedulePhase);
    startDateRate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    startDateRate.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    em.persist(startDateRate);

    relativeRate = new LicenceScheduleRate();
    relativeRate.setLicenceScheduleDetail(licenceScheduleDetail);
    relativeRate.setLicenceSchedule(licenceSchedule);
    relativeRate.setLicenceSchedulePhase(licenceSchedulePhase);
    relativeRate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    relativeRate.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    relativeRate.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    em.persist(relativeRate);

    otherScheduleEvent = new OtherScheduleEvent();
    otherScheduleEvent.setLicenceScheduleDetail(licenceScheduleDetail);
    otherScheduleEvent.setLicenceSchedule(licenceSchedule);
    otherScheduleEvent.setLicenceScheduleTerm(licenceScheduleTerm);
    otherScheduleEvent.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    otherScheduleEvent.setRelativeDuration(new ThreeFieldDuration(1, 0, 0));

    em.persist(otherScheduleEvent);

    otherScheduleEvent2 = new OtherScheduleEvent();
    otherScheduleEvent2.setLicenceScheduleDetail(licenceScheduleDetail);
    otherScheduleEvent2.setLicenceSchedule(licenceSchedule);
    otherScheduleEvent2.setLicenceSchedulePhase(licenceSchedulePhase2);
    otherScheduleEvent2.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    otherScheduleEvent2.setRelativeDuration(new ThreeFieldDuration(0, 1, 0));

    em.persist(otherScheduleEvent2);
    em.flush();
  }
}
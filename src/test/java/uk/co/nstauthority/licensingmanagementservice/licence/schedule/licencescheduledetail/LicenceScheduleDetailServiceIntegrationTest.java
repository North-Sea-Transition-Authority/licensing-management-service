package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicenceScheduleDetailServiceIntegrationTest {

  @Autowired
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Autowired
  private LicenceScheduleService licenceScheduleService;

  @Autowired
  private EntityManager em;

  @Autowired
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Test
  void searchByLicenceReferenceLicenceTypeAndStatus() {
    var pastDate = LocalDate.now().minusDays(1);
    var futureTermEndDate = LocalDate.now().plusYears(1);

    var licenceScheduleDetail = createLicenceAndScheduleDetail(1, "CS001", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);
    createLicenceAndScheduleDetail(2, "CS002", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);
    var licenceScheduleDetail3 = createLicenceAndScheduleDetail(4, "EX011", LicenceType.LANDWARD_PRODUCTION, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);
    createLicenceAndScheduleDetail(5, "EX012", LicenceType.LANDWARD_PRODUCTION, LicenceScheduleDetailStatus.DRAFT, pastDate, futureTermEndDate);
    createLicenceAndScheduleDetail(6, "P001", LicenceType.SEAWARD_PRODUCTION, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);

    em.flush();

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        "1",
        List.of(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION),
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .isEqualTo(List.of(licenceScheduleDetail, licenceScheduleDetail3));
  }

  @Test
  void searchByLicenceReferenceLicenceTypeAndStatus_excludesFutureStartDates() {
    var pastDate = LocalDate.now().minusDays(1);
    var futureDate = LocalDate.now().plusDays(1);
    var futureTermEndDate = LocalDate.now().plusYears(1);

    var pastScheduleDetail = createLicenceAndScheduleDetail(1, "CS001", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);
    createLicenceAndScheduleDetail(2, "CS002", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, futureDate, futureTermEndDate);

    em.flush();

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        "CS",
        List.of(LicenceType.CARBON_STORAGE),
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(pastScheduleDetail);
  }

  @Test
  void searchByLicenceReferenceLicenceTypeAndStatus_excludesScheduleDetailsWithNoStartDate() {
    var pastDate = LocalDate.now().minusDays(1);
    var futureTermEndDate = LocalDate.now().plusYears(1);

    var scheduleDetailWithStartDate = createLicenceAndScheduleDetail(1, "CS001", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);
    createLicenceAndScheduleDetail(2, "CS002", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, null, futureTermEndDate);

    em.flush();

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        "CS",
        List.of(LicenceType.CARBON_STORAGE),
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(scheduleDetailWithStartDate);
  }

  @Test
  void searchByLicenceReferenceLicenceTypeAndStatus_excludesScheduleDetailsWithAllTermsInThePast() {
    var pastDate = LocalDate.now().minusDays(1);
    var futureTermEndDate = LocalDate.now().plusYears(1);
    var pastTermEndDate = LocalDate.now().minusDays(1);

    var currentScheduleDetail = createLicenceAndScheduleDetail(1, "CS001", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, pastDate, futureTermEndDate);
    createLicenceAndScheduleDetail(2, "CS002", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE, pastDate, pastTermEndDate);

    em.flush();

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        "CS",
        List.of(LicenceType.CARBON_STORAGE),
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(currentScheduleDetail);
  }

  private LicenceScheduleDetail createLicenceAndScheduleDetail(int id,
                                                               String licenceReference,
                                                               LicenceType licenceType,
                                                               LicenceScheduleDetailStatus licenceScheduleDetailStatus,
                                                               LocalDate startDate,
                                                               LocalDate termEndDate) {
    var licence = createLicence(id, licenceReference, licenceType);
    var licenceSchedule = createLicenceSchedule(licence);
    var licenceScheduleDetail = createLicenceScheduleDetail(licenceSchedule, licenceScheduleDetailStatus);

    if (startDate != null) {
      createLicenceStartDate(licenceScheduleDetail, startDate);
    }

    createLicenceScheduleTerm(licenceScheduleDetail, termEndDate);

    return licenceScheduleDetail;
  }

  private LicenceScheduleDetail createLicenceScheduleDetail(LicenceSchedule licenceSchedule, LicenceScheduleDetailStatus active) {
    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(active)
        .build();

    em.persist(licenceScheduleDetail);
    return licenceScheduleDetail;
  }

  private LicenceSchedule createLicenceSchedule(Licence licence) {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);

    em.persist(licenceSchedule);
    return licenceSchedule;
  }

  private LicenceStartDate createLicenceStartDate(LicenceScheduleDetail licenceScheduleDetail, LocalDate startDate) {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceStartDate.setStartDate(startDate);

    em.persist(licenceStartDate);
    return licenceStartDate;
  }

  private LicenceScheduleTerm createLicenceScheduleTerm(LicenceScheduleDetail licenceScheduleDetail, LocalDate endDate) {
    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    term.setEndDate(endDate);

    em.persist(term);
    return term;
  }

  private Licence createLicence(int id, String licenceReference, LicenceType licenceType) {
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .build();

    em.persist(licence);
    return licence;
  }

}
package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
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
    var licenceScheduleDetail = createLicenceAndScheduleDetail(1, "CS001", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE);
    createLicenceAndScheduleDetail(2, "CS002", LicenceType.CARBON_STORAGE, LicenceScheduleDetailStatus.ACTIVE);
    var licenceScheduleDetail3 = createLicenceAndScheduleDetail(4, "EX011", LicenceType.LANDWARD_PRODUCTION, LicenceScheduleDetailStatus.ACTIVE);
    createLicenceAndScheduleDetail(5, "EX012", LicenceType.LANDWARD_PRODUCTION, LicenceScheduleDetailStatus.DRAFT);
    createLicenceAndScheduleDetail(6, "P001", LicenceType.SEAWARD_PRODUCTION, LicenceScheduleDetailStatus.ACTIVE);

    em.flush();

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        "1",
        List.of(LicenceType.CARBON_STORAGE, LicenceType.LANDWARD_PRODUCTION),
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .isEqualTo(List.of(licenceScheduleDetail, licenceScheduleDetail3));
  }

  private LicenceScheduleDetail createLicenceAndScheduleDetail(int id,
                                                               String licenceReference,
                                                               LicenceType licenceType,
                                                               LicenceScheduleDetailStatus licenceScheduleDetailStatus) {
    var licence = createLicence(id, licenceReference, licenceType);
    var licenceSchedule = createLicenceSchedule(licence);
    return createLicenceScheduleDetail(licenceSchedule, licenceScheduleDetailStatus);
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
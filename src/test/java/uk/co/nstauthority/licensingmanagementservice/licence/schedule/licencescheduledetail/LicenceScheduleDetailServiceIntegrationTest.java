package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
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
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference("CS001")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    em.persist(licence);

    var licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceReference("CS002")
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .build();

    em.persist(licence2);

    var licence3 = LicenceTestUtil.builder()
        .withId(3)
        .withLicenceReference("CS011")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    em.persist(licence3);

    var licence4 = LicenceTestUtil.builder()
        .withId(4)
        .withLicenceReference("P001")
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    em.persist(licence4);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);

    em.persist(licenceSchedule);

    var licenceSchedule2 = LicenceScheduleTestUtil.createLicenceSchedule(null, licence2);

    em.persist(licenceSchedule2);

    var licenceSchedule3 = LicenceScheduleTestUtil.createLicenceSchedule(null, licence3);

    em.persist(licenceSchedule3);

    var licenceSchedule4 = LicenceScheduleTestUtil.createLicenceSchedule(null, licence4);

    em.persist(licenceSchedule4);

    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();

    em.persist(licenceScheduleDetail);

    var licenceScheduleDetail2 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule2)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();

    em.persist(licenceScheduleDetail2);

    var licenceScheduleDetail3 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule3)
        .withStatus(LicenceScheduleDetailStatus.DRAFT)
        .build();

    em.persist(licenceScheduleDetail3);

    var licenceScheduleDetail4 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule3)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();

    em.persist(licenceScheduleDetail4);

    em.flush();

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        "1",
        LicenceType.CARBON_STORAGE,
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .isEqualTo(List.of(licenceScheduleDetail));
  }

}
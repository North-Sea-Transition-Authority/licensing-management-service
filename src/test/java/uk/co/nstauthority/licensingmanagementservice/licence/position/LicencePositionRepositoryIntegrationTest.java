package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class LicencePositionRepositoryIntegrationTest {

  private static final LocalDate JANUARY = LocalDate.of(2026, Month.JANUARY, 1);
  private static final LocalDate FEBRUARY = LocalDate.of(2026, Month.FEBRUARY, 1);
  private static final LocalDate MARCH = LocalDate.of(2026, Month.MARCH, 1);

  @Autowired
  private LicencePositionRepository licencePositionRepository;

  @Autowired
  private EntityManager em;

  private Licence firstLicence;
  private Licence secondLicence;
  private Licence otherLicence;
  private LicenceTransaction licenceTransaction;

  @BeforeEach
  void setUp() {
    firstLicence = persist(licence(9101, "P9101"));
    secondLicence = persist(licence(9102, "P9102"));
    otherLicence = persist(licence(9103, "P9103"));
    licenceTransaction = persist(LicenceTransactionTestUtil.newBuilder()
        .withId(null)
        .withRegulatorReference("XPT/1")
        .build());
  }

  @Test
  void findByLicenceInAndStatusOrderByPositionDateAscPositionDateOrderAsc() {
    var firstLicenceMarchPosition = persist(position(firstLicence, MARCH, 1, LicencePositionStatus.EXECUTED));
    var firstLicenceJanuarySecondPosition = persist(position(firstLicence, JANUARY, 2, LicencePositionStatus.EXECUTED));
    var firstLicenceJanuaryFirstPosition = persist(position(firstLicence, JANUARY, 1, LicencePositionStatus.EXECUTED));
    persist(position(firstLicence, FEBRUARY, 1, LicencePositionStatus.SUBMITTED));
    var secondLicenceFebruaryPosition = persist(position(secondLicence, FEBRUARY, 1, LicencePositionStatus.EXECUTED));
    persist(position(otherLicence, JANUARY, 1, LicencePositionStatus.EXECUTED));
    em.flush();
    em.clear();

    var result = licencePositionRepository.findByLicenceInAndStatusOrderByPositionDateAscPositionDateOrderAsc(
        List.of(firstLicence, secondLicence),
        LicencePositionStatus.EXECUTED
    );

    assertThat(result)
        .extracting(LicencePosition::getId)
        .containsExactly(
            firstLicenceJanuaryFirstPosition.getId(),
            firstLicenceJanuarySecondPosition.getId(),
            secondLicenceFebruaryPosition.getId(),
            firstLicenceMarchPosition.getId()
        );
  }

  private <T> T persist(T entity) {
    em.persist(entity);
    return entity;
  }

  private static Licence licence(int id, String licenceReference) {
    return LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicencePrefix("P")
        .withLicenceNumber(String.valueOf(id))
        .withLicenceReference(licenceReference)
        .build();
  }

  private LicencePosition position(
      Licence licence,
      LocalDate positionDate,
      int positionDateOrder,
      LicencePositionStatus status
  ) {
    return LicencePositionTestUtil.newBuilder()
        .withId(null)
        .withLicence(licence)
        .withLicenceTransaction(licenceTransaction)
        .withPositionDate(positionDate)
        .withPositionOrder(positionDateOrder)
        .withStatus(status)
        .build();
  }
}

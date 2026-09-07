package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * The writeback endpoint end to end: a licence holding positions, position changes, corrections and
 * position corrections is rebuilt out of PEARS, and the whole rebuild is one transaction.
 *
 * <p>The application's own database is the Testcontainers PostgreSQL instance {@code @IntegrationTest}.
 * PEARS is read through {@link PearsLicenceService}, which is mocked below. The {@code pears.datasource}
 * properties are only there to satisfy the endpoint's {@link ConditionalOnPearsDataSource}, and
 * describe a datasource that is never connected to.
 */
@IntegrationTest
@TestPropertySource(properties = {
    "pears.datasource.url=jdbc:postgresql://pears.invalid:5432/pears",
    "pears.datasource.username=never-connected-to",
    "pears.datasource.password=never-connected-to",
    "pears.datasource.driver-class-name=org.postgresql.Driver"
})
class PearsLicenceWritebackEndpointIntegrationTest {

  private static final String WRITEBACK_URL = "/actuator/pears-licence-writeback";

  // set by the development profile, which @IntegrationTest activates
  private static final String ACTUATOR_API_KEY = "dev1";

  private static final LocalDate FIRST_POSITION_DATE = LocalDate.of(2024, Month.JANUARY, 1);
  private static final LocalDate DRAFT_POSITION_DATE = LocalDate.of(2024, Month.FEBRUARY, 2);
  private static final LocalDate LAST_POSITION_DATE = LocalDate.of(2025, Month.MARCH, 4);

  @MockitoBean
  private PearsLicenceService pearsLicenceService;

  @MockitoSpyBean
  private LicenceTransactionService licenceTransactionService;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private LicenceRepository licenceRepository;

  @Autowired
  private LicenceTransactionRepository licenceTransactionRepository;

  @Autowired
  private LicencePositionRepository licencePositionRepository;

  @Autowired
  private LicencePositionChangeRepository licencePositionChangeRepository;

  @Autowired
  private LicenceCorrectionRepository licenceCorrectionRepository;

  @Autowired
  private LicencePositionCorrectionRepository licencePositionCorrectionRepository;

  private Licence licence;
  private Licence otherLicence;
  private Licence licenceWithoutPositions;

  private LicenceTransaction firstTransaction;
  private LicenceTransaction sharedTransaction;
  private LicenceTransaction draftTransaction;

  private LicencePosition firstPosition;
  private LicencePosition secondPosition;
  private LicencePosition draftPosition;
  private LicencePosition otherLicencePosition;

  private LicencePositionChange firstPositionChange;
  private LicencePositionChange secondPositionChange;
  private LicencePositionChange draftPositionChange;
  private LicencePositionChange otherLicencePositionChange;

  private LicenceCorrection inProgressCorrection;
  private LicenceCorrection completeCorrection;
  private LicenceCorrection otherLicenceCorrection;

  private LicencePositionCorrection inProgressPositionCorrection;
  private LicencePositionCorrection draftPositionCorrection;
  private LicencePositionCorrection completePositionCorrection;
  private LicencePositionCorrection otherLicencePositionCorrection;

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(status -> {
      licence = licenceRepository.save(licence(9001, "1", "P1"));
      otherLicence = licenceRepository.save(licence(9002, "2", "P2"));
      licenceWithoutPositions = licenceRepository.save(licence(9003, "3", "P3"));

      firstTransaction = persist(transaction("XPT/EXISTING-1"));
      // held by a position of each licence, so the writeback of one must leave it behind for the other
      sharedTransaction = persist(transaction("XPT/EXISTING-SHARED"));
      draftTransaction = persist(transaction("XPT/EXISTING-DRAFT"));

      firstPosition = persist(position(licence, firstTransaction, FIRST_POSITION_DATE, 1, true));
      secondPosition = persist(position(licence, sharedTransaction, FIRST_POSITION_DATE, 2, true));
      draftPosition = persist(position(licence, draftTransaction, DRAFT_POSITION_DATE, 1, false));
      otherLicencePosition = persist(position(otherLicence, sharedTransaction, FIRST_POSITION_DATE, 1, true));

      firstPositionChange = persist(positionChange(firstPosition));
      secondPositionChange = persist(positionChange(secondPosition));
      draftPositionChange = persist(positionChange(draftPosition));
      otherLicencePositionChange = persist(positionChange(otherLicencePosition));

      inProgressCorrection = persist(correction(licence, "CORRECTION-1", LicenceCorrectionStatus.IN_PROGRESS));
      completeCorrection = persist(correction(licence, "CORRECTION-2", LicenceCorrectionStatus.COMPLETE));
      otherLicenceCorrection = persist(correction(otherLicence, "CORRECTION-3", LicenceCorrectionStatus.IN_PROGRESS));

      inProgressPositionCorrection = persist(positionCorrection(inProgressCorrection, firstPosition));
      draftPositionCorrection = persist(positionCorrection(inProgressCorrection, draftPosition));
      completePositionCorrection = persist(positionCorrection(completeCorrection, secondPosition));
      otherLicencePositionCorrection = persist(positionCorrection(otherLicenceCorrection, otherLicencePosition));
    });
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(status -> {
      licencePositionCorrectionRepository.deleteAllInBatch();
      licenceCorrectionRepository.deleteAllInBatch();
      licencePositionChangeRepository.deleteAllInBatch();
      licencePositionRepository.deleteAllInBatch();
      licenceTransactionRepository.deleteAllInBatch();
      licenceRepository.deleteAllById(
          List.of(licence.getId(), otherLicence.getId(), licenceWithoutPositions.getId()));
    });
  }

  @Test
  void overwriteLicencePositionsFromPears_whenLicenceHasPositionsAndCorrections_thenTheyAreReplacedByPearsPositions() {
    when(pearsLicenceService.livePositions("P", 1)).thenReturn(pearsPositions());

    var response = writeback("P1", LicenceWritebackResult.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(new LicenceWritebackResult("Saved 3 positions for licence P1"));

    assertThat(licencePositionRepository.findByLicence(licence))
        .extracting(
            LicencePosition::getPositionDate,
            LicencePosition::getPositionDateOrder,
            LicencePosition::isExecuted,
            position -> position.getLicenceTransaction().getRegulatorReference(),
            LicencePosition::getFeatureIds
        )
        .containsExactlyInAnyOrder(
            tuple(FIRST_POSITION_DATE, 1, true, "XPT/1", Set.of()),
            tuple(FIRST_POSITION_DATE, 2, true, "XPT/2", Set.of()),
            tuple(LAST_POSITION_DATE, 1, true, "XPT/3", Set.of())
        );

    assertThat(licencePositionChangeRepository.findAll())
        .extracting(LicencePositionChange::getId)
        .containsExactly(otherLicencePositionChange.getId());

    assertThat(licenceCorrectionRepository.findAll())
        .extracting(LicenceCorrection::getId)
        .containsExactly(otherLicenceCorrection.getId());

    assertThat(licencePositionCorrectionRepository.findAll())
        .extracting(LicencePositionCorrection::getId)
        .containsExactly(otherLicencePositionCorrection.getId());

    assertThat(licenceTransactionRepository.findAll())
        .extracting(LicenceTransaction::getRegulatorReference)
        .containsExactlyInAnyOrder("XPT/EXISTING-SHARED", "XPT/1", "XPT/2", "XPT/3");

    assertThat(licencePositionRepository.findByLicence(otherLicence))
        .extracting(LicencePosition::getId)
        .containsExactly(otherLicencePosition.getId());
  }

  @Test
  void overwriteLicencePositionsFromPears_whenLicenceHasNoPositions_thenPearsPositionsAreCreated() {
    when(pearsLicenceService.livePositions("P", 3)).thenReturn(new LivePositions("P", 3, List.of(
        new LivePositions.Position(LAST_POSITION_DATE, 2, 1, "XPT/4")
    )));

    var response = writeback("P3", LicenceWritebackResult.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(new LicenceWritebackResult("Saved 1 positions for licence P3"));

    assertThat(licencePositionRepository.findByLicence(licenceWithoutPositions))
        .extracting(
            LicencePosition::getPositionDate,
            LicencePosition::getPositionDateOrder,
            LicencePosition::isExecuted,
            position -> position.getLicenceTransaction().getRegulatorReference(),
            LicencePosition::getFeatureIds
        )
        .containsExactly(tuple(LAST_POSITION_DATE, 1, true, "XPT/4", Set.of()));

    assertThat(licencePositionRepository.findByLicence(licence))
        .extracting(LicencePosition::getId)
        .containsExactlyInAnyOrder(firstPosition.getId(), secondPosition.getId(), draftPosition.getId());
  }

  @Test
  void overwriteLicencePositionsFromPears_whenAPositionCannotBeSaved_thenNothingIsDeleted() {
    when(pearsLicenceService.livePositions("P", 1)).thenReturn(pearsPositions());
    doThrow(new IllegalStateException("Could not create licence transaction"))
        .when(licenceTransactionService).createLicenceTransaction("XPT/3");

    var response = writeback("P1", String.class);

    assertThat(response.getStatusCode().is5xxServerError()).isTrue();

    assertThat(licencePositionRepository.findByLicence(licence))
        .extracting(LicencePosition::getId)
        .containsExactlyInAnyOrder(firstPosition.getId(), secondPosition.getId(), draftPosition.getId());

    assertThat(licencePositionChangeRepository.findAll())
        .extracting(LicencePositionChange::getId)
        .containsExactlyInAnyOrder(
            firstPositionChange.getId(),
            secondPositionChange.getId(),
            draftPositionChange.getId(),
            otherLicencePositionChange.getId()
        );

    assertThat(licenceCorrectionRepository.findAll())
        .extracting(LicenceCorrection::getId)
        .containsExactlyInAnyOrder(
            inProgressCorrection.getId(),
            completeCorrection.getId(),
            otherLicenceCorrection.getId()
        );

    assertThat(licencePositionCorrectionRepository.findAll())
        .extracting(LicencePositionCorrection::getId)
        .containsExactlyInAnyOrder(
            inProgressPositionCorrection.getId(),
            draftPositionCorrection.getId(),
            completePositionCorrection.getId(),
            otherLicencePositionCorrection.getId()
        );

    assertThat(licenceTransactionRepository.findAll())
        .extracting(LicenceTransaction::getId)
        .containsExactlyInAnyOrder(
            firstTransaction.getId(),
            sharedTransaction.getId(),
            draftTransaction.getId()
        );
  }

  private <T> ResponseEntity<T> writeback(String licenceReference, Class<T> responseType) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(ACTUATOR_API_KEY);

    return testRestTemplate.exchange(
        WRITEBACK_URL,
        HttpMethod.POST,
        new HttpEntity<>(Map.of("licenceReference", licenceReference), headers),
        responseType
    );
  }

  private LivePositions pearsPositions() {
    return new LivePositions("P", 1, List.of(
        new LivePositions.Position(FIRST_POSITION_DATE, 6, 1, "XPT/1"),
        new LivePositions.Position(FIRST_POSITION_DATE, 9, 2, "XPT/2"),
        new LivePositions.Position(LAST_POSITION_DATE, 2, 1, "XPT/3")
    ));
  }

  private <T> T persist(T entity) {
    entityManager.persist(entity);
    return entity;
  }

  private Licence licence(int id, String licenceNumber, String licenceReference) {
    return LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicencePrefix("P")
        .withLicenceNumber(licenceNumber)
        .withLicenceReference(licenceReference)
        .build();
  }

  private LicenceTransaction transaction(String regulatorReference) {
    return LicenceTransactionTestUtil.newBuilder()
        .withId(null)
        .withRegulatorReference(regulatorReference)
        .build();
  }

  private LicencePosition position(
      Licence licence,
      LicenceTransaction licenceTransaction,
      LocalDate positionDate,
      int positionDateOrder,
      boolean isExecuted
  ) {
    return LicencePositionTestUtil.newBuilder()
        .withId(null)
        .withLicence(licence)
        .withLicenceTransaction(licenceTransaction)
        .withPositionDate(positionDate)
        .withPositionOrder(positionDateOrder)
        .withIsExecuted(isExecuted)
        .withFeatureIds(Set.of(UUID.randomUUID()))
        .build();
  }

  private LicencePositionChange positionChange(LicencePosition licencePosition) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withId(null)
        .withLicencePosition(licencePosition)
        .build();
  }

  private LicenceCorrection correction(Licence licence, String correctionReference, LicenceCorrectionStatus status) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withId(null)
        .withLicence(licence)
        .withCorrectionReference(correctionReference)
        .withStatus(status)
        .build();
  }

  private LicencePositionCorrection positionCorrection(LicenceCorrection licenceCorrection, LicencePosition targetLicencePosition) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(null)
        .withLicenceCorrection(licenceCorrection)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(targetLicencePosition)
        .build();
  }
}

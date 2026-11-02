package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class PearsLicenceWritebackAllEndpointTest {

  private static final int PARALLELISM = 2;
  private static final int WRITE_PARALLELISM = 1;

  @Mock
  private PearsLicenceService pearsLicenceService;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceWritebackService licenceWritebackService;

  private PearsLicenceWritebackAllEndpoint pearsLicenceWritebackAllEndpoint;

  @BeforeEach
  void setUp() {
    pearsLicenceWritebackAllEndpoint = new PearsLicenceWritebackAllEndpoint(
        pearsLicenceService,
        licenceService,
        licenceWritebackService,
        PARALLELISM,
        WRITE_PARALLELISM
    );
  }

  @ParameterizedTest
  @CsvSource({"0, 1", "-1, 1", "1, 0", "1, -1"})
  void constructor_whenAParallelismIsBelowOne_thenThrows(int parallelism, int writeParallelism) {
    assertThatThrownBy(() -> new PearsLicenceWritebackAllEndpoint(
        pearsLicenceService,
        licenceService,
        licenceWritebackService,
        parallelism,
        writeParallelism
    ))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void overwriteAllLicencePositionsFromPears_whenSeveralLicences_thenAsManyAsTheParallelismAreReadAtOnce() {
    var firstLicence = givenPearsAndLmsHold("P1", 1);
    var secondLicence = givenPearsAndLmsHold("P2", 2);
    givenPearsHolds(List.of(firstLicence, secondLicence));
    givenReadsWaitForEachOther(firstLicence, secondLicence);
    when(licenceWritebackService.overwriteLicencePositionsFromPears(firstLicence.licence(), firstLicence.history()))
        .thenReturn(writeback("P", 1, 1, 1, 1, 0));
    when(licenceWritebackService.overwriteLicencePositionsFromPears(secondLicence.licence(), secondLicence.history()))
        .thenReturn(writeback("P", 2, 1, 1, 1, 0));

    var result = pearsLicenceWritebackAllEndpoint.overwriteAllLicencePositionsFromPears();

    assertThat(result).isEqualTo(
        new LicenceWritebackAllResult(2, 2, List.of(), List.of(), Map.of(), 2, 2, 2, 0));
  }

  @Test
  void overwriteAllLicencePositionsFromPears_whenSeveralLicencesAreRead_thenNoMoreThanTheWriteParallelismAreWrittenAtOnce() {
    var firstLicence = givenPearsAndLmsHold("P1", 1);
    var secondLicence = givenPearsAndLmsHold("P2", 2);
    givenPearsHolds(List.of(firstLicence, secondLicence));
    givenReadsWaitForEachOther(firstLicence, secondLicence);
    var writing = new AtomicInteger();
    var mostWrittenAtOnce = new AtomicInteger();
    Answer<LicenceWriteback> write = invocation -> {
      mostWrittenAtOnce.accumulateAndGet(writing.incrementAndGet(), Math::max);
      Thread.sleep(100);
      writing.decrementAndGet();
      return writeback("P", 1, 1, 1, 1, 0);
    };
    when(licenceWritebackService.overwriteLicencePositionsFromPears(firstLicence.licence(), firstLicence.history()))
        .thenAnswer(write);
    when(licenceWritebackService.overwriteLicencePositionsFromPears(secondLicence.licence(), secondLicence.history()))
        .thenAnswer(write);

    pearsLicenceWritebackAllEndpoint.overwriteAllLicencePositionsFromPears();

    assertThat(mostWrittenAtOnce).hasValue(WRITE_PARALLELISM);
  }

  @Test
  void overwriteAllLicencePositionsFromPears_whenEveryLicenceIsWrittenBack_thenTheirCountsAreSummed() {
    var firstLicence = givenPearsAndLmsHold("P1", 1);
    var secondLicence = givenPearsAndLmsHold("P2", 2);
    givenPearsHolds(List.of(firstLicence, secondLicence));
    givenReadable(firstLicence);
    givenReadable(secondLicence);
    when(licenceWritebackService.overwriteLicencePositionsFromPears(firstLicence.licence(), firstLicence.history()))
        .thenReturn(writeback("P", 1, 1, 2, 3, 4));
    when(licenceWritebackService.overwriteLicencePositionsFromPears(secondLicence.licence(), secondLicence.history()))
        .thenReturn(writeback("P", 2, 2, 5, 6, 7));

    var result = pearsLicenceWritebackAllEndpoint.overwriteAllLicencePositionsFromPears();

    assertThat(result).isEqualTo(
        new LicenceWritebackAllResult(2, 2, List.of(), List.of(), Map.of(), 3, 7, 9, 11));
  }

  @Test
  void overwriteAllLicencePositionsFromPears_whenLicenceIsNotInLms_thenItIsListedAndNotWrittenBack() {
    when(pearsLicenceService.licenceReferences()).thenReturn(List.of(new PearsLicenceReference("P", 99)));
    when(licenceService.findLicenceByReference("P99")).thenReturn(Optional.empty());

    var result = pearsLicenceWritebackAllEndpoint.overwriteAllLicencePositionsFromPears();

    assertThat(result).isEqualTo(
        new LicenceWritebackAllResult(1, 0, List.of(), List.of("P99"), Map.of(), 0, 0, 0, 0));
    verifyNoInteractions(licenceWritebackService);
  }

  @Test
  void overwriteAllLicencePositionsFromPears_whenAReadFails_thenItIsListedAndLaterLicencesAreStillWrittenBack() {
    var failingLicence = givenPearsAndLmsHold("P1", 1);
    var laterLicence = givenPearsAndLmsHold("P2", 2);
    givenPearsHolds(List.of(failingLicence, laterLicence));
    when(licenceWritebackService.readHistory(failingLicence.licence()))
        .thenThrow(new IllegalStateException("Could not read the operation history for P1"));
    givenReadable(laterLicence);
    when(licenceWritebackService.overwriteLicencePositionsFromPears(laterLicence.licence(), laterLicence.history()))
        .thenReturn(writeback("P", 2, 1, 1, 1, 0));

    var result = pearsLicenceWritebackAllEndpoint.overwriteAllLicencePositionsFromPears();

    assertThat(result).isEqualTo(new LicenceWritebackAllResult(
        2,
        1,
        List.of(),
        List.of(),
        Map.of("P1", "Could not read the operation history for P1"),
        1,
        1,
        1,
        0
    ));
    verify(licenceWritebackService).overwriteLicencePositionsFromPears(laterLicence.licence(), laterLicence.history());
  }

  @Test
  void overwriteAllLicencePositionsFromPears_whenPearsHoldsNoPositions_thenItIsListedAsNothingToDo() {
    var licence = givenPearsAndLmsHold("P1", 1);
    givenPearsHolds(List.of(licence));
    givenReadable(licence);
    when(licenceWritebackService.overwriteLicencePositionsFromPears(licence.licence(), licence.history()))
        .thenReturn(LicenceWriteback.nothingToDo("No positions found in PEARS for licence P1", licence.history()));

    var result = pearsLicenceWritebackAllEndpoint.overwriteAllLicencePositionsFromPears();

    assertThat(result).isEqualTo(
        new LicenceWritebackAllResult(1, 0, List.of("P1"), List.of(), Map.of(), 0, 0, 0, 0));
  }

  private HeldLicence givenPearsAndLmsHold(String licenceReference, int licenceNumber) {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(licenceReference)
        .build();
    when(licenceService.findLicenceByReference(licenceReference)).thenReturn(Optional.of(licence));

    return new HeldLicence(
        new PearsLicenceReference("P", licenceNumber),
        licence,
        new PearsLicenceHistory("P", licenceNumber, List.of())
    );
  }

  private void givenPearsHolds(List<HeldLicence> licences) {
    when(pearsLicenceService.licenceReferences())
        .thenReturn(licences.stream().map(HeldLicence::pearsLicence).toList());
  }

  private void givenReadable(HeldLicence licence) {
    when(licenceWritebackService.readHistory(licence.licence())).thenReturn(licence.history());
  }

  /**
   * Each read waits for the others, so a run that reads them one at a time fails rather than hangs.
   */
  private void givenReadsWaitForEachOther(HeldLicence... licences) {
    var allReading = new CountDownLatch(licences.length);
    for (var licence : licences) {
      when(licenceWritebackService.readHistory(licence.licence())).thenAnswer(invocation -> {
        allReading.countDown();
        if (!allReading.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Licences were not read at the same time");
        }
        return licence.history();
      });
    }
  }

  private static LicenceWriteback writeback(
      String licenceType,
      int licenceNumber,
      int positions,
      int changes,
      int operations,
      int operationsIgnored
  ) {
    var positionsByTransactionId = new LinkedHashMap<Long, LicencePosition>();
    for (var transactionId = 1L; transactionId <= positions; transactionId++) {
      positionsByTransactionId.put(transactionId, LicencePositionTestUtil.newBuilder().build());
    }

    return new LicenceWriteback(
        "written back",
        new PearsLicenceHistory(licenceType, licenceNumber, List.of()),
        positionsByTransactionId,
        Map.of(),
        Map.of(),
        Map.of("PED_RETENTION_AREA_CHANGE", operationsIgnored),
        changes,
        operations
    );
  }

  private record HeldLicence(PearsLicenceReference pearsLicence, Licence licence, PearsLicenceHistory history) {
  }
}

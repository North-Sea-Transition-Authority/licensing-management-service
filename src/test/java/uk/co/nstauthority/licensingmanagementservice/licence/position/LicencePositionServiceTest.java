package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @InjectMocks
  private LicencePositionService licencePositionService;

  @Captor
  private ArgumentCaptor<LicencePosition> licencePositionArgumentCaptor;

  @ParameterizedTest
  @MethodSource("provideMaxPositionOrderCombinations")
  void createLicencePosition_noExistingPositionOnDate(Integer maxPositionOrder, int positionOrder) {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var date  = LocalDate.of(2026, 1, 1);

    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, date)).thenReturn(maxPositionOrder);

    var expectedLicencePosition = LicencePositionTestUtil.newBuilder()
        .withId(null)
        .withLicence(LICENCE)
        .withLicenceTransaction(transaction)
        .withPositionDate(date)
        .withPositionOrder(positionOrder)
        .build();

    licencePositionService.createLicencePosition(LICENCE, transaction, date);

    verify(licencePositionRepository).save(licencePositionArgumentCaptor.capture());

    assertThat(licencePositionArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedLicencePosition);
  }

  private static Stream<Arguments> provideMaxPositionOrderCombinations() {
    return Stream.of(
        Arguments.of(null, 1),
        Arguments.of(1, 2),
        Arguments.of(2, 3)
    );
  }

  @Test
  void getTimelineView() {
    var olderPosition1 = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-A").build())
        .withPositionDate(LocalDate.of(2026, 1, 1))
        .withPositionOrder(1)
        .build();
    var olderPosition2 = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-B").build())
        .withPositionDate(LocalDate.of(2026, 1, 1))
        .withPositionOrder(2)
        .build();
    var newerPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-C").build())
        .withPositionDate(LocalDate.of(2026, 6, 1))
        .withPositionOrder(1)
        .build();

    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(olderPosition2, newerPosition, olderPosition1));

    var result = licencePositionService.getTimelineView(LICENCE);

    assertThat(result).extracting(LicencePositionTimelineView::regulatorReference, LicencePositionTimelineView::positionDate)
        .containsExactly(
            tuple("REF-C", LocalDate.of(2026, 6 ,1)),
            tuple("REF-B", LocalDate.of(2026, 1, 1)),
            tuple("REF-A", LocalDate.of(2026, 1, 1))
    );
  }

  @Test
  void getChronologicalLicencePositions() {
    var oldestFirst = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, 1, 1)).withPositionOrder(1).build();
    var oldestSecond = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, 1, 1)).withPositionOrder(2).build();
    var newest = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, 6, 1)).withPositionOrder(1).build();

    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(newest, oldestSecond, oldestFirst));

    assertThat(licencePositionService.getChronologicalLicencePositions(LICENCE))
        .containsExactly(oldestFirst, oldestSecond, newest);
  }
}

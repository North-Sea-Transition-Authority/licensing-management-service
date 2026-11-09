package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();

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
    var date  = LocalDate.of(2026, Month.JANUARY, 1);

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
  void findById() {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build();
    when(licencePositionRepository.findById(POSITION_ID)).thenReturn(Optional.of(position));

    var result = licencePositionService.findById(POSITION_ID);

    assertThat(result).contains(position);
  }

  @Test
  void getPositionForLicence() {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build();
    when(licencePositionRepository.findByIdAndLicence(POSITION_ID, LICENCE))
        .thenReturn(Optional.of(position));

    assertThat(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).isEqualTo(position);
  }

  @Test
  void getPositionForLicence_whenNotFound_throws() {
    when(licencePositionRepository.findByIdAndLicence(POSITION_ID, LICENCE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> licencePositionService.getPositionForLicence(LICENCE, POSITION_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getExecutedChronologicalLicencePositions() {
    var executedPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withStatus(LicencePositionStatus.EXECUTED)
        .build();

    when(licencePositionRepository.findByLicenceInAndStatusOrderByPositionDateAscPositionDateOrderAsc(
        List.of(LICENCE),
        LicencePositionStatus.EXECUTED
    ))
        .thenReturn(List.of(executedPosition));

    var result = licencePositionService.getExecutedChronologicalLicencePositions(LICENCE);

    assertThat(result).containsExactly(executedPosition);
  }
}

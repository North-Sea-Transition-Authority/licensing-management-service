package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

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
    var newestPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-C").build())
        .withPositionDate(LocalDate.of(2026, 6, 1))
        .withPositionOrder(1)
        .build();

    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(olderPosition2, newestPosition, olderPosition1));

    var result = licencePositionService.getTimelineView(LICENCE);

    assertThat(result)
        .extracting(
            LicencePositionTimelineView::positionId,
            LicencePositionTimelineView::url,
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::formattedPositionDate)
        .containsExactly(
            tuple(newestPosition.getId(),  urlFor(newestPosition),  "REF-C", "1 June 2026"),
            tuple(olderPosition2.getId(), urlFor(olderPosition2), "REF-B", "1 January 2026"),
            tuple(olderPosition1.getId(), urlFor(olderPosition1), "REF-A", "1 January 2026")
    );
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
  void getChronologicalLicencePositions() {
    var olderPosition1 = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, 1, 1)).withPositionOrder(1).build();
    var olderPosition2 = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, 1, 1)).withPositionOrder(2).build();
    var newestPosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, 6, 1)).withPositionOrder(1).build();


    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(newestPosition, olderPosition2, olderPosition1));

    assertThat(licencePositionService.getChronologicalLicencePositions(LICENCE))
        .containsExactly(olderPosition1, olderPosition2, newestPosition);
  }

  private static String urlFor(LicencePosition position) {
    return ReverseRouter.route(on(LicencePositionTimelineController.class)
        .renderLicencePosition(position.getLicence(), position.getId()));
  }
}

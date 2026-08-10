package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LocalDate JANUARY = LocalDate.of(2026, Month.JANUARY, 1);
  private static final LocalDate FEBRUARY = LocalDate.of(2026, Month.FEBRUARY, 1);

  private static final Feature BLOCK_30_1 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
  private static final Feature BLOCK_30_2 = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
  private static final Feature SUBAREA = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "Subarea A");

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private FeatureService featureService;

  @InjectMocks
  private LicencePositionService licencePositionService;

  @Captor
  private ArgumentCaptor<LicencePosition> licencePositionArgumentCaptor;

  private static Set<UUID> featureIds(Feature... features) {
    return Stream.of(features).map(Feature::getId).collect(Collectors.toUnmodifiableSet());
  }

  private static LicencePosition position(LocalDate positionDate, int positionDateOrder, Feature... features) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(positionDate)
        .withPositionOrder(positionDateOrder)
        .withFeatureIds(featureIds(features))
        .build();
  }

  private void givenFeaturesAreResolved(Feature... features) {
    when(featureService.getFeaturesByIds(featureIds(features))).thenReturn(List.of(features));
  }

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

  @Test
  void createLicencePosition_assertFeaturesAreCarriedForwardFromThePrecedingPosition() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();

    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, FEBRUARY)).thenReturn(null);
    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(position(JANUARY, 1, BLOCK_30_1, SUBAREA)));

    var expectedLicencePosition = LicencePositionTestUtil.newBuilder()
        .withId(null)
        .withLicence(LICENCE)
        .withLicenceTransaction(transaction)
        .withPositionDate(FEBRUARY)
        .withPositionOrder(1)
        .withFeatureIds(featureIds(BLOCK_30_1, SUBAREA))
        .build();

    licencePositionService.createLicencePosition(LICENCE, transaction, FEBRUARY);

    verify(licencePositionRepository).save(licencePositionArgumentCaptor.capture());

    assertThat(licencePositionArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedLicencePosition);
  }

  @Test
  void createLicencePosition_whenPositionsShareADate_assertFeaturesOfTheHighestOrderedAreCarriedForward() {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();

    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, FEBRUARY)).thenReturn(2);
    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(position(FEBRUARY, 1, BLOCK_30_1), position(FEBRUARY, 2, BLOCK_30_2)));

    licencePositionService.createLicencePosition(LICENCE, transaction, FEBRUARY);

    verify(licencePositionRepository).save(licencePositionArgumentCaptor.capture());

    assertThat(licencePositionArgumentCaptor.getValue().getFeatureIds()).isEqualTo(featureIds(BLOCK_30_2));
  }

  private static Stream<Arguments> provideMaxPositionOrderCombinations() {
    return Stream.of(
        Arguments.of(null, 1),
        Arguments.of(1, 2),
        Arguments.of(2, 3)
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
  void getExecutedChronologicalLicencePositions_returnsOnlyExecutedInChronologicalOrder() {
    var licence = LicenceTestUtil.builder().build();

    var earlierExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1))
        .withIsExecuted(true)
        .build();
    var nonExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.MARCH, 1))
        .withIsExecuted(false)
        .build();

    when(licencePositionRepository.findByLicence(licence))
        .thenReturn(List.of(nonExecuted, earlierExecuted));

    var result = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    assertThat(result).containsExactly(earlierExecuted);
  }

  @Test
  void getExecutedChronologicalLicencePositions_sortsByDateThenOrder() {
    var licence = LicenceTestUtil.builder().build();

    var pos1 = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPositionDate(LocalDate.of(2026, Month.FEBRUARY, 1))
        .withPositionOrder(1)
        .withIsExecuted(true)
        .build();
    var pos2 = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1))
        .withPositionOrder(2)
        .withIsExecuted(true)
        .build();
    var pos3 = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1))
        .withPositionOrder(1)
        .withIsExecuted(true)
        .build();

    when(licencePositionRepository.findByLicence(licence)).thenReturn(List.of(pos1, pos2, pos3));

    var result = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    assertThat(result).containsExactly(pos3, pos2, pos1);
  }


  @Test
  void setFeatures_assertPreviouslyHeldFeaturesAreReplaced() {
    var licencePosition = position(JANUARY, 1, BLOCK_30_1);

    licencePositionService.setFeatures(licencePosition, List.of(BLOCK_30_2, SUBAREA));

    verify(licencePositionRepository).save(licencePositionArgumentCaptor.capture());

    assertThat(licencePositionArgumentCaptor.getValue().getFeatureIds())
        .isEqualTo(featureIds(BLOCK_30_2, SUBAREA));
  }

  @Test
  void setFeatures_whenAnIdIsRepeated_assertDuplicatesAreCollapsed() {
    var licencePosition = position(JANUARY, 1);

    licencePositionService.setFeatures(licencePosition, List.of(BLOCK_30_1, BLOCK_30_2, BLOCK_30_1));

    verify(licencePositionRepository).save(licencePositionArgumentCaptor.capture());

    assertThat(licencePositionArgumentCaptor.getValue().getFeatureIds())
        .isEqualTo(featureIds(BLOCK_30_1, BLOCK_30_2));
  }

  @Test
  void setFeatures_whenAFeatureHasNotBeenPersisted_assertThrows() {
    var licencePosition = position(JANUARY, 1, BLOCK_30_1);
    var unpersistedFeature = FeatureTestUtil.builder().withId(null).build();

    assertThatThrownBy(() -> licencePositionService.setFeatures(licencePosition, List.of(unpersistedFeature)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Features to be associated with licence position %s have not been persisted before updating the position features"
                .formatted(licencePosition.getId()));

    verify(licencePositionRepository, never()).save(licencePosition);
  }

  @Test
  void getFeatures() {
    var licencePosition = position(JANUARY, 1, BLOCK_30_1, SUBAREA);
    givenFeaturesAreResolved(BLOCK_30_1, SUBAREA);

    assertThat(licencePositionService.getFeatures(licencePosition))
        .containsExactlyInAnyOrder(BLOCK_30_1, SUBAREA);
  }

  @Test
  void getBlockFeatures_excludesOtherLayersAndOrdersByBlock() {
    var licencePosition = position(JANUARY, 1, SUBAREA, BLOCK_30_2, BLOCK_30_1);
    givenFeaturesAreResolved(SUBAREA, BLOCK_30_2, BLOCK_30_1);

    assertThat(licencePositionService.getBlockFeatures(licencePosition))
        .containsExactly(BLOCK_30_1, BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesAsAt_assertBlocksOfLatestPositionAtOrBeforeThatPoint() {
    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(List.of(
        position(JANUARY, 1, BLOCK_30_1),
        position(FEBRUARY, 1, BLOCK_30_2),
        position(FEBRUARY, 3, SUBAREA)));
    givenFeaturesAreResolved(BLOCK_30_2);

    assertThat(licencePositionService.getBlockFeaturesOnLicenceOnOrBefore(LICENCE, FEBRUARY, 2))
        .containsExactly(BLOCK_30_2);
  }

  @Test
  void getBlockFeaturesAsAt_whenNoPositionAtOrBeforeThatPoint_assertEmpty() {
    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(List.of(position(FEBRUARY, 1, BLOCK_30_1)));

    assertThat(licencePositionService.getBlockFeaturesOnLicenceOnOrBefore(LICENCE, JANUARY, 1)).isEmpty();
  }

  @Test
  void getBlockFeaturesAsAt_whenLatestPositionIsNotExecuted_assertItIsIgnored() {
    var notExecuted = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(FEBRUARY)
        .withPositionOrder(1)
        .withIsExecuted(false)
        .withFeatureIds(featureIds(SUBAREA))
        .build();

    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(position(JANUARY, 1, BLOCK_30_1), notExecuted));
    givenFeaturesAreResolved(BLOCK_30_1);

    assertThat(licencePositionService.getBlockFeaturesOnLicenceOnOrBefore(LICENCE, FEBRUARY, 1))
        .containsExactly(BLOCK_30_1);
  }
}

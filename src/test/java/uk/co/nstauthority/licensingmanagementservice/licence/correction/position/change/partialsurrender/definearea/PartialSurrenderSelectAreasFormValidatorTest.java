package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderSelectAreasFormValidatorTest {

  private static final String FIELD = "surrenderedFeatureIds";
  private static final String SELECT_AREAS = "Select the areas being surrendered";
  private static final String CANNOT_SELECT_ALL = "You cannot surrender all the areas in a partial surrender";
  private static final String WHOLE_BLOCK = "Select areas that do not cover the whole block";

  private static final Feature BLOCK_FEATURE = FeatureTestUtil.builder().build();
  private static final Feature FIRST_AREA = FeatureTestUtil.builder().build();
  private static final Feature SECOND_AREA = FeatureTestUtil.builder().build();
  private static final List<Feature> ACTIVE_FEATURES = List.of(FIRST_AREA, SECOND_AREA);
  private static final Feature OTHER_ACTIVE_BLOCK = FeatureTestUtil.builder().build();
  private static final Map<Feature, Map<UUID, String>> OTHER_ACTIVE_BLOCK_POLYGONS =
      Map.of(OTHER_ACTIVE_BLOCK, Map.of(UUID.randomUUID(), "other active block polygon"));

  @Mock
  private FeatureRelationshipService featureRelationshipService;

  @InjectMocks
  private PartialSurrenderSelectAreasFormValidator validator;

  private PartialSurrenderSelectAreasForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    form = new PartialSurrenderSelectAreasForm();
    errors = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasSelectionErrors_whenNoAreasSelected_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of());

    var result = validator.hasSelectionErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(SELECT_AREAS)));
  }

  @Test
  void hasSelectionErrors_whenSelectedAreaIsNotActive_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of(UUID.randomUUID()));

    var result = validator.hasSelectionErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(SELECT_AREAS)));
  }

  @Test
  void hasSelectionErrors_whenAllAreasSelected_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId(), SECOND_AREA.getId()));

    var result = validator.hasSelectionErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(CANNOT_SELECT_ALL)));
  }

  @Test
  void hasSelectionErrors_whenSelectedAreasCoverWholeBlock_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(FIRST_AREA), BLOCK_FEATURE)).thenReturn(true);

    var result = validator.hasSelectionErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(WHOLE_BLOCK)));
  }

  @Test
  void hasSelectionErrors_whenSomeButNotAllAreasSelected_thenNoErrors() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(FIRST_AREA), BLOCK_FEATURE)).thenReturn(false);

    var result = validator.hasSelectionErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("invalidSelections")
  void hasOverlapErrors_whenSelectionHasSelectionErrors_thenThrowsException(Set<UUID> selected) {
    form.setSurrenderedFeatureIds(selected);

    assertThatThrownBy(() -> validator.hasOverlapErrors(form, errors, ACTIVE_FEATURES, OTHER_ACTIVE_BLOCK_POLYGONS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Areas to surrender must be some but not all of the active areas");
  }

  private static Stream<Arguments> invalidSelections() {
    return Stream.of(
        Arguments.of(Set.of()),
        Arguments.of(Set.of(UUID.randomUUID())),
        Arguments.of(Set.of(FIRST_AREA.getId(), SECOND_AREA.getId()))
    );
  }

  @Test
  void hasOverlapErrors_whenAreasKeptOverlapOtherActiveBlocks_thenErrorPerOverlappedBlock() {
    var firstOverlappedBlock = FeatureTestUtil.builder().withFeatureName("30/2").build();
    var secondOverlappedBlock = FeatureTestUtil.builder().withFeatureName("30/3").build();
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(SECOND_AREA), OTHER_ACTIVE_BLOCK_POLYGONS))
        .thenReturn(List.of(firstOverlappedBlock, secondOverlappedBlock));

    var result = validator.hasOverlapErrors(form, errors, ACTIVE_FEATURES, OTHER_ACTIVE_BLOCK_POLYGONS);

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, List.of(
            "Block after surrender must not overlap licence block 30/2",
            "Block after surrender must not overlap licence block 30/3"
        )));
  }

  @Test
  void hasOverlapErrors_whenAreasKeptDoNotOverlapOtherActiveBlocks_thenNoErrors() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(SECOND_AREA), OTHER_ACTIVE_BLOCK_POLYGONS))
        .thenReturn(List.of());

    var result = validator.hasOverlapErrors(form, errors, ACTIVE_FEATURES, OTHER_ACTIVE_BLOCK_POLYGONS);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void getOverlapErrors_whenAreasKeptInSeveralCoordinateSystems_thenEachCheckedTogetherAgainstBlocksInTheirSystem() {
    var firstEd50AreaKept = FeatureTestUtil.builder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var secondEd50AreaKept = FeatureTestUtil.builder().withCoordinateSystem(CoordinateSystem.ED50).build();
    var wgs84AreaKept = FeatureTestUtil.builder().withCoordinateSystem(CoordinateSystem.WGS84).build();
    var ed50Block = FeatureTestUtil.builder()
        .withFeatureName("30/2")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var wgs84Block = FeatureTestUtil.builder()
        .withFeatureName("30/3")
        .withCoordinateSystem(CoordinateSystem.WGS84)
        .build();
    var ed50BlockPolygons = Map.of(UUID.randomUUID(), "ED50 block polygon");
    var wgs84BlockPolygons = Map.of(UUID.randomUUID(), "WGS84 block polygon");
    when(featureRelationshipService.get2dOverlappingFeatures(
        List.of(firstEd50AreaKept, secondEd50AreaKept),
        Map.of(ed50Block, ed50BlockPolygons)
    ))
        .thenReturn(List.of(ed50Block));
    when(featureRelationshipService.get2dOverlappingFeatures(
        List.of(wgs84AreaKept),
        Map.of(wgs84Block, wgs84BlockPolygons)
    ))
        .thenReturn(List.of(wgs84Block));

    var result = validator.getOverlapErrors(
        List.of(firstEd50AreaKept, wgs84AreaKept, secondEd50AreaKept),
        Map.of(ed50Block, ed50BlockPolygons, wgs84Block, wgs84BlockPolygons)
    );

    assertThat(result).containsExactlyInAnyOrder(
        "Block after surrender must not overlap licence block 30/2",
        "Block after surrender must not overlap licence block 30/3"
    );
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  void hasErrors_whenNoAreasSelected_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of());

    var result = validator.hasErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE, List.of());

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(SELECT_AREAS)));
  }

  @Test
  void hasErrors_whenSelectedAreaIsNotActive_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of(UUID.randomUUID()));

    var result = validator.hasErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE, List.of());

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(SELECT_AREAS)));
  }

  @Test
  void hasErrors_whenAllAreasSelected_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId(), SECOND_AREA.getId()));

    var result = validator.hasErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE, List.of());

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(CANNOT_SELECT_ALL)));
  }

  @Test
  void hasErrors_whenSelectedAreasCoverWholeBlock_thenErrorWithMessage() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(FIRST_AREA), BLOCK_FEATURE)).thenReturn(true);

    var result = validator.hasErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE, List.of());

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, Collections.singletonList(WHOLE_BLOCK)));
  }

  @Test
  void hasErrors_whenSomeButNotAllAreasSelected_thenNoErrors() {
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(FIRST_AREA), BLOCK_FEATURE)).thenReturn(false);

    var result = validator.hasErrors(form, errors, ACTIVE_FEATURES, BLOCK_FEATURE, List.of());

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void hasErrors_whenAreasKeptOverlapOtherActiveBlocks_thenErrorPerOverlappedBlock() {
    var firstOverlappedBlock = FeatureTestUtil.builder().withFeatureName("30/2").build();
    var secondOverlappedBlock = FeatureTestUtil.builder().withFeatureName("30/3").build();
    var blockInAnotherCoordinateSystem = FeatureTestUtil.builder()
        .withFeatureName("30/4")
        .withCoordinateSystem(CoordinateSystem.WGS84)
        .build();
    form.setSurrenderedFeatureIds(Set.of(FIRST_AREA.getId()));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(FIRST_AREA), BLOCK_FEATURE)).thenReturn(false);
    when(featureRelationshipService.get2dOverlappingFeatures(
        List.of(SECOND_AREA),
        List.of(firstOverlappedBlock, secondOverlappedBlock)
    )).thenReturn(List.of(firstOverlappedBlock, secondOverlappedBlock));

    var result = validator.hasErrors(
        form,
        errors,
        ACTIVE_FEATURES,
        BLOCK_FEATURE,
        List.of(firstOverlappedBlock, secondOverlappedBlock, blockInAnotherCoordinateSystem)
    );

    assertThat(result).isTrue();
    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(errors))
        .containsOnly(entry(FIELD, List.of(
            "Block after surrender must not overlap licence block 30/2",
            "Block after surrender must not overlap licence block 30/3"
        )));
  }

  @Test
  void getOverlapErrors_whenAreasKeptInSeveralCoordinateSystems_thenAreasKeptInEachCheckedTogetherAgainstItsBlocks() {
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
    when(featureRelationshipService.get2dOverlappingFeatures(
        List.of(firstEd50AreaKept, secondEd50AreaKept),
        List.of(ed50Block)
    )).thenReturn(List.of(ed50Block));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(wgs84AreaKept), List.of(wgs84Block)))
        .thenReturn(List.of(wgs84Block));

    var result = validator.getOverlapErrors(
        List.of(firstEd50AreaKept, wgs84AreaKept, secondEd50AreaKept),
        List.of(ed50Block, wgs84Block)
    );

    assertThat(result).containsExactlyInAnyOrder(
        "Block after surrender must not overlap licence block 30/2",
        "Block after surrender must not overlap licence block 30/3"
    );
  }
}

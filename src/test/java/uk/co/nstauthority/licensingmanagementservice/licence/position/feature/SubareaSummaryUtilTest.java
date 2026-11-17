package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

class SubareaSummaryUtilTest {

  @Test
  void subareaCard_whenSubareaHasShapeAndEquity_thenShowsAreaAndSortedBeneficialInterests() {
    var feature = subareaFeature("A", 10_000_000);
    var organisationIdToEquity = Map.of(
        1, BigDecimal.valueOf(25),
        2, BigDecimal.valueOf(50),
        3, BigDecimal.valueOf(25)
    );

    var card = SubareaSummaryUtil.subareaCard(
        SubareaDetails.from(feature),
        organisationIdToEquity,
        "Licensees",
        Map.of(feature.getId(), feature),
        Map.of(1, "beta", 2, "Alpha")
    );

    assertThat(card).usingRecursiveComparison().isEqualTo(SummaryCard.simpleSummaryCardWithHeading(
        "Subarea A",
        SummaryDataView.newBuilder()
            .addStringValue("Area", "10.00 km²")
            .addStringValue("Licensees", List.of("Alpha (50%)", "beta (25%)", "Not available (25%)"))
            .build()
    ));
  }

  @Test
  void subareaCard_whenLegacySubareaHasNoEquity_thenNoRows() {
    var legacySubarea = new SubareaDetails(null, null, "E");

    var card = SubareaSummaryUtil.subareaCard(legacySubarea, Map.of(), "Licensees", Map.of(), Map.of());

    assertThat(card).usingRecursiveComparison().isEqualTo(SummaryCard.simpleSummaryCardWithHeading(
        "Subarea E",
        SummaryDataView.newBuilder().build()
    ));
  }

  @Test
  void toMapSummaryView_whenSomeSubareasHaveShapes_thenMapsOnlyThose() {
    var subarea = SubareaDetails.from(subareaFeature("A", 10_000_000));
    var legacySubarea = new SubareaDetails(null, null, "E");

    var map = SubareaSummaryUtil.toMapSummaryView(List.of(subarea, legacySubarea), 4230);

    assertThat(map).isEqualTo(new SummaryMapView(List.of(subarea.featureId()), 4230));
  }

  @Test
  void toMapSummaryView_whenNoSubareaHasShape_thenNull() {
    var legacySubarea = new SubareaDetails(null, null, "E");

    var map = SubareaSummaryUtil.toMapSummaryView(List.of(legacySubarea), 4230);

    assertThat(map).isNull();
  }

  @Test
  void getFeatureOrThrow_whenFeatureMissing_thenThrows() {
    var featureId = UUID.randomUUID();

    assertThatThrownBy(() -> SubareaSummaryUtil.getFeatureOrThrow(Map.of(), featureId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Subarea feature %s not found".formatted(featureId));
  }

  private static Feature subareaFeature(
      String name,
      long squareMetres
  ) {
    return FeatureTestUtil.builder()
        .withFeatureName(name)
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name(), "NAME", name))
        .withFeatureArea(BigDecimal.valueOf(squareMetres))
        .build();
  }
}

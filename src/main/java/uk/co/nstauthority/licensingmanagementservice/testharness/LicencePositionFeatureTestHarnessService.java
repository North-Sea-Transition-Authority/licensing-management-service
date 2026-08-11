package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;

@Service
@Profile("test-harness")
class LicencePositionFeatureTestHarnessService {

  private static final String QUADRANT_NUMBER = "30";
  private static final int BLOCKS_PER_POSITION = 2;
  private static final int SUBAREAS_PER_BLOCK = 2;
  private static final int SHAPES_PER_BLOCK = 1 + SUBAREAS_PER_BLOCK;

  private static final CoordinateSystem COORDINATE_SYSTEM = CoordinateSystem.ED50;
  private static final int RING_NUMBER = 1;

  // Nominal - the harness does not ask the node server to calculate the square's real area.
  private static final BigDecimal FEATURE_AREA = BigDecimal.valueOf(200000000);

  private static final String SOUTH_LINE =
      "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[2.8,53.8333333333333],[3.0,53.8333333333333]]]}";
  private static final String EAST_LINE =
      "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[3.0,53.8333333333333],[3.0,54.0]]]}";
  private static final String NORTH_LINE =
      "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[3.0,54.0],[2.8,54.0]]]}";
  private static final String WEST_LINE =
      "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[2.8,54.0],[2.8,53.8333333333333]]]}";

  private static final List<String> SQUARE_LINES = List.of(SOUTH_LINE, EAST_LINE, NORTH_LINE, WEST_LINE);

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;
  private final LicencePositionService licencePositionService;

  LicencePositionFeatureTestHarnessService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      LicencePositionService licencePositionService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.licencePositionService = licencePositionService;
  }

  public LicencePositionFeatureSeedState getSeedState(Licence licence) {
    var licencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    return new LicencePositionFeatureSeedState(
        licencePositions.size(),
        licencePositions.stream().anyMatch(licencePosition -> CollectionUtils.isNotEmpty(licencePosition.getFeatureIds()))
    );
  }

  /**
   * Creates and links the spatial data of every position on a licence - two blocks per position, and a
   * fixed number of subareas within each of those blocks. The features belong to the one position, so each
   * position holds its own blocks rather than inheriting those of the position before it.
   *
   * @return the number of features created
   */
  @Transactional
  public int createAndLinkFeatures(Licence licence) {
    Objects.requireNonNull(licence);

    var licencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var createdFeatureCount = 0;

    for (var positionIndex = 1; positionIndex <= licencePositions.size(); positionIndex++) {
      var features = createFeaturesForPosition(licence, positionIndex);

      licencePositionService.setFeatures(licencePositions.get(positionIndex - 1), features);
      createdFeatureCount += features.size();
    }

    return createdFeatureCount;
  }

  private List<Feature> createFeaturesForPosition(Licence licence, int positionIndex) {
    var features = new ArrayList<Feature>();

    for (var blockIndex = 1; blockIndex <= BLOCKS_PER_POSITION; blockIndex++) {
      var blockNumber = (positionIndex - 1) * BLOCKS_PER_POSITION + blockIndex;
      var shapeIndex = (blockIndex - 1) * SHAPES_PER_BLOCK + 1;

      var block = createFeature(licence, positionIndex, shapeIndex, blockAttributes(blockNumber), null);
      features.add(block);

      for (var subareaIndex = 1; subareaIndex <= SUBAREAS_PER_BLOCK; subareaIndex++) {
        var subarea = createFeature(
            licence,
            positionIndex,
            shapeIndex + subareaIndex,
            subareaAttributes(blockNumber, subareaIndex),
            block
        );
        features.add(subarea);
      }
    }

    return features;
  }

  /**
   * Creates the square as a feature, its single polygon and the four lines of that polygon's only ring.
   * None of this needs the node server - the lines are built here rather than by an ArcGIS operation.
   */
  private Feature createFeature(
      Licence licence,
      int positionIndex,
      int shapeIndex,
      Map<String, String> attributes,
      Feature parentFeature
  ) {
    var feature = new Feature();
    feature.setFeatureName("test harness for %s %s.%s".formatted(
        licence.getLicenceReference(),
        positionIndex,
        shapeIndex
    ));
    feature.setCoordinateSystem(COORDINATE_SYSTEM);
    feature.setFeatureArea(FEATURE_AREA);
    feature.setAttributes(attributes);
    feature.setParentFeature(parentFeature);
    feature.setActive(true);
    featureService.saveFeature(feature);

    var polygon = new Polygon();
    polygon.setFeature(feature);
    polygon.setAttributes(Map.of());
    polygonService.savePolygon(polygon);

    lineService.saveLines(squareLines(polygon));

    return feature;
  }

  private List<Line> squareLines(Polygon polygon) {
    return IntStream.rangeClosed(1, SQUARE_LINES.size())
        .mapToObj(displayOrder -> {
          var line = new Line();
          line.setPolygon(polygon);
          line.setEsriJson(SQUARE_LINES.get(displayOrder - 1));
          line.setNavigationType(LineNavigationType.LOXODROME);
          line.setRingNumber(RING_NUMBER);
          line.setDisplayOrder(displayOrder);
          line.setAttributes(Map.of());

          return line;
        })
        .toList();
  }

  private Map<String, String> blockAttributes(int blockNumber) {
    return Map.of(
        FeatureAttribute.LAYER.name(), Layer.BLOCKS.name(),
        FeatureAttribute.QUADRANT_NO.name(), QUADRANT_NUMBER,
        FeatureAttribute.BLOCK_NO.name(), String.valueOf(blockNumber)
    );
  }

  private Map<String, String> subareaAttributes(int blockNumber, int subareaIndex) {
    return Map.of(
        FeatureAttribute.LAYER.name(), Layer.SUBAREAS.name(),
        FeatureAttribute.NAME.name(), "%s/%s%s".formatted(QUADRANT_NUMBER, blockNumber, subareaSuffix(subareaIndex))
    );
  }

  private static String subareaSuffix(int subareaIndex) {
    return String.valueOf((char) ('a' + subareaIndex - 1));
  }
}

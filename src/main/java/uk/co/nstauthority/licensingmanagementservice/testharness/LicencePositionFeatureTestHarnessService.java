package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;

@Service
@Profile("test-harness")
class LicencePositionFeatureTestHarnessService {

  private static final String QUADRANT_NUMBER = "30";
  private static final int BLOCKS_PER_LICENCE = 4;
  private static final int SUBAREAS_PER_BLOCK = 2;
  private static final int SHAPES_PER_BLOCK = 1 + SUBAREAS_PER_BLOCK;
  private static final int FIRST_CHANGE_ORDER = 1;

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
  private final LicencePositionChangeService licencePositionChangeService;
  private final CommandJourneyService commandJourneyService;

  LicencePositionFeatureTestHarnessService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      CommandJourneyService commandJourneyService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.commandJourneyService = commandJourneyService;
  }

  public LicencePositionFeatureSeedState getSeedState(Licence licence) {
    var licencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    var hasSeededFeatures = licencePositionChangeService.findByLicencePositionIn(licencePositions)
        .stream()
        .map(change -> LicencePositionChangeOperationUtil.findOperation(change, PartialSurrenderOperation.class))
        .flatMap(Optional::stream)
        .anyMatch(partialSurrender -> !partialSurrender.outputFeatureIds().isEmpty());

    return new LicencePositionFeatureSeedState(licencePositions.size(), hasSeededFeatures);
  }

  /**
   * Creates the spatial data a licence holds and anchors it to the timeline with a spatial operation on the earliest
   * position, from which every later position derives its own features. That operation is a partial surrender because
   * it is the only spatial operation that exists so far - in time it will be a block create. A surrender has to
   * surrender something, so one extra block is created and fully surrendered by the seed operation, leaving the licence
   * holding the rest.
   *
   * @return the features created and the blocks the licence is left holding
   */
  @Transactional
  public SeededFeatures createAndLinkFeatures(Licence licence) {
    Objects.requireNonNull(licence);

    var licencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    if (licencePositions.isEmpty()) {
      return new SeededFeatures(List.of(), List.of());
    }

    var seededFeatures = createFeaturesForLicence(licence);
    var seedPosition = licencePositions.getFirst();

    licencePositionChangeService.createLicencePositionChange(
        seedPosition,
        List.of(seedSpatialOperation(seededFeatures.surrenderedBlock(), seededFeatures.retainedBlocks())),
        nextChangeOrder(seedPosition),
        LicencePositionChangeStatus.CONSENTED
    );

    return seededFeatures;
  }

  /**
   * The seed shares its position with whatever the rest of the harness has already put there, so it is appended rather
   * than assuming an order is free. Appending is safe spatially: the other seeded operations leave the features
   * untouched, so where the seed sits within the position makes no difference to what is derived from it.
   */
  private int nextChangeOrder(LicencePosition licencePosition) {
    var highestExistingOrder = licencePositionChangeService.findByLicencePositionId(licencePosition.getId())
        .stream()
        .mapToInt(LicencePositionChange::getChangeOrder)
        .max();

    return highestExistingOrder.isPresent() ? highestExistingOrder.getAsInt() + 1 : FIRST_CHANGE_ORDER;
  }

  private LicenceOperation seedSpatialOperation(Feature surrenderedBlock, List<Feature> retainedBlocks) {
    // a full surrender still carries a command journey (with no splits) so downstream processing is uniform
    var commandJourneyId = commandJourneyService.createAndAssignCommandJourney(List.of(surrenderedBlock)).getId();

    // no surrender date - the change takes the date of the position it sits on
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(surrenderedBlock.getId()))
        .withSurrenderDetails(Map.of(surrenderedBlock.getId(), new SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, commandJourneyId, List.of(surrenderedBlock.getId()))))
        .withOutputFeatureIds(retainedBlocks.stream().map(Feature::getId).toList())
        .build();
  }

  private SeededFeatures createFeaturesForLicence(Licence licence) {
    var blocks = new ArrayList<Feature>();
    var subareas = new ArrayList<Feature>();

    for (var blockIndex = 1; blockIndex <= BLOCKS_PER_LICENCE; blockIndex++) {
      var shapeIndex = (blockIndex - 1) * SHAPES_PER_BLOCK + 1;

      var block = createFeature(licence, shapeIndex, blockAttributes(blockIndex), null);
      blocks.add(block);

      for (var subareaIndex = 1; subareaIndex <= SUBAREAS_PER_BLOCK; subareaIndex++) {
        subareas.add(createFeature(
            licence,
            shapeIndex + subareaIndex,
            subareaAttributes(blockIndex, subareaIndex),
            block
        ));
      }
    }

    return new SeededFeatures(blocks, subareas);
  }

  /**
   * The blocks are kept apart from their subareas because only the blocks are named by the seed operation. The seed
   * operation fully surrenders the first block, so the licence is left holding the rest.
   */
  record SeededFeatures(List<Feature> blocks, List<Feature> subareas) {

    int count() {
      return blocks.size() + subareas.size();
    }

    Feature surrenderedBlock() {
      return blocks.getFirst();
    }

    List<Feature> retainedBlocks() {
      return blocks.stream().skip(1).toList();
    }
  }

  /**
   * Creates the square as a feature, its single polygon and the four lines of that polygon's only ring.
   * None of this needs the node server - the lines are built here rather than by an ArcGIS operation.
   */
  private Feature createFeature(
      Licence licence,
      int shapeIndex,
      Map<String, String> attributes,
      Feature parentFeature
  ) {
    var feature = new Feature();
    feature.setFeatureName("test harness for %s %s".formatted(licence.getLicenceReference(), shapeIndex));
    feature.setCoordinateSystem(COORDINATE_SYSTEM);
    feature.setFeatureArea(FEATURE_AREA);
    feature.setAttributes(attributes);
    feature.setParentFeature(parentFeature);
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

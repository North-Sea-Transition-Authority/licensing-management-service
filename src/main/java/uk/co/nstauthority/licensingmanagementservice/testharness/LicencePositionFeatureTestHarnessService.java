package uk.co.nstauthority.licensingmanagementservice.testharness;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.FeatureJourneyStateService;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.command.TransformationType;
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

  private static final String LINE_TEMPLATE =
      "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[%s,%s],[%s,%s]]]}";

  private static final String WESTERN_LONGITUDE = "2.8";
  private static final String MIDDLE_LONGITUDE = "2.9";
  private static final String EASTERN_LONGITUDE = "3.0";
  private static final String SOUTHERN_LATITUDE = "53.8333333333333";
  private static final String NORTHERN_LATITUDE = "54.0";

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final CommandJourneyService commandJourneyService;
  private final OperatorCommandService operatorCommandService;
  private final FeatureJourneyStateService featureJourneyStateService;

  LicencePositionFeatureTestHarnessService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      CommandJourneyService commandJourneyService,
      OperatorCommandService operatorCommandService,
      FeatureJourneyStateService featureJourneyStateService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.commandJourneyService = commandJourneyService;
    this.operatorCommandService = operatorCommandService;
    this.featureJourneyStateService = featureJourneyStateService;
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
   * Cuts a block down the middle, as drawing a split on the map would. The two halves become the journey's active
   * features and the block itself is deactivated, so the surrender journey's map and select-areas pages read back the
   * halves rather than the whole block.
   *
   * <p>The node server calculates neither the halves' geometry nor their area here - the block is a fixed square, so
   * both halves are known up front.</p>
   *
   * @return the halves of the block, the western one to be surrendered
   */
  @Transactional
  public SplitBlock splitBlockInHalf(CommandJourney commandJourney, Feature block) {
    var westernHalf = createHalfBlock(block, WESTERN_LONGITUDE, MIDDLE_LONGITUDE, 1);
    var easternHalf = createHalfBlock(block, MIDDLE_LONGITUDE, EASTERN_LONGITUDE, 2);

    var splitCommand = operatorCommandService.createOperatorCommand(
        commandJourney,
        Set.of(block.getId()),
        TransformationType.SPLIT
    );

    featureJourneyStateService.deactivateFeatures(commandJourney, List.of(block));
    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(
        commandJourney,
        splitCommand,
        List.of(westernHalf, easternHalf)
    );

    return new SplitBlock(westernHalf, easternHalf);
  }

  // a split output keeps the attributes and parent of the shape it came from, and is named after it
  private Feature createHalfBlock(
      Feature block,
      String westernLongitude,
      String easternLongitude,
      int splitPartNumber
  ) {
    return createShape(
        "%s_%s".formatted(block.getFeatureName(), splitPartNumber),
        Map.copyOf(block.getAttributes()),
        block.getParentFeature(),
        block.getFeatureArea().divide(BigDecimal.TWO),
        rectangleEdges(westernLongitude, easternLongitude)
    );
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

  record SplitBlock(Feature surrenderedHalf, Feature retainedHalf) {
  }

  private Feature createFeature(
      Licence licence,
      int shapeIndex,
      Map<String, String> attributes,
      Feature parentFeature
  ) {
    return createShape(
        "test harness for %s %s".formatted(licence.getLicenceReference(), shapeIndex),
        attributes,
        parentFeature,
        FEATURE_AREA,
        rectangleEdges(WESTERN_LONGITUDE, EASTERN_LONGITUDE)
    );
  }

  /**
   * Creates the shape as a feature, its single polygon and the four lines of that polygon's only ring.
   * None of this needs the node server - the lines are built here rather than by an ArcGIS operation.
   */
  private Feature createShape(
      String featureName,
      Map<String, String> attributes,
      @Nullable Feature parentFeature,
      BigDecimal featureArea,
      List<String> edges
  ) {
    var feature = new Feature();
    feature.setFeatureName(featureName);
    feature.setCoordinateSystem(COORDINATE_SYSTEM);
    feature.setFeatureArea(featureArea);
    feature.setAttributes(attributes);
    feature.setParentFeature(parentFeature);
    featureService.saveFeature(feature);

    var polygon = new Polygon();
    polygon.setFeature(feature);
    polygon.setAttributes(Map.of());
    polygonService.savePolygon(polygon);

    lineService.saveLines(lines(polygon, edges));

    return feature;
  }

  /**
   * The four edges of a rectangle spanning the two longitudes, clockwise from its southern edge. Every shape the
   * harness creates is one of these, so a block and either half of a split block differ only in their longitudes.
   */
  private static List<String> rectangleEdges(String westernLongitude, String easternLongitude) {
    return List.of(
        LINE_TEMPLATE.formatted(westernLongitude, SOUTHERN_LATITUDE, easternLongitude, SOUTHERN_LATITUDE),
        LINE_TEMPLATE.formatted(easternLongitude, SOUTHERN_LATITUDE, easternLongitude, NORTHERN_LATITUDE),
        LINE_TEMPLATE.formatted(easternLongitude, NORTHERN_LATITUDE, westernLongitude, NORTHERN_LATITUDE),
        LINE_TEMPLATE.formatted(westernLongitude, NORTHERN_LATITUDE, westernLongitude, SOUTHERN_LATITUDE)
    );
  }

  private List<Line> lines(Polygon polygon, List<String> edges) {
    return IntStream.rangeClosed(1, edges.size())
        .mapToObj(displayOrder -> {
          var line = new Line();
          line.setPolygon(polygon);
          line.setEsriJson(edges.get(displayOrder - 1));
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

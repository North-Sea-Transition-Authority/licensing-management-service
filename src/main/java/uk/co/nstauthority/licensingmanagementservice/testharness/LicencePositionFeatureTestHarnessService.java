package uk.co.nstauthority.licensingmanagementservice.testharness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourney;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeOperationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;

@Service
@Profile("test-harness")
class LicencePositionFeatureTestHarnessService {

  // no real block is in this quadrant, so it marks out the blocks the harness has seeded
  private static final String QUADRANT_NUMBER = "999";
  private static final String SUBAREA_NAME = "ALL";
  private static final String WESTERN_SUBAREA_NAME = "A";
  private static final String EASTERN_SUBAREA_NAME = "B";
  private static final int FIRST_CHANGE_ORDER = 1;

  private static final CoordinateSystem COORDINATE_SYSTEM = CoordinateSystem.ED50;
  private static final int RING_NUMBER = 1;

  // Nominal - the harness does not ask the node server to calculate the square's real area.
  private static final BigDecimal FEATURE_AREA = BigDecimal.valueOf(200000000);

  private static final String LINE_TEMPLATE =
      "{\"spatialReference\":{\"wkid\":4230},\"paths\":[[[%s,%s],[%s,%s]]]}";

  // Seeded blocks are laid out in rows from the south west, starting east of every real block so a seeded block never
  // overlaps one. Blocks are spaced further apart than their size so neighbouring blocks never share an edge.
  private static final int BLOCKS_PER_ROW = 10;
  private static final BigDecimal FIRST_BLOCK_WESTERN_LONGITUDE = new BigDecimal("3.6");
  private static final BigDecimal FIRST_BLOCK_SOUTHERN_LATITUDE = new BigDecimal("54.0");
  private static final BigDecimal BLOCK_WIDTH = new BigDecimal("0.2");
  private static final BigDecimal BLOCK_HEIGHT = new BigDecimal("0.1666666666667");
  private static final BigDecimal BLOCK_COLUMN_SPACING = new BigDecimal("0.3");
  private static final BigDecimal BLOCK_ROW_SPACING = new BigDecimal("0.25");

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final OperatorCommandService operatorCommandService;
  private final FeatureJourneyStateService featureJourneyStateService;

  LicencePositionFeatureTestHarnessService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      OperatorCommandService operatorCommandService,
      FeatureJourneyStateService featureJourneyStateService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.operatorCommandService = operatorCommandService;
    this.featureJourneyStateService = featureJourneyStateService;
  }

  public LicencePositionFeatureSeedState getSeedState(Licence licence) {
    var licencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    var hasSeededFeatures = licencePositionChangeService.findByLicencePositionIn(licencePositions)
        .stream()
        .map(change -> LicencePositionChangeOperationUtil.findOperation(change, BlockCreateOperation.class))
        .flatMap(Optional::stream)
        .anyMatch(blockCreate -> !blockCreate.createdBlockFeatureIds().isEmpty());

    return new LicencePositionFeatureSeedState(licencePositions.size(), hasSeededFeatures);
  }

  /**
   * Creates the spatial data a licence holds and anchors it to the timeline with a block create on the earliest
   * position, from which every later position derives its own features.
   *
   * <p>Every block is given the next free place after the blocks already seeded, so any number of licences can be
   * seeded without their blocks overlapping.</p>
   *
   * @return the blocks and subareas created
   */
  @Transactional
  public SeededFeatures createAndLinkFeatures(Licence licence) {
    Objects.requireNonNull(licence);

    var licencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    if (licencePositions.isEmpty()) {
      return new SeededFeatures(List.of(), Map.of());
    }

    var seededFeatures = createFeaturesForLicence(licence);
    var seedPosition = licencePositions.getFirst();

    licencePositionChangeService.createLicencePositionChange(
        seedPosition,
        List.of(seedSpatialOperation(seededFeatures)),
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

  private LicenceOperation seedSpatialOperation(SeededFeatures seededFeatures) {
    return LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(seededFeatures.blocks().stream().map(Feature::getId).toList())
        .withCreatedSubareas(seededFeatures.blockIdToSubareas().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(SeededFeatures::toSubareaDetails).toList()
            )))
        .build();
  }

  private SeededFeatures createFeaturesForLicence(Licence licence) {
    var featureLayout = FeatureLayout.of(licence.getType());
    var blocks = new ArrayList<Feature>();
    var blockIdToSubareas = new LinkedHashMap<UUID, List<Feature>>();
    var highestSeededBlockNumber = getHighestSeededBlockNumber();

    for (var blockIndex = 1; blockIndex <= featureLayout.blockCount(); blockIndex++) {
      var shapeIndex = (blockIndex - 1) * featureLayout.shapesPerBlock() + 1;
      var blockNumber = highestSeededBlockNumber + blockIndex;
      var blockShape = blockShape(blockNumber);

      var block = createFeature(licence, shapeIndex, blockAttributes(blockNumber), blockShape);
      blocks.add(block);

      // each subarea covers the whole of its block
      var subareas = new ArrayList<Feature>();
      for (var subareaIndex = 1; subareaIndex <= featureLayout.subareasPerBlock(); subareaIndex++) {
        subareas.add(createFeature(licence, shapeIndex + subareaIndex, subareaAttributes(), blockShape));
      }
      blockIdToSubareas.put(block.getId(), subareas);
    }

    return new SeededFeatures(blocks, blockIdToSubareas);
  }

  // the halves of a split block keep its number, so they never free up its place for another block
  private int getHighestSeededBlockNumber() {
    return featureService.findAllByAttribute(FeatureAttribute.QUADRANT_NO.name(), QUADRANT_NUMBER).stream()
        .filter(LicenceBlockFeatureUtil::isLicenceBlock)
        .mapToInt(block -> Integer.parseInt(block.getAttributes().get(FeatureAttribute.BLOCK_NO.name())))
        .max()
        .orElse(0);
  }

  /**
   * Cuts a block down the middle, as drawing a split on the map would. The two halves become the journey's active
   * features and the block itself is deactivated, so the surrender journey's map and select-areas pages read back the
   * halves rather than the whole block.
   *
   * <p>The node server calculates neither the halves' geometry nor their area here - the block is a fixed rectangle,
   * so both halves are known up front.</p>
   *
   * @return the halves of the block, the western one to be surrendered
   */
  @Transactional
  public SplitBlock splitBlockInHalf(CommandJourney commandJourney, Feature block) {
    var blockShape = blockShapeOf(block);
    var westernHalf = createHalfBlock(block, blockShape.westernHalf(), 1);
    var easternHalf = createHalfBlock(block, blockShape.easternHalf(), 2);

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

  /**
   * Cuts a subarea back to the half of its block that {@link #splitBlockInHalf} keeps, as a partial surrender of that
   * block would. The original subarea is ended and the cropped one starts on the date of the surrender, as applying the
   * surrender does.
   *
   * @param block the block the subarea covers
   * @return the cropped subarea
   */
  @Transactional
  public Feature cropSubareaToRetainedHalf(
      Feature subarea,
      Feature block,
      LocalDate surrenderDate
  ) {
    var croppedSubarea = createHalfBlock(subarea, blockShapeOf(block).easternHalf(), 2);
    croppedSubarea.setStartDate(surrenderDate);
    featureService.saveFeature(croppedSubarea);

    subarea.setEndDate(surrenderDate);
    featureService.saveFeature(subarea);

    return croppedSubarea;
  }

  /**
   * Cuts a subarea down the middle into two new subareas, as a subarea change splitting it would. The original subarea
   * is ended and both halves start on the date of the change, as applying the change does.
   *
   * @param block the block the subarea covers
   * @return the halves of the subarea, the western one first
   */
  @Transactional
  public List<Feature> splitSubareaInHalf(
      Feature subarea,
      Feature block,
      LocalDate changeDate
  ) {
    var blockShape = blockShapeOf(block);
    var westernHalf = createSubareaHalf(subarea, blockShape.westernHalf(), WESTERN_SUBAREA_NAME, changeDate);
    var easternHalf = createSubareaHalf(subarea, blockShape.easternHalf(), EASTERN_SUBAREA_NAME, changeDate);

    subarea.setEndDate(changeDate);
    featureService.saveFeature(subarea);

    return List.of(westernHalf, easternHalf);
  }

  // unlike a split block, each half of a split subarea is a subarea in its own right, so is given its own name
  private Feature createSubareaHalf(
      Feature subarea,
      Rectangle half,
      String subareaName,
      LocalDate startDate
  ) {
    var subareaHalf = createShape(
        "%s_%s".formatted(subarea.getFeatureName(), subareaName),
        subareaAttributes(subareaName),
        subarea.getFeatureArea().divide(BigDecimal.TWO),
        half.edges()
    );
    subareaHalf.setStartDate(startDate);
    featureService.saveFeature(subareaHalf);

    return subareaHalf;
  }

  // a split output keeps the attributes of the shape it came from, and is named after it
  private Feature createHalfBlock(
      Feature block,
      Rectangle half,
      int splitPartNumber
  ) {
    return createShape(
        "%s_%s".formatted(block.getFeatureName(), splitPartNumber),
        Map.copyOf(block.getAttributes()),
        block.getFeatureArea().divide(BigDecimal.TWO),
        half.edges()
    );
  }

  // a seeded block's number is its place in the layout
  private static Rectangle blockShapeOf(Feature block) {
    return blockShape(Integer.parseInt(block.getAttributes().get(FeatureAttribute.BLOCK_NO.name())));
  }

  private static Rectangle blockShape(int blockNumber) {
    var column = (blockNumber - 1) % BLOCKS_PER_ROW;
    var row = (blockNumber - 1) / BLOCKS_PER_ROW;
    var westernLongitude = FIRST_BLOCK_WESTERN_LONGITUDE.add(BLOCK_COLUMN_SPACING.multiply(BigDecimal.valueOf(column)));
    var southernLatitude = FIRST_BLOCK_SOUTHERN_LATITUDE.add(BLOCK_ROW_SPACING.multiply(BigDecimal.valueOf(row)));
    return new Rectangle(
        westernLongitude,
        westernLongitude.add(BLOCK_WIDTH),
        southernLatitude,
        southernLatitude.add(BLOCK_HEIGHT)
    );
  }

  private record Rectangle(
      BigDecimal westernLongitude,
      BigDecimal easternLongitude,
      BigDecimal southernLatitude,
      BigDecimal northernLatitude
  ) {

    Rectangle westernHalf() {
      return new Rectangle(westernLongitude, middleLongitude(), southernLatitude, northernLatitude);
    }

    Rectangle easternHalf() {
      return new Rectangle(middleLongitude(), easternLongitude, southernLatitude, northernLatitude);
    }

    private BigDecimal middleLongitude() {
      return westernLongitude.add(easternLongitude).divide(BigDecimal.TWO);
    }

    /**
     * The four edges of the rectangle, clockwise from its southern edge.
     */
    List<String> edges() {
      var western = coordinate(westernLongitude);
      var eastern = coordinate(easternLongitude);
      var southern = coordinate(southernLatitude);
      var northern = coordinate(northernLatitude);
      return List.of(
          LINE_TEMPLATE.formatted(western, southern, eastern, southern),
          LINE_TEMPLATE.formatted(eastern, southern, eastern, northern),
          LINE_TEMPLATE.formatted(eastern, northern, western, northern),
          LINE_TEMPLATE.formatted(western, northern, western, southern)
      );
    }

    private static String coordinate(BigDecimal degrees) {
      return degrees.stripTrailingZeros().toPlainString();
    }
  }

  /**
   * The blocks are kept apart from their subareas because the block create names the two separately.
   */
  record SeededFeatures(
      List<Feature> blocks,
      Map<UUID, List<Feature>> blockIdToSubareas
  ) {

    int count() {
      return blocks.size() + subareas().size();
    }

    List<Feature> subareas() {
      return blockIdToSubareas.values().stream()
          .flatMap(List::stream)
          .toList();
    }

    Feature subareaOf(Feature block) {
      return blockIdToSubareas.get(block.getId()).getFirst();
    }

    SubareaDetails subareaDetailsOf(Feature block) {
      return toSubareaDetails(subareaOf(block));
    }

    static SubareaDetails toSubareaDetails(Feature subarea) {
      var name = subarea.getAttributes().get(FeatureAttribute.NAME.name());
      return new SubareaDetails(subarea.getId(), name, name);
    }
  }

  record SplitBlock(Feature surrenderedHalf, Feature retainedHalf) {
  }

  private Feature createFeature(
      Licence licence,
      int shapeIndex,
      Map<String, String> attributes,
      Rectangle shape
  ) {
    return createShape(
        "test harness for %s %s".formatted(licence.getLicenceReference(), shapeIndex),
        attributes,
        FEATURE_AREA,
        shape.edges()
    );
  }

  /**
   * Creates the shape as a feature, its single polygon and the four lines of that polygon's only ring.
   * None of this needs the node server - the lines are built here rather than by an ArcGIS operation.
   */
  private Feature createShape(
      String featureName,
      Map<String, String> attributes,
      BigDecimal featureArea,
      List<String> edges
  ) {
    var feature = new Feature();
    feature.setFeatureName(featureName);
    feature.setCoordinateSystem(COORDINATE_SYSTEM);
    feature.setFeatureArea(featureArea);
    feature.setAttributes(attributes);
    featureService.saveFeature(feature);

    var polygon = new Polygon();
    polygon.setFeature(feature);
    polygon.setAttributes(Map.of());
    polygonService.savePolygon(polygon);

    lineService.saveLines(lines(polygon, edges));

    return feature;
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

  private Map<String, String> subareaAttributes() {
    return subareaAttributes(SUBAREA_NAME);
  }

  private Map<String, String> subareaAttributes(String subareaName) {
    return Map.of(
        FeatureAttribute.LAYER.name(), Layer.SUBAREAS.name(),
        FeatureAttribute.NAME.name(), subareaName
    );
  }

  private record FeatureLayout(int blockCount, int subareasPerBlock) {

    private static final FeatureLayout CARBON_STORAGE = new FeatureLayout(1, 0);
    private static final FeatureLayout BLOCKS_WITH_SUBAREAS = new FeatureLayout(3, 1);

    static FeatureLayout of(LicenceType licenceType) {
      return LicenceType.CARBON_STORAGE == licenceType ? CARBON_STORAGE : BLOCKS_WITH_SUBAREAS;
    }

    int shapesPerBlock() {
      return 1 + subareasPerBlock;
    }
  }
}

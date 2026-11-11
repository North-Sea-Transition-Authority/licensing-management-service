package uk.co.nstauthority.licensingmanagementservice.testharness;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;

@Service
@Profile("test-harness")
class LicencePositionFeatureTestHarnessService {

  // no real block is in this quadrant, so it marks out the blocks the harness has seeded
  private static final String QUADRANT_NUMBER = "999";
  // the licence a feature was seeded for, so the next run on that licence can clear it away
  private static final String HARNESS_LICENCE_ID_ATTRIBUTE = "TEST_HARNESS_LICENCE_ID";
  private static final String WHOLE_BLOCK_SUBAREA_NAME = "ALL";
  private static final String WESTERN_SUBAREA_NAME = "A";
  private static final String EASTERN_SUBAREA_NAME = "B";
  private static final String ABOVE_SUBAREA_NAME = "ABOVE";
  private static final String BELOW_SUBAREA_NAME = "BELOW";
  private static final String WESTERN_HALF_BLOCK_SUFFIX = "a";
  private static final String EASTERN_HALF_BLOCK_SUFFIX = "b";
  private static final int FIRST_CHANGE_ORDER = 1;

  private static final BigDecimal QUARTER = new BigDecimal("0.25");
  private static final BigDecimal HALF = new BigDecimal("0.5");
  private static final BigDecimal THREE_QUARTERS = new BigDecimal("0.75");

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

  private static final List<List<SubareaDivision>> CARBON_STORAGE_SUBAREA_DIVISIONS = List.of(List.of());

  private static final List<List<SubareaDivision>> WHOLE_BLOCK_SUBAREA_DIVISIONS = List.of(
      List.of(SubareaDivision.WHOLE_BLOCK),
      List.of(SubareaDivision.WHOLE_BLOCK),
      List.of(SubareaDivision.WHOLE_BLOCK)
  );

  // the second block is divided so that cutting it down the middle leaves a subarea either side of the cut and one
  // across it
  private static final List<List<SubareaDivision>> PRODUCTION_SUBAREA_DIVISIONS = List.of(
      List.of(SubareaDivision.WHOLE_BLOCK),
      List.of(
          new SubareaDivision("A", BigDecimal.ZERO, QUARTER),
          SubareaDivision.ACROSS_THE_MIDDLE,
          new SubareaDivision("C", THREE_QUARTERS, BigDecimal.ONE)
      ),
      List.of(SubareaDivision.WHOLE_BLOCK),
      List.of(SubareaDivision.WHOLE_BLOCK)
  );

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

  /**
   * Creates the spatial data a licence holds and anchors it to the timeline with a block create on the earliest
   * position, from which every later position derives its own features.
   *
   * <p>Every block is given the next free place after the blocks already seeded, so any number of licences can be
   * seeded without their blocks overlapping.</p>
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

  @Transactional
  public void clearFeatures(Licence licence) {
    featureService.deleteAll(
        featureService.findAllByAttribute(HARNESS_LICENCE_ID_ATTRIBUTE, harnessLicenceIdOf(licence))
    );
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
    var blocks = new ArrayList<Feature>();
    var blockIdToSubareas = new LinkedHashMap<UUID, List<Feature>>();
    var blockNumber = getHighestSeededBlockNumber();
    var harnessLicenceId = harnessLicenceIdOf(licence);

    for (var divisions : subareaDivisionsPerBlock(licence.getType())) {
      blockNumber++;
      var blockShape = blockShape(blockNumber);

      var block = createShape(
          blockFeatureName(blockNumber),
          blockAttributes(blockNumber, harnessLicenceId),
          FEATURE_AREA,
          blockShape
      );
      blocks.add(block);

      var subareas = new ArrayList<Feature>();
      for (var division : divisions) {
        subareas.add(createSubarea(
            division.name(),
            harnessLicenceId,
            areaOf(FEATURE_AREA, division),
            division.shapeWithin(blockShape)
        ));
      }
      blockIdToSubareas.put(block.getId(), subareas);
    }

    return new SeededFeatures(blocks, blockIdToSubareas);
  }

  private static List<List<SubareaDivision>> subareaDivisionsPerBlock(LicenceType licenceType) {
    if (LicenceType.CARBON_STORAGE == licenceType) {
      return CARBON_STORAGE_SUBAREA_DIVISIONS;
    }
    return licenceType.isProduction() ? PRODUCTION_SUBAREA_DIVISIONS : WHOLE_BLOCK_SUBAREA_DIVISIONS;
  }

  private static String harnessLicenceIdOf(Licence licence) {
    return String.valueOf(licence.getId());
  }

  private static String harnessLicenceIdOf(Feature feature) {
    return feature.getAttributes().get(HARNESS_LICENCE_ID_ATTRIBUTE);
  }

  private static String blockFeatureName(int blockNumber) {
    return "%s/%s".formatted(QUADRANT_NUMBER, blockNumber);
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
   */
  @Transactional
  public SplitBlock splitBlockInHalf(CommandJourney commandJourney, Feature block) {
    var blockShape = blockShapeOf(block);
    var westernHalf = createHalfBlock(block, blockShape.westernHalf(), WESTERN_HALF_BLOCK_SUFFIX);
    var easternHalf = createHalfBlock(block, blockShape.easternHalf(), EASTERN_HALF_BLOCK_SUFFIX);

    var splitCommand = operatorCommandService.createOperatorCommand(commandJourney, TransformationType.SPLIT);

    featureJourneyStateService.deactivateFeatures(commandJourney, List.of(block));
    featureJourneyStateService.createFeatureJourneyStatesForCommandOutput(
        commandJourney,
        splitCommand,
        Set.of(block.getId()),
        List.of(westernHalf, easternHalf)
    );

    return new SplitBlock(westernHalf, easternHalf);
  }

  /**
   * Cuts the subarea across the middle of its block back to its part within the half {@link #splitBlockInHalf} keeps,
   * as a partial surrender of that block would. The original subarea is ended and the cropped one starts on the date of
   * the surrender.
   */
  @Transactional
  public Feature cropSubareaAcrossTheMiddle(
      Feature subarea,
      Feature block,
      LocalDate surrenderDate
  ) {
    var croppedDivision = SubareaDivision.ACROSS_THE_MIDDLE.withinEasternHalf();
    var croppedSubarea = createShape(
        subarea.getFeatureName(),
        Map.copyOf(subarea.getAttributes()),
        areaOf(block.getFeatureArea(), croppedDivision),
        croppedDivision.shapeWithin(blockShapeOf(block))
    );

    startSubarea(croppedSubarea, surrenderDate);
    endSubarea(subarea, surrenderDate);

    return croppedSubarea;
  }

  @Transactional
  public void relinquishSubarea(
      Feature subarea,
      LocalDate surrenderDate
  ) {
    endSubarea(subarea, surrenderDate);
  }

  /**
   * Cuts a subarea down the middle into two new subareas, as a subarea change splitting it would. The original subarea
   * is ended and both halves, the western one first, start on the date of the change.
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

    endSubarea(subarea, changeDate);

    return List.of(westernHalf, easternHalf);
  }

  /**
   * Joins the subareas left on the half of a block that {@link #splitBlockInHalf} keeps into one subarea covering that
   * half, as a subarea change merging them would. The merged subareas are ended and the new one starts on the date of
   * the change.
   */
  @Transactional
  public Feature mergeSubareasOnRetainedHalf(
      List<Feature> subareas,
      Feature block,
      LocalDate changeDate
  ) {
    var mergedSubarea = createSubarea(
        subareas.stream().map(Feature::getFeatureName).collect(Collectors.joining()),
        harnessLicenceIdOf(subareas.getFirst()),
        subareas.stream().map(Feature::getFeatureArea).reduce(BigDecimal.ZERO, BigDecimal::add),
        blockShapeOf(block).easternHalf()
    );

    startSubarea(mergedSubarea, changeDate);
    subareas.forEach(subarea -> endSubarea(subarea, changeDate));

    return mergedSubarea;
  }

  /**
   * Splits a subarea at a depth into the strata above and below it, as a subarea change splitting its strata would.
   * Both strata keep the subarea's footprint, and so its area.
   * The original subarea is ended and both strata, the shallower one first, start on the date of the change.
   *
   * @param depth the depth of the boundary between the strata, in metres below the surface as a negative number
   */
  @Transactional
  public List<Feature> strataSplitSubarea(
      Feature subarea,
      Feature block,
      long depth,
      LocalDate changeDate
  ) {
    var blockShape = blockShapeOf(block);
    var shallowStratum = createSubarea(
        ABOVE_SUBAREA_NAME,
        harnessLicenceIdOf(subarea),
        subarea.getFeatureArea(),
        blockShape,
        null,
        depth
    );
    var deepStratum = createSubarea(
        BELOW_SUBAREA_NAME,
        harnessLicenceIdOf(subarea),
        subarea.getFeatureArea(),
        blockShape,
        depth,
        null
    );

    startSubarea(shallowStratum, changeDate);
    startSubarea(deepStratum, changeDate);
    endSubarea(subarea, changeDate);

    return List.of(shallowStratum, deepStratum);
  }

  private Feature createSubareaHalf(
      Feature subarea,
      Rectangle half,
      String subareaName,
      LocalDate startDate
  ) {
    var subareaHalf = createSubarea(
        subareaName,
        harnessLicenceIdOf(subarea),
        subarea.getFeatureArea().divide(BigDecimal.TWO),
        half
    );
    startSubarea(subareaHalf, startDate);

    return subareaHalf;
  }

  private void startSubarea(
      Feature subarea,
      LocalDate startDate
  ) {
    subarea.setStartDate(startDate);
    featureService.saveFeature(subarea);
  }

  private void endSubarea(
      Feature subarea,
      LocalDate endDate
  ) {
    subarea.setEndDate(endDate);
    featureService.saveFeature(subarea);
  }

  // Nominal, like the block's own area
  private static BigDecimal areaOf(
      BigDecimal blockArea,
      SubareaDivision division
  ) {
    return blockArea.multiply(division.share()).setScale(0, RoundingMode.HALF_UP);
  }

  // a split output keeps the attributes of the shape it came from, and is named after it with a suffix
  private Feature createHalfBlock(
      Feature block,
      Rectangle half,
      String suffix
  ) {
    return createShape(
        block.getFeatureName() + suffix,
        Map.copyOf(block.getAttributes()),
        block.getFeatureArea().divide(BigDecimal.TWO),
        half
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
      return slice(BigDecimal.ZERO, HALF);
    }

    Rectangle easternHalf() {
      return slice(HALF, BigDecimal.ONE);
    }

    /**
     * The part of the rectangle between two shares of its width, measured from its western edge.
     */
    Rectangle slice(
        BigDecimal westernShare,
        BigDecimal easternShare
    ) {
      return new Rectangle(longitudeAt(westernShare), longitudeAt(easternShare), southernLatitude, northernLatitude);
    }

    private BigDecimal longitudeAt(BigDecimal share) {
      return westernLongitude.add(easternLongitude.subtract(westernLongitude).multiply(share));
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
   * The part of its block a subarea covers, between two shares of the block's width measured from its western edge.
   */
  private record SubareaDivision(
      String name,
      BigDecimal westernShare,
      BigDecimal easternShare
  ) {

    static final SubareaDivision WHOLE_BLOCK = new SubareaDivision(WHOLE_BLOCK_SUBAREA_NAME, BigDecimal.ZERO, BigDecimal.ONE);
    static final SubareaDivision ACROSS_THE_MIDDLE = new SubareaDivision("B", QUARTER, THREE_QUARTERS);

    BigDecimal share() {
      return easternShare.subtract(westernShare);
    }

    SubareaDivision withinEasternHalf() {
      return new SubareaDivision(name, westernShare.max(HALF), easternShare);
    }

    private Rectangle shapeWithin(Rectangle blockShape) {
      return blockShape.slice(westernShare, easternShare);
    }
  }

  /**
   * The blocks are kept apart from their subareas because the block create names the two separately.
   */
  record SeededFeatures(
      List<Feature> blocks,
      Map<UUID, List<Feature>> blockIdToSubareas
  ) {

    List<Feature> subareasOf(Feature block) {
      return blockIdToSubareas.get(block.getId());
    }

    Feature subareaOf(Feature block) {
      return subareasOf(block).getFirst();
    }

    static SubareaDetails toSubareaDetails(Feature subarea) {
      var name = subarea.getAttributes().get(FeatureAttribute.NAME.name());
      return new SubareaDetails(subarea.getId(), name, name);
    }
  }

  record SplitBlock(Feature surrenderedHalf, Feature retainedHalf) {
  }

  private Feature createShape(
      String featureName,
      Map<String, String> attributes,
      BigDecimal featureArea,
      Rectangle shape
  ) {
    return createShape(featureName, attributes, featureArea, shape, null, null);
  }

  private Feature createSubarea(
      String subareaName,
      String harnessLicenceId,
      BigDecimal featureArea,
      Rectangle shape
  ) {
    return createSubarea(subareaName, harnessLicenceId, featureArea, shape, null, null);
  }

  private Feature createSubarea(
      String subareaName,
      String harnessLicenceId,
      BigDecimal featureArea,
      Rectangle shape,
      @Nullable Long startDepth,
      @Nullable Long endDepth
  ) {
    return createShape(
        subareaName,
        subareaAttributes(subareaName, harnessLicenceId),
        featureArea,
        shape,
        startDepth,
        endDepth
    );
  }

  /**
   * Creates the shape as a feature, its single polygon and the four lines of that polygon's only ring.
   * None of this needs the node server - the lines are built here rather than by an ArcGIS operation.
   *
   * @param startDepth the depth the polygon starts at, or null from the surface
   * @param endDepth   the depth the polygon ends at, or null for no lower limit
   */
  private Feature createShape(
      String featureName,
      Map<String, String> attributes,
      BigDecimal featureArea,
      Rectangle shape,
      @Nullable Long startDepth,
      @Nullable Long endDepth
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
    polygon.setStartDepth(startDepth);
    polygon.setEndDepth(endDepth);
    polygonService.savePolygon(polygon);

    lineService.saveLines(lines(polygon, shape.edges()));

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

  private Map<String, String> blockAttributes(
      int blockNumber,
      String harnessLicenceId
  ) {
    return Map.of(
        FeatureAttribute.LAYER.name(), Layer.BLOCKS.name(),
        FeatureAttribute.QUADRANT_NO.name(), QUADRANT_NUMBER,
        FeatureAttribute.BLOCK_NO.name(), String.valueOf(blockNumber),
        HARNESS_LICENCE_ID_ATTRIBUTE, harnessLicenceId
    );
  }

  private Map<String, String> subareaAttributes(
      String subareaName,
      String harnessLicenceId
  ) {
    return Map.of(
        FeatureAttribute.LAYER.name(), Layer.SUBAREAS.name(),
        FeatureAttribute.NAME.name(), subareaName,
        HARNESS_LICENCE_ID_ATTRIBUTE, harnessLicenceId
    );
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.grpc.gis.IntersectionStatus;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.formatting.DecimalFormatUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class PartialSurrenderBlockSummarySectionService
    implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String AREA = "Area";
  static final String LICENSEES = "Licensees";
  static final String BEFORE = "Before";
  static final String AFTER = "After";
  static final String CHANGE = "Change";
  static final String RELINQUISHED_WITH_BLOCK = "This subarea is relinquished as the whole block is surrendered.";
  static final String RELINQUISHED_OUTSIDE_AREA_KEPT = "This subarea is relinquished as it lies outside the area kept.";
  static final String KEPT_UNCHANGED = "This subarea is kept unchanged as it lies wholly inside the area kept.";
  static final int SECTION_ORDER = 20;

  private static final Comparator<SubareaSummary> SUBAREA_ORDER = Comparator
      .comparingInt((SubareaSummary subareaSummary) -> subareaSummary.outcome().getDisplayOrder())
      .thenComparing(subareaSummary -> subareaSummary.subarea().shortName());

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final FeatureService featureService;

  PartialSurrenderBlockSummarySectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      OrganisationUnitQueryService organisationUnitQueryService,
      FeatureService featureService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.featureService = featureService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    var surrender = context.surrender();

    if (surrender.surrenderedFeatureIds().isEmpty()) {
      return Optional.empty();
    }

    var surrenderedBlockFeatures = partialSurrenderCorrectionService.getSurrenderedBlockFeatures(surrender);
    var labelsById = LicenceBlockFeatureUtil.toBlockCheckboxOptions(surrenderedBlockFeatures);
    var featuresById = surrenderedBlockFeatures.stream()
        .collect(Collectors.toMap(Feature::getId, feature -> feature));
    var organisationNamesById = getOrganisationNamesById(context.stateBefore(), surrender.surrenderedFeatureIds());

    var blockItems = surrender.surrenderedFeatureIds().stream()
        .map(featureId -> {
          var feature = featuresById.get(featureId);
          if (feature == null) {
            throw new IllegalStateException(
                "Surrendered feature %s not found for %s"
                    .formatted(featureId, getSurrenderSource(context)));
          }
          return feature;
        })
        .sorted(LicenceBlockFeatureUtil.BLOCK_ORDER)
        .map(feature -> {
          var label = labelsById.get(feature.getId().toString());
          return getSummaryItemForBlock(context, feature, label, organisationNamesById);
        })
        .toList();
    return Optional.of(new SummarySection(SECTION_ORDER, blockItems));
  }

  private String getSurrenderSource(PartialSurrenderSummaryContext context) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged staged ->
          "correction %s".formatted(staged.licencePositionCorrection().getId());
      case PartialSurrenderSummaryContext.LiveChange liveChange -> "change %s".formatted(liveChange.changeId());
    };
  }

  private SummaryItem getSummaryItemForBlock(
      PartialSurrenderSummaryContext context,
      Feature block,
      String label,
      Map<Integer, String> organisationNamesById
  ) {
    var blockSurrender = context.surrender().featureIdToSurrenderDetails().get(block.getId());
    var srsWkid = CoordinateSystemUtils.getWkid(block.getCoordinateSystem());
    var changeUrl = getChangeUrl(context, block.getId(), blockSurrender);
    var changeAction = changeUrl == null ? null : new SummaryCardAction(CHANGE, changeUrl);
    var blockCard = getSummaryCardForBlock(block, label);
    var blockMap = new SummaryMapView(List.of(block.getId()), srsWkid);

    if (blockSurrender == null) {
      return SummaryItem.withRows(label, List.of(
          new SummaryRow(List.of(new SummaryColumn(BEFORE, blockMap, List.of(withChangeAction(blockCard, changeAction)))))
      ));
    }

    var rows = new ArrayList<SummaryRow>();
    rows.add(new SummaryRow(List.of(
        new SummaryColumn(BEFORE, blockMap, List.of(blockCard)),
        getAfterColumnForBlock(blockSurrender, label, srsWkid, changeAction)
    )));
    rows.addAll(getSubareaRows(context.stateBefore(), block, blockSurrender, organisationNamesById, srsWkid));

    var surrenderType = blockSurrender.type();
    return SummaryItem.withRows(label, rows)
        .withTag(new Tag(surrenderType.getDisplayName(), surrenderType.getTagColour()));
  }

  private SummaryCard getSummaryCardForBlock(Feature block, String label) {
    return SummaryCard.simpleSummaryCardWithHeading(
        label,
        SummaryDataView.newStringKeyValue(AREA, LicenceBlockFeatureUtil.formatArea(block.getFeatureArea()))
    );
  }

  /**
   * The "After" column for a surrendered block.
   *
   * @param changeAction   The card's change link, or null when the summary is read-only.
   */
  private SummaryColumn getAfterColumnForBlock(
      SurrenderDetails blockSurrender,
      String label,
      int srsWkid,
      @Nullable SummaryCardAction changeAction
  ) {
    var details = SummaryDataView.newBuilder();
    SummaryMapView map = null;

    if (blockSurrender.type() == BlockSurrenderType.FULL_SURRENDER) {
      details.addStringValue(AREA, LicenceBlockFeatureUtil.formatArea(BigDecimal.ZERO));
    } else {
      var retainedFeatureIds = blockSurrender.outputRetainedFeatureIds();

      if (!retainedFeatureIds.isEmpty()) {
        details.addStringValue(
            AREA,
            LicenceBlockFeatureUtil.formatArea(
                LicenceBlockFeatureUtil.getTotalArea(featureService.getFeaturesByIds(retainedFeatureIds))
            )
        );
        map = new SummaryMapView(retainedFeatureIds, srsWkid);
      }
    }

    var card = withChangeAction(SummaryCard.simpleSummaryCardWithHeading(label, details.build()), changeAction);
    return new SummaryColumn(AFTER, map, List.of(card));
  }

  private static SummaryCard withChangeAction(SummaryCard card, @Nullable SummaryCardAction changeAction) {
    return changeAction == null ? card : card.withAction(changeAction);
  }

  /**
   * The URL to change a block's surrender.
   *
   * @param blockSurrender The surrender details for the block, or null if it has none yet.
   * @return The change URL, or null when the live change is not being corrected and the summary is read-only.
   */
  @Nullable
  private String getChangeUrl(
      PartialSurrenderSummaryContext context,
      UUID featureId,
      @Nullable SurrenderDetails blockSurrender
  ) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection, var ignored, var ignoredState) -> {
        var correction = licencePositionCorrection.getLicenceCorrection();

        if (blockSurrender != null && blockSurrender.type() == BlockSurrenderType.PARTIAL_SURRENDER) {
          yield ReverseRouter.route(on(PartialSurrenderDefineAreaController.class).renderDefineArea(
              correction,
              licencePositionCorrection,
              featureId
          ));
        }
        yield ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeForm(
            correction,
            licencePositionCorrection,
            featureId
        ));
      }

      case PartialSurrenderSummaryContext.LiveChange(
          var correction, var licencePosition, var changeId, var ignored, var ignoredState) -> correction == null
          ? null
          : ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeFormForCorrectingChange(
              correction,
              licencePosition,
              new LicencePositionChange(UUID.fromString(changeId)),
              featureId
          ));
    };
  }

  private List<SummaryRow> getSubareaRows(
      LicencePositionState stateBefore,
      Feature block,
      SurrenderDetails blockSurrender,
      Map<Integer, String> organisationNamesById,
      int srsWkid
  ) {
    var subareaSummaries = blockSurrender.type() == BlockSurrenderType.FULL_SURRENDER
        ? getFullySurrenderedBlockSubareas(stateBefore, block)
        : getPartiallySurrenderedBlockSubareas(blockSurrender);

    if (subareaSummaries.isEmpty()) {
      return List.of();
    }

    var featuresById = getSubareaFeaturesById(subareaSummaries);
    var subareaEquity = stateBefore.blockFeatureIdToSubareaEquity().getOrDefault(block.getId(), Map.of());

    return subareaSummaries.stream()
        .sorted(SUBAREA_ORDER)
        .map(subareaSummary -> getSubareaRow(
            subareaSummary,
            blockSurrender.type(),
            subareaEquity.getOrDefault(subareaSummary.subarea(), Map.of()),
            featuresById,
            organisationNamesById,
            srsWkid
        ))
        .toList();
  }

  private List<SubareaSummary> getFullySurrenderedBlockSubareas(
      LicencePositionState stateBefore,
      Feature block
  ) {
    return stateBefore.subareasOf(block.getId()).stream()
        .map(subarea -> new SubareaSummary(subarea, PartialSurrenderSubareaOutcome.RELINQUISHED, List.of()))
        .toList();
  }

  private List<SubareaSummary> getPartiallySurrenderedBlockSubareas(SurrenderDetails blockSurrender) {
    var subareaToOutcomes = blockSurrender.retainedFeatureIdToSubareas().values().stream()
        .flatMap(List::stream)
        .collect(Collectors.groupingBy(
            SubareaSurrenderOutcome::subarea,
            LinkedHashMap::new,
            Collectors.toList()
        ));

    return subareaToOutcomes.entrySet().stream()
        .map(entry -> toSubareaSummary(entry.getKey(), entry.getValue()))
        .toList();
  }

  private SubareaSummary toSubareaSummary(
      SubareaDetails subarea,
      List<SubareaSurrenderOutcome> outcomes
  ) {
    var croppedSubareas = outcomes.stream()
        .map(SubareaSurrenderOutcome::croppedSubarea)
        .filter(Objects::nonNull)
        .toList();

    if (!croppedSubareas.isEmpty()) {
      return new SubareaSummary(subarea, PartialSurrenderSubareaOutcome.CROPPED, croppedSubareas);
    }

    var isKept = outcomes.stream()
        .anyMatch(outcome -> outcome.status() == IntersectionStatus.FULLY_INSIDE);
    var outcome = isKept ? PartialSurrenderSubareaOutcome.UNCHANGED : PartialSurrenderSubareaOutcome.RELINQUISHED;

    return new SubareaSummary(subarea, outcome, List.of());
  }

  private Map<UUID, Feature> getSubareaFeaturesById(List<SubareaSummary> subareaSummaries) {
    var featureIds = subareaSummaries.stream()
        .flatMap(subareaSummary -> Stream.concat(
            Stream.ofNullable(subareaSummary.subarea().featureId()),
            subareaSummary.croppedSubareas().stream().map(SubareaDetails::featureId)
        ))
        .collect(Collectors.toSet());

    return featureService.getFeaturesByIds(featureIds).stream()
        .collect(Collectors.toMap(Feature::getId, Function.identity()));
  }

  private String getOutcomeExplanation(PartialSurrenderSubareaOutcome outcome, BlockSurrenderType surrenderType) {
    if (outcome == PartialSurrenderSubareaOutcome.UNCHANGED) {
      return KEPT_UNCHANGED;
    }
    return surrenderType == BlockSurrenderType.FULL_SURRENDER
        ? RELINQUISHED_WITH_BLOCK
        : RELINQUISHED_OUTSIDE_AREA_KEPT;
  }

  private SummaryRow getSubareaRow(
      SubareaSummary subareaSummary,
      BlockSurrenderType surrenderType,
      Map<Integer, BigDecimal> equityByOrganisationId,
      Map<UUID, Feature> featuresById,
      Map<Integer, String> organisationNamesById,
      int srsWkid
  ) {
    var subarea = subareaSummary.subarea();
    var outcome = subareaSummary.outcome();
    var tag = new Tag(outcome.getDisplayName(), outcome.getTagColour());

    var before = new SummaryColumn(
        BEFORE,
        subarea.featureId() != null ? toSubareaMapView(List.of(subarea), srsWkid) : null,
        List.of(SummaryCard.simpleSummaryCardWithHeading(
            getSubareaHeading(subarea),
            getSubareaDetails(subarea, equityByOrganisationId, featuresById, organisationNamesById)))
    );

    if (outcome != PartialSurrenderSubareaOutcome.CROPPED) {
      var after = new SummaryColumn(AFTER, null, List.of(), getOutcomeExplanation(outcome, surrenderType));
      return new SummaryRow(getSubareaHeading(subarea), tag, List.of(before, after));
    }

    var croppedSubareas = subareaSummary.croppedSubareas().stream()
        .sorted(Comparator.comparing(
            croppedSubarea -> getSubareaFeatureOrThrow(featuresById, croppedSubarea.featureId()).getFeatureName()))
        .toList();

    var after = new SummaryColumn(
        AFTER,
        toSubareaMapView(croppedSubareas, srsWkid),
        croppedSubareas.stream()
            .map(croppedSubarea -> SummaryCard.simpleSummaryCardWithHeading(
                getSubareaHeading(croppedSubarea),
                // TODO LMS2-414: cascade subarea beneficial interests
                getSubareaDetails(croppedSubarea, Map.of(), featuresById, organisationNamesById)
            ))
            .toList()
    );

    return new SummaryRow(getSubareaHeading(subarea), tag, List.of(before, after));
  }

  private String getSubareaHeading(SubareaDetails subarea) {
    return "Subarea %s".formatted(StringUtils.isBlank(subarea.name()) ? subarea.shortName() : subarea.name());
  }

  private SummaryMapView toSubareaMapView(List<SubareaDetails> subareas, int srsWkid) {
    return new SummaryMapView(subareas.stream().map(SubareaDetails::featureId).toList(), srsWkid);
  }

  private SummaryDataView getSubareaDetails(
      SubareaDetails subarea,
      Map<Integer, BigDecimal> equityByOrganisationId,
      Map<UUID, Feature> featuresById,
      Map<Integer, String> organisationNamesById
  ) {
    var details = SummaryDataView.newBuilder();

    // legacy subareas may have no shape to show
    if (subarea.featureId() != null) {
      var subareaFeature = getSubareaFeatureOrThrow(featuresById, subarea.featureId());
      details.addStringValue(AREA, LicenceBlockFeatureUtil.formatArea(subareaFeature.getFeatureArea()));
    }

    if (!equityByOrganisationId.isEmpty()) {
      details.addStringValue(LICENSEES, formatLicensees(equityByOrganisationId, organisationNamesById));
    }

    return details.build();
  }

  private List<String> formatLicensees(
      Map<Integer, BigDecimal> equityByOrganisationId,
      Map<Integer, String> organisationNamesById
  ) {
    return equityByOrganisationId.entrySet().stream()
        .map(entry -> "%s (%s%%)".formatted(
            organisationNamesById.getOrDefault(entry.getKey(), NOT_AVAILABLE),
            DecimalFormatUtils.bigDecimalToFormattedString(entry.getValue())
        ))
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  private Map<Integer, String> getOrganisationNamesById(
      LicencePositionState stateBefore,
      Collection<UUID> blockFeatureIds
  ) {
    var organisationIds = blockFeatureIds.stream()
        .map(blockFeatureId -> stateBefore.blockFeatureIdToSubareaEquity().getOrDefault(blockFeatureId, Map.of()))
        .flatMap(subareaEquity -> subareaEquity.values().stream())
        .flatMap(equityByOrganisationId -> equityByOrganisationId.keySet().stream())
        .distinct()
        .toList();

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds);
  }

  private Feature getSubareaFeatureOrThrow(
      Map<UUID, Feature> featuresById,
      UUID featureId
  ) {
    var feature = featuresById.get(featureId);
    if (feature == null) {
      throw new IllegalStateException("Subarea feature %s not found".formatted(featureId));
    }
    return feature;
  }

  private record SubareaSummary(
      SubareaDetails subarea,
      PartialSurrenderSubareaOutcome outcome,
      List<SubareaDetails> croppedSubareas
  ) {
  }
}

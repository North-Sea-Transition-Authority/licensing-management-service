package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.grpc.gis.IntersectionStatus;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class PartialSurrenderBlockSummarySectionService
    implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String TYPE_OF_CHANGE = "Type of surrender";
  static final String OUTCOME = "Outcome";
  static final String AREA_BEFORE = "Area before";
  static final String AREA_AFTER = "Area after";
  static final String BEFORE = "Before";
  static final String AFTER = "After";
  static final String UNLABELLED_MAP = "";
  static final String CHANGE = "Change";
  static final int SECTION_ORDER = 20;

  private static final Comparator<SubareaSummary> SUBAREA_ORDER = Comparator
      .comparingInt((SubareaSummary subareaSummary) -> subareaSummary.outcome().getDisplayOrder())
      .thenComparing(subareaSummary -> subareaSummary.subarea().shortName());

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final FeatureService featureService;

  PartialSurrenderBlockSummarySectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService,
      FeatureService featureService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.featureService = featureService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection) ->
          partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection)
              .flatMap(surrender -> getSummarySection(
                  context,
                  surrender,
                  "correction %s".formatted(licencePositionCorrection.getId())));
      case PartialSurrenderSummaryContext.LiveChange(var correction, var licencePosition, var changeId) ->
          getSummarySection(
              context,
              partialSurrenderCorrectionService
                  .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId),
              "change %s".formatted(changeId));
    };
  }

  private Optional<SummarySection> getSummarySection(
      PartialSurrenderSummaryContext context,
      PartialSurrenderOperation surrender,
      String surrenderSource
  ) {
    if (surrender.surrenderedFeatureIds().isEmpty()) {
      return Optional.empty();
    }

    var surrenderedBlockFeatures = partialSurrenderCorrectionService.getSurrenderedBlockFeatures(surrender);
    var labelsById = LicenceBlockFeatureUtil.toBlockCheckboxOptions(surrenderedBlockFeatures);
    var featuresById = surrenderedBlockFeatures.stream()
        .collect(Collectors.toMap(Feature::getId, feature -> feature));

    var blockItems = surrender.surrenderedFeatureIds().stream()
        .map(featureId -> {
          var feature = featuresById.get(featureId);
          if (feature == null) {
            throw new IllegalStateException(
                "Surrendered feature %s not found for %s"
                    .formatted(featureId, surrenderSource));
          }
          return feature;
        })
        .sorted(LicenceBlockFeatureUtil.BLOCK_ORDER)
        .map(feature -> {
          var label = labelsById.get(feature.getId().toString());
          return SummaryItem.withCards(label, getSummaryCardsForBlock(context, surrender, feature, label));
        })
        .toList();
    return Optional.of(new SummarySection(SECTION_ORDER, blockItems));
  }

  private List<SummaryCard> getSummaryCardsForBlock(
      PartialSurrenderSummaryContext context,
      PartialSurrenderOperation surrender,
      Feature block,
      String label
  ) {
    var blockSurrender = surrender.featureIdToSurrenderDetails().get(block.getId());

    var cards = new ArrayList<SummaryCard>();
    cards.add(getSummaryCardForBlock(block, blockSurrender, label)
        .withAction(new SummaryCardAction(CHANGE, getChangeUrl(context, block.getId(), blockSurrender))));

    if (blockSurrender != null) {
      cards.addAll(getSubareaCards(context, block, blockSurrender));
    }
    return cards;
  }

  private SummaryCard getSummaryCardForBlock(
      Feature block,
      @Nullable SurrenderDetails blockSurrender,
      String label
  ) {
    var details = SummaryDataView.newBuilder()
        .addStringValue(
            TYPE_OF_CHANGE,
            blockSurrender != null ? blockSurrender.type().getDisplayName() : null
        );

    if (blockSurrender != null) {
      var srsWkid = CoordinateSystemUtils.getWkid(block.getCoordinateSystem());
      var wholeBlock = new SummaryMapView(List.of(block.getId()), srsWkid);

      details.addStringValue(AREA_BEFORE, formatArea(block.getFeatureArea()));

      if (blockSurrender.type() == BlockSurrenderType.FULL_SURRENDER) {
        details
            .addStringValue(AREA_AFTER, formatArea(BigDecimal.ZERO))
            .addMapValue(UNLABELLED_MAP, wholeBlock);
      } else {
        var retainedFeatureIds = partialSurrenderCorrectionService.getRetainedFeatureIds(blockSurrender);

        if (retainedFeatureIds.isEmpty()) {
          details.addMapValue(BEFORE, wholeBlock);
        } else {
          details
              .addStringValue(AREA_AFTER, formatArea(getTotalArea(featureService.getFeaturesByIds(retainedFeatureIds))))
              .addMapValue(BEFORE, wholeBlock)
              .addMapValue(AFTER, new SummaryMapView(retainedFeatureIds, srsWkid));
        }
      }
    }

    return SummaryCard.simpleSummaryCardWithHeading(label, details.build());
  }

  private String getChangeUrl(
      PartialSurrenderSummaryContext context,
      UUID featureId,
      @Nullable SurrenderDetails blockSurrender
  ) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection) -> {
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

      case PartialSurrenderSummaryContext.LiveChange(var correction, var licencePosition, var changeId) ->
          ReverseRouter.route(on(BlockSurrenderTypeController.class).renderSurrenderTypeFormForCorrectingChange(
              correction,
              licencePosition,
              new LicencePositionChange(UUID.fromString(changeId)),
              featureId
          ));
    };
  }

  private List<SummaryCard> getSubareaCards(
      PartialSurrenderSummaryContext context,
      Feature block,
      SurrenderDetails blockSurrender
  ) {
    var subareaSummaries = blockSurrender.type() == BlockSurrenderType.FULL_SURRENDER
        ? getFullySurrenderedBlockSubareas(context, block)
        : getPartiallySurrenderedBlockSubareas(blockSurrender);

    if (subareaSummaries.isEmpty()) {
      return List.of();
    }

    var featuresById = getSubareaFeaturesById(subareaSummaries);
    var srsWkid = CoordinateSystemUtils.getWkid(block.getCoordinateSystem());

    return subareaSummaries.stream()
        .sorted(SUBAREA_ORDER)
        .map(subareaSummary -> getSubareaCard(subareaSummary, featuresById, srsWkid))
        .toList();
  }

  private List<SubareaSummary> getFullySurrenderedBlockSubareas(
      PartialSurrenderSummaryContext context,
      Feature block
  ) {
    var subareas = switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection) ->
          licencePositionSpatialService.getSubareasGoingIntoChange(
              licencePositionCorrection,
              block.getId(),
              partialSurrenderCorrectionService
                  .getCommittedPartialSurrenderChangeId(licencePositionCorrection)
                  .orElse(null)
          );
      case PartialSurrenderSummaryContext.LiveChange(var correction, var licencePosition, var changeId) ->
          licencePositionSpatialService.getSubareasGoingIntoChange(
              correction,
              licencePosition.getId(),
              block.getId(),
              changeId
          );
    };

    return subareas.stream()
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
    var croppedFeatureIds = outcomes.stream()
        .map(SubareaSurrenderOutcome::croppedSubarea)
        .filter(Objects::nonNull)
        .map(SubareaDetails::featureId)
        .toList();

    if (!croppedFeatureIds.isEmpty()) {
      return new SubareaSummary(subarea, PartialSurrenderSubareaOutcome.CROPPED, croppedFeatureIds);
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
            subareaSummary.croppedFeatureIds().stream()
        ))
        .collect(Collectors.toSet());

    return featureService.getFeaturesByIds(featureIds).stream()
        .collect(Collectors.toMap(Feature::getId, Function.identity()));
  }

  private SummaryCard getSubareaCard(
      SubareaSummary subareaSummary,
      Map<UUID, Feature> featuresById,
      int srsWkid
  ) {
    var subarea = subareaSummary.subarea();
    var details = SummaryDataView.newBuilder()
        .addStringValue(OUTCOME, subareaSummary.outcome().getDisplayName());

    // legacy subareas may have no shape to show
    if (subarea.featureId() != null) {
      var subareaFeature = getSubareaFeatureOrThrow(featuresById, subarea.featureId());
      var subareaMap = new SummaryMapView(List.of(subarea.featureId()), srsWkid);
      var croppedFeatures = subareaSummary.croppedFeatureIds().stream()
          .map(featureId -> getSubareaFeatureOrThrow(featuresById, featureId))
          .sorted(Comparator.comparing(Feature::getFeatureName))
          .toList();

      var areaAfter = switch (subareaSummary.outcome()) {
        case RELINQUISHED -> BigDecimal.ZERO;
        case CROPPED -> getTotalArea(croppedFeatures);
        case UNCHANGED -> subareaFeature.getFeatureArea();
      };

      details
          .addStringValue(AREA_BEFORE, formatArea(subareaFeature.getFeatureArea()))
          .addStringValue(AREA_AFTER, formatArea(areaAfter));

      if (subareaSummary.outcome() == PartialSurrenderSubareaOutcome.CROPPED) {
        details
            .addMapValue(BEFORE, subareaMap)
            .addMapValue(AFTER, new SummaryMapView(croppedFeatures.stream().map(Feature::getId).toList(), srsWkid));
      } else {
        details.addMapValue(UNLABELLED_MAP, subareaMap);
      }
    }

    var heading = subarea.name() != null ? subarea.name() : "Subarea %s".formatted(subarea.shortName());
    return SummaryCard.simpleSummaryCardWithHeading(heading, details.build());
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

  private BigDecimal getTotalArea(Collection<Feature> features) {
    return features.stream()
        .map(Feature::getFeatureArea)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // feature areas are stored in square metres
  private String formatArea(BigDecimal squareMetres) {
    var squareKilometres = squareMetres.movePointLeft(6).setScale(2, RoundingMode.HALF_UP);
    return String.format(Locale.UK, "%,.2f km²", squareKilometres);
  }

  private record SubareaSummary(
      SubareaDetails subarea,
      PartialSurrenderSubareaOutcome outcome,
      List<UUID> croppedFeatureIds
  ) {
  }
}

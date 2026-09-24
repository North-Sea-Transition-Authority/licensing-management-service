package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class PartialSurrenderBlockSummarySectionService
    implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String TYPE_OF_CHANGE = "Type of surrender";
  static final String BEFORE = "Before";
  static final String AFTER = "After";
  static final String UNLABELLED_MAP = "";
  static final int SECTION_ORDER = 20;

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;

  public PartialSurrenderBlockSummarySectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection) -> {
        var stagedChangeId = partialSurrenderCorrectionService
            .getCommittedPartialSurrenderChangeId(licencePositionCorrection)
            .orElse(null);
        yield partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection)
            .flatMap(surrender -> getSummarySection(
                surrender,
                licencePositionSpatialService.getBlockFeaturesGoingIntoChange(licencePositionCorrection, stagedChangeId),
                "correction %s".formatted(licencePositionCorrection.getId())));
      }
      case PartialSurrenderSummaryContext.LiveChange(var correction, var licencePosition, var changeId) ->
          getSummarySection(
              partialSurrenderCorrectionService
                  .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId),
              licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, changeId),
              "change %s".formatted(changeId));
    };
  }

  private Optional<SummarySection> getSummarySection(
      PartialSurrenderOperation surrender,
      List<Feature> surrenderableBlockFeatures,
      String surrenderSource
  ) {
    if (surrender.surrenderedFeatureIds().isEmpty()) {
      return Optional.empty();
    }

    var labelsById = LicenceBlockFeatureUtil.toBlockCheckboxOptions(
        surrenderableBlockFeatures
    );
    var featuresById = surrenderableBlockFeatures.stream()
        .collect(Collectors.toMap(Feature::getId, feature -> feature));

    var blockItems = surrender.surrenderedFeatureIds().stream()
        .map(featureId -> {
          var feature = featuresById.get(featureId);
          if (feature == null) {
            throw new IllegalStateException(
                "Surrendered feature %s not resolvable as a surrenderable block on %s"
                    .formatted(featureId, surrenderSource));
          }
          return feature;
        })
        .sorted(LicenceBlockFeatureUtil.BLOCK_ORDER)
        .map(feature -> SummaryItem.withCard(
            labelsById.get(feature.getId().toString()),
            getSummaryCardForBlock(surrender, feature)))
        .toList();
    return Optional.of(new SummarySection(SECTION_ORDER, blockItems));
  }

  private SummaryCard getSummaryCardForBlock(PartialSurrenderOperation surrender, Feature block) {
    var blockSurrender = surrender.featureIdToSurrenderDetails().get(block.getId());

    var details = SummaryDataView.newBuilder()
        .addStringValue(
            TYPE_OF_CHANGE,
            blockSurrender != null ? blockSurrender.type().getDisplayName() : null
        );

    if (blockSurrender != null) {
      var srsWkid = CoordinateSystemUtils.getWkid(block.getCoordinateSystem());
      var wholeBlock = new SummaryMapView(List.of(block.getId()), srsWkid);

      if (blockSurrender.type() == BlockSurrenderType.FULL_SURRENDER) {
        details.addMapValue(UNLABELLED_MAP, wholeBlock);
      } else {
        details.addMapValue(BEFORE, wholeBlock);

        var retainedFeatureIds = partialSurrenderCorrectionService.getRetainedFeatureIds(blockSurrender);
        if (!retainedFeatureIds.isEmpty()) {
          details.addMapValue(AFTER, new SummaryMapView(retainedFeatureIds, srsWkid));
        }
      }
    }

    return SummaryCard.simpleSummaryCard(details.build());
  }
}

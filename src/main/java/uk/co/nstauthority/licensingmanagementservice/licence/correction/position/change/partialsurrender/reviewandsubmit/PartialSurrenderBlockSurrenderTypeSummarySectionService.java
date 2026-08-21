package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class PartialSurrenderBlockSurrenderTypeSummarySectionService
    implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String TYPE_OF_CHANGE = "Type of surrender";
  static final int SECTION_ORDER = 20;

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public PartialSurrenderBlockSurrenderTypeSummarySectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection) ->
          partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection)
              .flatMap(surrender -> getSummarySection(
                  surrender,
                  partialSurrenderCorrectionService.getSurrenderableBlockFeatures(licencePositionCorrection),
                  "correction %s".formatted(licencePositionCorrection.getId())));
      case PartialSurrenderSummaryContext.LiveChange(var correction, var licencePosition, var changeId) ->
          getSummarySection(
              partialSurrenderCorrectionService
                  .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId),
              partialSurrenderCorrectionService.getSurrenderableBlockFeatures(licencePosition),
              "change %s".formatted(changeId));
    };
  }

  private Optional<SummarySection> getSummarySection(
      PartialSurrenderOperation surrender,
      List<Feature> surrenderableBlockFeatures,
      String surrenderSource
  ) {
    if (surrender.featureIds().isEmpty()) {
      return Optional.empty();
    }

    var labelsById = LicenceBlockFeatureUtil.toBlockCheckboxOptions(
        surrenderableBlockFeatures
    );
    var featuresById = surrenderableBlockFeatures.stream()
        .collect(Collectors.toMap(Feature::getId, feature -> feature));

    var blockItems = surrender.featureIds().stream()
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
            blockSurrenderTypeCard(surrender, feature.getId())))
        .toList();
    return Optional.of(new SummarySection(SECTION_ORDER, blockItems));
  }

  private SummaryCard blockSurrenderTypeCard(PartialSurrenderOperation surrender, UUID featureId) {
    var surrenderType = surrender.blockSurrenderTypeByFeatureId().get(featureId);
    return SummaryCard.simpleSummaryCard(
        SummaryDataView.newStringKeyValue(
            TYPE_OF_CHANGE,
            surrenderType != null ? surrenderType.getDisplayName() : null
        )
    );
  }
}

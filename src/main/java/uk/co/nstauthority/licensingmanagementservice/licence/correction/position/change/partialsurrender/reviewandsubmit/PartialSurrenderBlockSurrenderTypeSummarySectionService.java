package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

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
    var licencePositionCorrection = context.licencePositionCorrection();
    var surrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection).orElse(null);

    if (surrender == null || surrender.featureIds().isEmpty()) {
      return Optional.empty();
    }

    var surrenderableBlockFeatures = partialSurrenderCorrectionService.getSurrenderableBlockFeatures(licencePositionCorrection);
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
                "Surrendered feature %s not resolvable as a surrenderable block on correction %s"
                    .formatted(featureId, licencePositionCorrection.getId()));
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

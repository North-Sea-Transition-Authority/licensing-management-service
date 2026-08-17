package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class PartialSurrenderDetailsSummarySectionService implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String SURRENDER_DETAILS = "Surrender details";
  static final int SECTION_ORDER = 10;

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public PartialSurrenderDetailsSummarySectionService(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    var licencePositionCorrection = context.licencePositionCorrection();

    var surrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection);

    if (surrender.isEmpty()) {
      return Optional.empty();
    }

    var surrenderDate = licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection);
    var labelsById = LicenceBlockFeatureUtil.toBlockCheckboxOptions(
        partialSurrenderCorrectionService.getSurrenderableBlockFeatures(licencePositionCorrection)
    );
    var surrenderedBlocks = surrender.get().featureIds().stream()
        .map(id -> {
          var label = labelsById.get(id.toString());
          if (label == null) {
            throw new IllegalStateException(
                "Surrendered feature %s not resolvable as a surrenderable block on correction %s"
                    .formatted(id, licencePositionCorrection.getId()));
          }
          return label;
        })
        .toList();
    var summaryCard = SummaryCard.simpleSummaryCard(
        SummaryDataView.newBuilder()
            .addStringValue("Date of surrender", DateUtil.formatLongDate(surrenderDate))
            .addStringValue("Blocks surrendered", surrenderedBlocks)
            .build()
    );

    return Optional.of(new SummarySection(SECTION_ORDER, List.of(SummaryItem.withCard(SURRENDER_DETAILS, summaryCard))));
  }
}

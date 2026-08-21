package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
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
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection) ->
          partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection)
              .map(surrender -> getSummarySection(
                  licencePositionCorrection.getLicenceCorrection().getLicence().getLicenceReference(),
                  licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection)));
      case PartialSurrenderSummaryContext.LiveChange(var correction, var licencePosition, var ignored) ->
          Optional.of(getSummarySection(
              correction.getLicence().getLicenceReference(),
              licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition)));
    };
  }

  private SummarySection getSummarySection(String licenceReference, LocalDate surrenderDate) {
    var summaryCard = SummaryCard.simpleSummaryCard(
        SummaryDataView.newBuilder()
            .addStringValue("Licence", licenceReference)
            .addStringValue("Surrender date", DateUtil.formatLongDate(surrenderDate))
            .build()
    );

    return new SummarySection(SECTION_ORDER, List.of(SummaryItem.withCard(SURRENDER_DETAILS, summaryCard)));
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class PartialSurrenderDetailsSummarySectionService implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String SURRENDER_DETAILS = "Surrender details";
  static final int SECTION_ORDER = 10;

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public PartialSurrenderDetailsSummarySectionService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    return switch (context) {
      case PartialSurrenderSummaryContext.Staged(var licencePositionCorrection, var ignored, var ignoredState) ->
          Optional.of(getSummarySection(
              licencePositionCorrection.getLicenceCorrection().getLicence().getLicenceReference(),
              licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection)));
      case PartialSurrenderSummaryContext.LiveChange(
          var correction, var licencePosition, var ignored, var surrender, var ignoredState) ->
          Optional.of(getSummarySection(
              licencePosition.getLicence().getLicenceReference(),
              correction == null
                  ? Objects.requireNonNullElseGet(surrender.surrenderDate(), licencePosition::getPositionDate)
                  : licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition)));
    };
  }

  private SummarySection getSummarySection(String licenceReference, LocalDate surrenderDate) {
    var summaryCard = SummaryCard.simpleSummaryCard(
        SummaryDataView.newBuilder()
            .addStringValue("Licence", licenceReference)
            .addStringValue("Surrender date", DateUtil.formatLongDate(surrenderDate))
            .build()
    );

    return new SummarySection(
        SECTION_ORDER,
        List.of(SummaryItem.withCard(SURRENDER_DETAILS, summaryCard)),
        SummarySectionDisplay.INLINE
    );
  }
}

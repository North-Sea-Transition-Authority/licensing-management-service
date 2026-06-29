package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementRequest;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibility;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.OtherRequirementsVisibilityResolverService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class ContinuationRequirementSummarySectionService implements SummarySectionService<LicenceContinuationApplicationDetail> {

  public static final String SECTION_NAME = "Other requirement";
  public static final int SECTION_DISPLAY_ORDER = 30;
  private final LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService;
  private final OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService;

  public ContinuationRequirementSummarySectionService(
      LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService,
      OtherRequirementsVisibilityResolverService otherRequirementsVisibilityResolverService
  ) {
    this.licenceContinuationOtherRequirementService = licenceContinuationOtherRequirementService;
    this.otherRequirementsVisibilityResolverService = otherRequirementsVisibilityResolverService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var continuationRequirementSummaryItem = getContinuationRequirementSummaryItem(licenceContinuationApplicationDetail);
    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(continuationRequirementSummaryItem));
    return Optional.of(summarySection);
  }

  private SummaryItem getContinuationRequirementSummaryItem(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var otherRequirementRequest = licenceContinuationOtherRequirementService.getLicenceContinuationApplicationDetail(
        licenceContinuationApplicationDetail
    );
    var visibility = otherRequirementsVisibilityResolverService.resolveVisibility(licenceContinuationApplicationDetail);
    List<SummaryCard> summaryCards = new ArrayList<>();

    if (otherRequirementRequest.isPresent()) {
      var otherRequirementRequestSummaryCard = buildOtherRequirementRequestSummaryCard(
          otherRequirementRequest.get(), visibility);
      summaryCards.addAll(otherRequirementRequestSummaryCard);
    }

    return SummaryItem.withCards(
        SECTION_NAME,
        summaryCards
    );
  }

  private List<SummaryCard> buildOtherRequirementRequestSummaryCard(
      LicenceContinuationOtherRequirementRequest licenceContinuationOtherRequirementRequest,
      OtherRequirementsVisibility visibility
  ) {
    List<SummaryCard> cards = new ArrayList<>();

    if (visibility.showFinancialCapacity()) {
      var financialCapacityBuilder = SummaryDataView.newBuilder()
          .addStringValue(
              "Evidence of financial capacity submitted",
              licenceContinuationOtherRequirementRequest.getFinancialCapacityEvidenceSubmissionStatus()
          );

      if (BooleanUtils.isFalse(licenceContinuationOtherRequirementRequest.getFinancialCapacityEvidenceSubmissionStatus())) {
        financialCapacityBuilder.addStringValue(
            "Actions are being taken to provide evidence",
            licenceContinuationOtherRequirementRequest.getActionsToProvideFinancialEvidence()
        );
      }

      cards.add(SummaryCard.simpleSummaryCardWithHeading(
          "Financial Capacity", financialCapacityBuilder.build()
      ));
    }

    if (visibility.showRelinquishment()) {
      var relinquishmentBuilder = SummaryDataView.newBuilder()
          .addStringValue("Required amount of the licensed area relinquished",
              licenceContinuationOtherRequirementRequest.getRelinquishmentRequirementStatus()
          );

      if (BooleanUtils.isFalse(licenceContinuationOtherRequirementRequest.getRelinquishmentRequirementStatus())) {
        relinquishmentBuilder.addStringValue(
            "Actions are being taken to relinquish the required amount of the licence area",
            licenceContinuationOtherRequirementRequest.getActionsToRelinquishRequiredLicenceArea()
        );
      }

      cards.add(SummaryCard.simpleSummaryCardWithHeading("Relinquishment", relinquishmentBuilder.build()));
    }

    if (visibility.showDevelopmentConsent()) {
      var developmentConsentBuilder = SummaryDataView.newBuilder()
          .addStringValue(
              "Development Consent (PCON) been granted by the NSTA",
              licenceContinuationOtherRequirementRequest.getDevelopmentConsentGrantStatus()
          );

      if (BooleanUtils.isFalse(licenceContinuationOtherRequirementRequest.getDevelopmentConsentGrantStatus())) {
        developmentConsentBuilder.addStringValue(
            "Actions are being taken to get Development Consent approved",
            licenceContinuationOtherRequirementRequest.getActionsToApproveDevelopmentConsent()
        );
      }

      cards.add(SummaryCard.simpleSummaryCardWithHeading("Development Consent", developmentConsentBuilder.build()));
    }

    return cards;
  }
}
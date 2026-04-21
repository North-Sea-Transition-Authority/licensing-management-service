package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

public record OtherRequirementsVisibility(
    boolean showFinancialCapacity,
    boolean showRelinquishment,
    boolean showDevelopmentConsent
) {
  public boolean hasAnyRequirements() {
    return showFinancialCapacity || showRelinquishment || showDevelopmentConsent;
  }
}
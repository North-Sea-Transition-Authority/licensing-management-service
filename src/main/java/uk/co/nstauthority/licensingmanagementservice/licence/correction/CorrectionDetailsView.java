package uk.co.nstauthority.licensingmanagementservice.licence.correction;

public record CorrectionDetailsView(
    String correctionReference,
    String reason,
    String allocatedToUserName,
    String statusDisplayName,
    String licenceReference,
    String createdDate
) {
}
package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

public record ReminderRecipient(
    Integer licenceId,
    Integer responsibleOrganisationId,
    String licenseeName,
    String contactEmail
) {
}

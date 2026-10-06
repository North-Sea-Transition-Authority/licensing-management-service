package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

public record EventReminderRecipient(
    Integer licenceId,
    Integer responsibleOrganisationId,
    String licenseeName,
    String contactEmail
) {
}

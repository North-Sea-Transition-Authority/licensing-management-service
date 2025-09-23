package uk.co.nstauthority.licensingmanagementservice.file;

public record TestApplicationFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {
}
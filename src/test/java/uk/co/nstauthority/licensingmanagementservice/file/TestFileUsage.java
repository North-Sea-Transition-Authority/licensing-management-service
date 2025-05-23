package uk.co.nstauthority.licensingmanagementservice.file;

public record TestFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements XyzApplicationFileUsage {
}
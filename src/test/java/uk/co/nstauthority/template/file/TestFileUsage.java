package uk.co.nstauthority.template.file;

public record TestFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements XyzApplicationFileUsage {
}
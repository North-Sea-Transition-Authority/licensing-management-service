package uk.co.nstauthority.licensingmanagementservice.xyzapplication;

import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;

public record SupportingDocumentApplicationFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static SupportingDocumentApplicationFileUsage fromApplication(XyzApplication xyzApplication) {
    return new SupportingDocumentApplicationFileUsage(
        xyzApplication.getId().toString(),
        FileUsageType.APPLICATION_SUPPORTING_DOCUMENT.getUsageType(),
        "application-supporting-document"
    );
  }

}

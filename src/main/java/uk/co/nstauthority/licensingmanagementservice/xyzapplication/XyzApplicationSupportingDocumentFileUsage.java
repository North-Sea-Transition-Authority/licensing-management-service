package uk.co.nstauthority.licensingmanagementservice.xyzapplication;

import uk.co.nstauthority.licensingmanagementservice.file.XyzApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.XyzApplicationFileUsageType;

public record XyzApplicationSupportingDocumentFileUsage(
    String usageId,
    String usageType,
    String documentType
) implements XyzApplicationFileUsage {

  public static XyzApplicationSupportingDocumentFileUsage fromApplication(XyzApplication xyzApplication) {
    return new XyzApplicationSupportingDocumentFileUsage(
        xyzApplication.getId().toString(),
        XyzApplicationFileUsageType.SUPPORTING_DOCUMENT.getUsageType(),
        "xyz-supporting-document"
    );
  }

}

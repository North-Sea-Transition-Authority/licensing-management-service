package uk.co.nstauthority.template.xyzapplication;

import uk.co.nstauthority.template.file.XyzApplicationFileUsage;
import uk.co.nstauthority.template.file.XyzApplicationFileUsageType;

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

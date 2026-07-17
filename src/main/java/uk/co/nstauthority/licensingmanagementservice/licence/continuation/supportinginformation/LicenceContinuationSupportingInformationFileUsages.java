package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

public record LicenceContinuationSupportingInformationFileUsages(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static LicenceContinuationSupportingInformationFileUsages fromApplication(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    return new LicenceContinuationSupportingInformationFileUsages(
        licenceContinuationApplicationDetail.getId().toString(),
        FileUsageType.CONTINUATION_ADDITIONAL_SUPPORTING_DOCUMENT.getUsageType(),
        "licence-continuation-additional-supporting-document"
    );
  }
}

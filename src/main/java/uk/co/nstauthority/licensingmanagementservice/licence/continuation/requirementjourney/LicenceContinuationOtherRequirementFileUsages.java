package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

public record LicenceContinuationOtherRequirementFileUsages(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static LicenceContinuationOtherRequirementFileUsages fromApplication(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    return new LicenceContinuationOtherRequirementFileUsages(
        licenceContinuationApplicationDetail.getId().toString(),
        FileUsageType.CONTINUATION_OTHER_REQUIREMENT_DOCUMENT.getUsageType(),
        "licence-continuation-other-requirement-document"
    );
  }
}

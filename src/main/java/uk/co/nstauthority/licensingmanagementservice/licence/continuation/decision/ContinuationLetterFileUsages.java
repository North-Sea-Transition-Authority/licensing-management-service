package uk.co.nstauthority.licensingmanagementservice.licence.continuation.decision;

import uk.co.nstauthority.licensingmanagementservice.document.DocumentItemType;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

public record ContinuationLetterFileUsages(
    String usageId,
    String usageType,
    String documentType
) implements ApplicationFileUsage {

  public static ContinuationLetterFileUsages fromApplication(LicenceContinuationApplicationDetail applicationDetail) {
    return new ContinuationLetterFileUsages(
        applicationDetail.getLicenceContinuationApplication().getId().toString(),
        FileUsageType.APPLICATION_CONTINUATION_LETTER.getUsageType(),
        DocumentItemType.CONTINUATION_LETTER.name()
    );
  }
}
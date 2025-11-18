package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleFileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSupportingInformationFileUsagesTest {

  @Test
  void fromApplication_CorrectlyMapsIdsAndUsageTypes() {

    UUID expectedApplicationId = UUID.fromString("11112222-3333-4444-5555-666677778888");

    ScheduleWorkProgrammeApplicationDetail expectedDetail = new ScheduleWorkProgrammeApplicationDetail(expectedApplicationId);

    String expectedUsageType = LicenceScheduleFileUsageType.SCHEDULE_AMENDMENT_APP_SUPPORTING_DOCUMENT.getUsageType();
    String expectedDocumentType = "licence-schedule-application-supporting-document";

    LicenceScheduleSupportingInformationFileUsages result = LicenceScheduleSupportingInformationFileUsages.fromApplication(expectedDetail);

    assertThat(result.usageId()).isEqualTo(expectedApplicationId.toString());

    assertThat(result.usageType()).isEqualTo(expectedUsageType);

    assertThat(result.documentType()).isEqualTo(expectedDocumentType);
  }
}
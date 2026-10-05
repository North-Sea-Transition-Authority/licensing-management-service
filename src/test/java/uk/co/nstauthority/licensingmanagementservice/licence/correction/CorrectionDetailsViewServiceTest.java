package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectionDetailsViewServiceTest {

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private CorrectionDetailsViewService correctionDetailsViewService;

  @Test
  void getDetailsView() {
    var allocatedToWuaId = 123L;
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withCorrectionReference("COR-1")
        .withReason("Wrong licensee recorded")
        .withStatus(LicenceCorrectionStatus.IN_PROGRESS)
        .withAllocatedToWuaId(allocatedToWuaId)
        .withCreatedInstant(Instant.parse("2026-06-05T10:00:00Z"))
        .withLicence(LicenceTestUtil.builder()
            .withLicenceReference("P1234")
            .build())
        .build();
    var allocatedToUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(allocatedToWuaId)
        .withForename("Jane")
        .withSurname("Doe")
        .buildJson();

    when(energyPortalUserService.getByWuaId(
        WebUserAccountId.from(allocatedToWuaId),
        "Get correction allocated to user details"
    )).thenReturn(allocatedToUser);

    var result = correctionDetailsViewService.getDetailsView(correction);

    assertThat(result).isEqualTo(new CorrectionDetailsView(
        "COR-1",
        "Wrong licensee recorded",
        allocatedToUser.displayName(),
        LicenceCorrectionStatus.IN_PROGRESS.getDisplayName(),
        "P1234",
        "5 June 2026"
    ));
  }
}
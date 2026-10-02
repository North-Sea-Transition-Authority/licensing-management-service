package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenseeChangeServiceTest {

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private LicenseeChangeService licenseeChangeService;

  @Test
  void addLicenseeChangeForAddedLicencePosition() {
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();

    licenseeChangeService.addLicenseeChangeForAddedLicencePosition(
        licencePositionCorrection,
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    verify(licencePositionCorrectionService).save(licencePositionCorrection);
    var expectedLicenseeOperation = (LicenseeOperation)
        licencePositionCorrection.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(licencePositionCorrection.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void addLicenseeChangeForExistingLicencePosition_existingCorrection() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(10).build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition))
        .thenReturn(licencePositionCorrection);

    licenseeChangeService.addLicenseeChangeForExistingLicencePosition(
        licencePosition,
        licenceCorrection,
        joiningLicenseeIds,
        withdrawingLicenseeIds
    );

    ArgumentCaptor<LicencePositionCorrection> captor = ArgumentCaptor.forClass(LicencePositionCorrection.class);
    verify(licencePositionCorrectionService).save(captor.capture());

    var positionCorrection = captor.getValue();
    var expectedLicenseeOperation = (LicenseeOperation)
        positionCorrection.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(positionCorrection.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }
}
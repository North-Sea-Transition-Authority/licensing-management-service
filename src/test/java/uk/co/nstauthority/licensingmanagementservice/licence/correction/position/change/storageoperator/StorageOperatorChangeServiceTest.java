package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class StorageOperatorChangeServiceTest {

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private ChronologicalPositionService chronologicalPositionService;

  @Mock
  private OrganisationChangeService organisationChangeService;

  private StorageOperatorChangeService storageOperatorChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    storageOperatorChangeService = new StorageOperatorChangeService(
        licencePositionCorrectionService,
        organisationChangeService,
        chronologicalPositionService
    );
  }

  @Test
  void getJoiningOperatorForReinstating() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var payloadToUndo = CreateLicencePositionPayloadTestUtil
        .newBuilder()
        .withLicencePositionId(POSITION_ID.toString())
        .build();
    var otherPayload = CreateLicencePositionPayloadTestUtil
        .newBuilder()
        .withLicencePositionId(POSITION_ID.toString())
        .build();
    var positionCorrectionToUndo = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPayload(payloadToUndo)
        .build();
    var otherPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withPayload(otherPayload)
        .build();

    var targetPosition1 = ChronologicalPositionTestUtil.newBuilder()
        .withOrder(1)
        .withChanges(List.of(storageOperatorChange(1)))
        .build();
    var targetPosition2 = ChronologicalPositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withOrder(2)
        .withChanges(List.of(storageOperatorChange(2)))
        .build();
    var targetPositions = List.of(targetPosition1, targetPosition2);

    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(positionCorrectionToUndo, otherPositionCorrection));
    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, List.of(otherPositionCorrection), Set.of(POSITION_ID)))
        .thenReturn(targetPositions);
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, StorageOperatorOperation.class, targetPositions))
        .thenReturn(new StorageOperatorChangeContext(2, 1, "Uncorrected Current Operator Name", "Uncorrected Previous Operator Name"));

    assertThat(storageOperatorChangeService.getJoiningOperatorForReinstating(correction, positionCorrectionToUndo))
        .isEqualTo("Uncorrected Current Operator Name");
  }

  private static PositionChange storageOperatorChange(int operatorId) {
    return PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(LicenceOperation.newStorageOperatorOperation().withOperator(operatorId).build()))
        .build();
  }
}
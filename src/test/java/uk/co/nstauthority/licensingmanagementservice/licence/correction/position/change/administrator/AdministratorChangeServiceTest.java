package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changeoperation.LicencePositionUpdateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayload;

@ExtendWith(MockitoExtension.class)
class AdministratorChangeServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final Integer ADMINISTRATOR_ID = 116;
  private static final Integer UPDATED_ADMINISTRATOR_ID = 999;
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private ChronologicalPositionService chronologicalPositionService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FeatureService featureService;

  private AdministratorChangeService administratorChangeService;

  @BeforeEach
  void setUp() {
    administratorChangeService = new AdministratorChangeService(
        licencePositionStagedPayloadService,
        licencePositionChangeService,
        chronologicalPositionService,
        new LicencePositionNameService(organisationUnitQueryService, featureService)
    );
  }

  @Captor
  private ArgumentCaptor<LicencePositionStagedPayload> licencePositionStagedPayloadCaptor;

  @Test
  void correctExistingAdministratorChange_whenNoExistingUpdatePositionStagedPayload_createsNewPositionStagedPayloadWithUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID();

    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(originalChangeId)).thenReturn(liveAdminChange(UPDATED_ADMINISTRATOR_ID));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId.toString(), ADMINISTRATOR_ID);

    verify(licencePositionStagedPayloadService).save(licencePositionStagedPayloadCaptor.capture());
    var saved = licencePositionStagedPayloadCaptor.getValue();

    assertThat(saved.getLicenceCorrection()).isEqualTo(LICENCE_CORRECTION);
    assertThat(saved.getChangeType()).isEqualTo(LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    assertThat(saved.getTargetLicencePosition()).isEqualTo(licencePosition);

    var payload = (UpdateLicencePositionPayload) saved.getPayload();
    assertThat(payload.changes()).hasSize(1);

    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(change.changeId()).isEqualTo(originalChangeId.toString());
    assertThat(updateOperatorIdOf(change)).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenNoExistingPositionStagedPayloadAndLiveAdminUnchanged_doesNotSave() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID();

    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.empty());
    when(licencePositionChangeService.getByIdOrThrow(originalChangeId)).thenReturn(liveAdminChange(ADMINISTRATOR_ID));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId.toString(), ADMINISTRATOR_ID);

    verify(licencePositionStagedPayloadService, never()).save(any());
  }

  @Test
  void correctExistingAdministratorChange_whenExistingUpdatePositionStagedPayloadWithoutAdminChange_appendsUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().withChanges(List.of()).build())
        .build();

    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId, ADMINISTRATOR_ID);

    verify(licencePositionStagedPayloadService).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    assertThat(payload.changes().getFirst().changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(payload.changes().getFirst())).isEqualTo(ADMINISTRATOR_ID);
  }

  @Test
  void correctExistingAdministratorChange_whenAdminChangeAlreadyExists_rewritesExistingUpdateChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var originalChangeId = UUID.randomUUID().toString();
    var existing = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload()
            .withChanges(List.of(adminUpdateChange(originalChangeId, ADMINISTRATOR_ID)))
            .build())
        .build();

    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(LICENCE_CORRECTION, licencePosition))
        .thenReturn(Optional.of(existing));

    administratorChangeService
        .correctExistingAdministratorChange(licencePosition, LICENCE_CORRECTION, originalChangeId, UPDATED_ADMINISTRATOR_ID);

    verify(licencePositionStagedPayloadService).save(licencePositionStagedPayloadCaptor.capture());
    var payload = (UpdateLicencePositionPayload) licencePositionStagedPayloadCaptor.getValue().getPayload();
    assertThat(payload.changes()).hasSize(1);
    var change = payload.changes().getFirst();
    assertThat(change).isInstanceOf(UpdateChangeOperations.class);
    assertThat(change.changeId()).isEqualTo(originalChangeId);
    assertThat(updateOperatorIdOf(change)).isEqualTo(UPDATED_ADMINISTRATOR_ID);
  }

  private LicencePositionChange liveAdminChange(Integer administratorId) {
    var change = new LicencePositionChange();
    change.setOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(administratorId).build()));
    return change;
  }

  private LicencePositionChangeType adminUpdateChange(String changeId, Integer administratorId) {
    var administratorOperation = LicenceOperation.newAdministratorChange().withOperator(administratorId).build();
    var operation = LicencePositionChangeOperation.newLicencePositionUpdateOperation()
        .withOperationId(administratorOperation.id())
        .withOperation(administratorOperation)
        .build();
    return LicencePositionChangeType.updateChangeOperations()
        .withChangeId(changeId)
        .withOperations(List.of(operation))
        .build();
  }

  private static LicencePositionChangeType removeChange(String changeId) {
    return LicencePositionChangeType.removeChange().withChangeId(changeId).build();
  }

  private Integer updateOperatorIdOf(LicencePositionChangeType change) {
    var op = (LicencePositionUpdateOperation) change.operations().getFirst();
    return ((AdministratorOperation) op.operation()).operatorId();
  }
}

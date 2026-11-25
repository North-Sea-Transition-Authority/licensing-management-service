package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;

@ExtendWith(MockitoExtension.class)
class StorageOperatorChangeActionHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeStorageOperatorChange";
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
      .withTargetLicencePosition(POSITION)
      .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
      .build();
  private static final StorageOperatorChangeContext CONTEXT =
      new StorageOperatorChangeContext(2, 1, "Joining operator", "Withdrawing operator");

  @Mock
  private OrganisationChangeService organisationChangeService;

  @InjectMocks
  private StorageOperatorChangeActionHandler storageOperatorChangeActionHandler;

  @Test
  void executedChangeConfirmation() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION.getId(), StorageOperatorOperation.class))
        .thenReturn(CONTEXT);

    var result = storageOperatorChangeActionHandler.executedChangeConfirmation(
        CORRECTION,
        POSITION,
        LicencePositionChangeTestUtil.newBuilder().build()
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation("Joining operator"));
  }

  @Test
  void stagedChangeSummary_whenUndoing_thenJoiningOperatorIsTheCorrectedOne() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION.getId(), StorageOperatorOperation.class))
        .thenReturn(CONTEXT);

    var result = storageOperatorChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_STAGED_PAYLOAD,
        CHANGE_ID,
        ChangeAction.UNDO
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation("Joining operator"));
  }

  @Test
  void stagedChangeSummary_whenReinstating_thenJoiningOperatorIsTheOneBeingReinstated() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION.getId(), StorageOperatorOperation.class))
        .thenReturn(CONTEXT);
    when(organisationChangeService.getJoiningOrganisationForReinstating(CORRECTION, POSITION_STAGED_PAYLOAD, StorageOperatorOperation.class))
        .thenReturn("Reinstated operator");

    var result = storageOperatorChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_STAGED_PAYLOAD,
        CHANGE_ID,
        ChangeAction.REINSTATE
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation("Reinstated operator"));
  }

  private static ModelAndView expectedConfirmation(String joiningOperatorName) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingStorageOperatorName", "Withdrawing operator")
        .addObject("joiningStorageOperatorName", joiningOperatorName);
  }
}

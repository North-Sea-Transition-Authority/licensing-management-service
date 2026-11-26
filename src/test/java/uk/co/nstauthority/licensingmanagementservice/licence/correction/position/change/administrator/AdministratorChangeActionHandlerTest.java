package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.AdministratorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;

@ExtendWith(MockitoExtension.class)
class AdministratorChangeActionHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeAdministratorChange";
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().withId(UUID.randomUUID()).build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
      .withTargetLicencePosition(POSITION)
      .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
      .build();
  private static final AdministratorChangeContext CONTEXT =
      new AdministratorChangeContext(2, 1, "Joining admin", "Withdrawing admin");

  @Mock
  private OrganisationChangeService organisationChangeService;

  @InjectMocks
  private AdministratorChangeActionHandler administratorChangeActionHandler;

  @Test
  void executedChangeConfirmation() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION.getId(), AdministratorOperation.class))
        .thenReturn(CONTEXT);

    var result = administratorChangeActionHandler.executedChangeConfirmation(
        CORRECTION,
        POSITION,
        LicencePositionChangeTestUtil.newBuilder().build()
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation("Joining admin"));
  }

  @Test
  void stagedChangeConfirmation() {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION.getId(), AdministratorOperation.class))
        .thenReturn(CONTEXT);

    var result = administratorChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        positionStagedPayload,
        UUID.randomUUID().toString(),
        ChangeAction.UNDO
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation("Joining admin"));
  }

  @Test
  void stagedChangeSummary_whenReinstating_thenJoiningOperatorIsTheOneBeingReinstated() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION.getId(), AdministratorOperation.class))
        .thenReturn(CONTEXT);
    when(organisationChangeService.getJoiningOrganisationForReinstating(CORRECTION, POSITION_STAGED_PAYLOAD, AdministratorOperation.class))
        .thenReturn("Reinstated admin");

    var result = administratorChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_STAGED_PAYLOAD,
        CHANGE_ID,
        ChangeAction.REINSTATE
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation("Reinstated admin"));
  }

  private static ModelAndView expectedConfirmation(String joiningAdministratorName) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("withdrawingAdministratorName", "Withdrawing admin")
        .addObject("joiningAdministratorName", joiningAdministratorName);
  }
}

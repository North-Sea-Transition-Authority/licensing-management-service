package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Component
public class PartialSurrenderChangeActionHandler implements ChangeActionHandler {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/partialSurrender/removePartialSurrenderChange";

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  PartialSurrenderChangeActionHandler(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  @Override
  public ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var executedSurrender = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(liveChange.getId().toString());

    var surrenderDate = Objects.requireNonNullElseGet(
        executedSurrender.surrenderDate(),
        () -> licencePositionStagedPayloadService.getEffectivePositionDate(licenceCorrection, licencePosition)
    );

    return confirmation(executedSurrender, surrenderDate);
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayload,
      String changeId,
      ChangeAction action
  ) {
    var stagedSurrender = partialSurrenderCorrectionService.getStagedPartialSurrenderOrThrow(positionStagedPayload, changeId);

    var surrenderDate = Objects.requireNonNullElseGet(
        stagedSurrender.surrenderDate(),
        () -> licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)
    );

    return confirmation(stagedSurrender, surrenderDate);
  }

  @Override
  public void beforeDroppingStagedChange(
      LicencePositionStagedPayload positionStagedPayload,
      LicencePositionChangeType stagedChange
  ) {
    partialSurrenderCorrectionService.deleteStagedSurrenderArtefacts(stagedChange);
  }

  private ModelAndView confirmation(PartialSurrenderOperation surrender, @Nullable LocalDate surrenderDate) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("surrenderDate", surrenderDate == null ? null : DateUtil.formatLongDate(surrenderDate))
        .addObject("blockRows", partialSurrenderCorrectionService.getBlockRows(surrender));
  }
}

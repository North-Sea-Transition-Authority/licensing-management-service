package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
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

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  PartialSurrenderChangeActionHandler(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
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
        () -> licencePositionCorrectionService.getEffectivePositionDate(licenceCorrection, licencePosition)
    );

    return confirmation(executedSurrender, surrenderDate);
  }

  @Override
  public ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection,
      String changeId,
      ChangeAction action
  ) {
    var stagedSurrender = partialSurrenderCorrectionService.getStagedPartialSurrenderOrThrow(positionCorrection, changeId);

    var surrenderDate = Objects.requireNonNullElseGet(
        stagedSurrender.surrenderDate(),
        () -> licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)
    );

    return confirmation(stagedSurrender, surrenderDate);
  }

  @Override
  public void beforeDroppingStagedChange(
      LicencePositionCorrection positionCorrection,
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

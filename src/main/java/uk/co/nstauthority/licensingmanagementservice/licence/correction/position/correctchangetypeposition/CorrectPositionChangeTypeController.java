package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/position/{licencePositionId}/change/{changeId}/correct-change-type-position")
@InvokingUserCanViewCorrection
@LicencePositionIsNotRemovedInCorrection
public class CorrectPositionChangeTypeController {

  private final CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;

  CorrectPositionChangeTypeController(
      CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.correctPositionChangeTypeFormValidator = correctPositionChangeTypeFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping
  public ModelAndView renderMoveChangeTypePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService
        .getPositionForLicence(correction.getLicence(), licencePositionId);
    var changeTypePositionMoveOptions = changeTypePositionMoveOptions(
        licencePositionCorrectionService.getOrderableDatePositions(correction));

    return correctChangeTypePositionMoveModelAndView(
        new CorrectPositionChangeTypeForm(), licencePosition, correction, changeTypePositionMoveOptions);
  }

  @PostMapping
  public ModelAndView correctChangeTypePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") CorrectPositionChangeTypeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService
        .getPositionForLicence(correction.getLicence(), licencePositionId);
    var changeTypePositionMoveOptions = changeTypePositionMoveOptions(
        licencePositionCorrectionService.getOrderableDatePositions(correction));

    if (correctPositionChangeTypeFormValidator.hasErrors(
        form,
        bindingResult,
        List.copyOf(changeTypePositionMoveOptions.keySet())
    )) {
      return correctChangeTypePositionMoveModelAndView(
          form, licencePosition, correction, changeTypePositionMoveOptions);
    }

    NotificationBanner.newSuccessBannerWithHeader("Change type position updated", redirectAttributes);

    return ReverseRouter.redirectToUrl(correctionUrl(correction));
  }

  private ModelAndView correctChangeTypePositionMoveModelAndView(
      CorrectPositionChangeTypeForm form,
      LicencePosition licencePosition,
      LicenceCorrection correction,
      LinkedHashMap<String, String> changeTypePositionMoveOptions
  ) {
    return new ModelAndView("lms/licence/correction/change/moveChangeTypePosition")
        .addObject("pageTitle", "Update position of change type")
        .addObject("positionDate",
            DateUtil.formatLongDateWithOrder(licencePosition.getPositionDate(), licencePosition.getPositionDateOrder()))
        .addObject("positionReference", licencePosition.getLicenceTransaction().getRegulatorReference())
        .addObject("changeTypePositionMoveOptions", changeTypePositionMoveOptions)
        .addObject("form", form)
        .addObject("backLinkUrl", correctionUrl(correction));
  }

  private String correctionUrl(LicenceCorrection correction) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private static LinkedHashMap<String, String> changeTypePositionMoveOptions(
      List<OrderablePosition> targetPositions
  ) {
    return targetPositions.stream()
        .collect(Collectors.toMap(
            position -> position.id().toString(),
            position -> "%s - %s".formatted(
                position.reference(),
                DateUtil.formatLongDateWithOrder(position.effectiveDate(), position.effectiveDateOrder())),
            (existing, duplicate) -> existing,
            LinkedHashMap::new
        ));
  }

}
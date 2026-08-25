package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionId}/correct-position-order")
@InvokingUserCanViewCorrection
public class LicencePositionCorrectionOrderChangeController {

  private final CorrectPositionOrderFormValidator correctPositionOrderFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public LicencePositionCorrectionOrderChangeController(
      CorrectPositionOrderFormValidator correctPositionOrderFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.correctPositionOrderFormValidator = correctPositionOrderFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView renderCorrectionLicencePositionOrder(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var orderedPositions = licencePositionCorrectionService.getOrderableSameDatePositions(correction, licencePositionId);
    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(orderedPositions, licencePositionId);

    if (moveOptions.isEmpty()) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderCorrection(correction.getId(), null));
    }

    return correctPositionCorrectionOrderModelAndView(
        correction, orderedPositions, licencePositionId, moveOptions, new CorrectPositionOrderForm());
  }

  @PostMapping
  public ModelAndView correctLicencePositionCorrectionOrder(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") CorrectPositionOrderForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var orderedPositions = licencePositionCorrectionService.getOrderableSameDatePositions(correction, licencePositionId);
    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(orderedPositions, licencePositionId);

    if (correctPositionOrderFormValidator.hasErrors(form, bindingResult, moveOptions.keySet())) {
      return correctPositionCorrectionOrderModelAndView(correction, orderedPositions, licencePositionId, moveOptions, form);
    }

    var move = PositionMove.fromFormValue(form.getPositionMove().getInputValue());

    licencePositionCorrectionService.correctPositionOrder(
        correction, licencePositionId, move.targetId(), move.direction());

    NotificationBanner.newSuccessBannerWithHeader("Licence position order updated", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private ModelAndView correctPositionCorrectionOrderModelAndView(
      LicenceCorrection correction,
      List<OrderablePosition> orderedPositions,
      UUID positionBeingMovedId,
      LinkedHashMap<String, String> moveOptions,
      CorrectPositionOrderForm form
  ) {

    return new ModelAndView("lms/licence/correction/correctPositionCorrectionOrder")
        .addObject("pageTitle", buildPageTitle(orderedPositions, positionBeingMovedId, moveOptions))
        .addObject("form", form)
        .addObject("positionMoveOptions", moveOptions)
        .addObject("currentPositionOrder", PositionMoveOptionUtil.buildCurrentOrder(orderedPositions, positionBeingMovedId))
        .addObject("singleOutcome", moveOptions.size() == 1)
        .addObject("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction.getId(), null)));
  }

  private String buildPageTitle(
      List<OrderablePosition> orderedPositions,
      UUID positionBeingMovedId,
      LinkedHashMap<String, String> moveOptions
  ) {
    var movedReference = referenceOf(orderedPositions, positionBeingMovedId);

    if (moveOptions.size() != 1) {
      return "Correct the order of position %s".formatted(movedReference);
    }

    var move = PositionMove.fromFormValue(moveOptions.keySet().iterator().next());
    var targetReference = referenceOf(orderedPositions, move.targetId());
    var direction = move.direction() == PositionMoveDirection.BEFORE ? "before" : "after";

    return "Do you want position %s to be moved %s %s?".formatted(movedReference, direction, targetReference);
  }

  private String referenceOf(List<OrderablePosition> orderedPositions, UUID positionId) {
    return orderedPositions.stream()
        .filter(position -> position.id().equals(positionId))
        .map(OrderablePosition::reference)
        .findFirst()
        .orElse("");
  }
}
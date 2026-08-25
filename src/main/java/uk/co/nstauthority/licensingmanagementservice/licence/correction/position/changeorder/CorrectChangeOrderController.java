package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/position/{licencePositionId}/change/{changeId}/correct-change-order")
@InvokingUserCanViewCorrection
public class CorrectChangeOrderController {

  private final CorrectChangeOrderFormValidator correctChangeOrderFormValidator;
  private final CorrectChangeOrderService correctChangeOrderService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public CorrectChangeOrderController(
      CorrectChangeOrderFormValidator correctChangeOrderFormValidator,
      CorrectChangeOrderService correctChangeOrderService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.correctChangeOrderFormValidator = correctChangeOrderFormValidator;
    this.correctChangeOrderService = correctChangeOrderService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView renderCorrectChangeOrder(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var orderedChanges = correctChangeOrderService.getOrderableChanges(correction, licencePositionId);
    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(orderedChanges, changeId);
    var positionPageUrl = positionPageUrl(correction, licencePositionId);

    if (doesNotContainChange(orderedChanges, changeId) || moveOptions.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl);
    }

    return correctChangeOrderModelAndView(positionPageUrl, orderedChanges, changeId, moveOptions,
        new CorrectChangeOrderForm());
  }

  @PostMapping
  public ModelAndView correctChangeOrder(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") CorrectChangeOrderForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var orderedChanges = correctChangeOrderService.getOrderableChanges(correction, licencePositionId);
    var moveOptions = PositionMoveOptionUtil.buildMoveOptions(orderedChanges, changeId);
    var positionPageUrl = positionPageUrl(correction, licencePositionId);

    if (doesNotContainChange(orderedChanges, changeId) || moveOptions.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl);
    }

    if (correctChangeOrderFormValidator.hasErrors(form, bindingResult, moveOptions.keySet())) {
      return correctChangeOrderModelAndView(positionPageUrl, orderedChanges, changeId, moveOptions, form);
    }

    var move = PositionMove.fromFormValue(form.getChangeMove().getInputValue());

    correctChangeOrderService.correctChangeOrder(
        correction, licencePositionId, changeId, move.targetId(), move.direction());

    NotificationBanner.newSuccessBannerWithHeader("Change order updated", redirectAttributes);

    return ReverseRouter.redirectToUrl(positionPageUrl);
  }

  private String positionPageUrl(LicenceCorrection correction, UUID licencePositionId) {
    return licencePositionCorrectionService.findFirstAddedPositionCorrection(correction, licencePositionId)
        .map(LicencePositionCorrection::getId)
        .map(addedPositionCorrectionId -> ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(correction.getId(), addedPositionCorrectionId, null)))
        .orElseGet(() -> ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction.getId(), licencePositionId, null)));
  }

  private ModelAndView correctChangeOrderModelAndView(
      String positionPageUrl,
      List<OrderableChange> orderedChanges,
      UUID changeBeingMovedId,
      LinkedHashMap<String, String> moveOptions,
      CorrectChangeOrderForm form
  ) {
    return new ModelAndView("lms/licence/correction/correctChangeOrder")
        .addObject("pageTitle", buildPageTitle(orderedChanges, changeBeingMovedId, moveOptions))
        .addObject("form", form)
        .addObject("changeMoveOptions", moveOptions)
        .addObject("currentChangeOrder", PositionMoveOptionUtil.buildCurrentOrder(orderedChanges, changeBeingMovedId))
        .addObject("singleOutcome", moveOptions.size() == 1)
        .addObject("backLinkUrl", positionPageUrl);
  }

  private String buildPageTitle(
      List<OrderableChange> orderedChanges,
      UUID changeBeingMovedId,
      LinkedHashMap<String, String> moveOptions
  ) {
    var movedLabel = referenceOf(orderedChanges, changeBeingMovedId);

    if (moveOptions.size() != 1) {
      return "Correct the order of %s".formatted(movedLabel);
    }

    var move = PositionMove.fromFormValue(moveOptions.keySet().iterator().next());
    var targetLabel = referenceOf(orderedChanges, move.targetId());
    var direction = move.direction() == PositionMoveDirection.BEFORE ? "before" : "after";

    return "Do you want %s to be moved %s %s?".formatted(movedLabel, direction, targetLabel);
  }

  private static boolean doesNotContainChange(List<OrderableChange> orderedChanges, UUID changeId) {
    return orderedChanges.stream().noneMatch(change -> change.id().equals(changeId));
  }

  private static String referenceOf(List<OrderableChange> orderedChanges, UUID changeId) {
    return orderedChanges.stream()
        .filter(change -> change.id().equals(changeId))
        .map(OrderableChange::reference)
        .findFirst()
        .orElse("Not available");
  }
}
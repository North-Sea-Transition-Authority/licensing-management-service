package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.OrderableChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/position/{licencePositionId}/change/{changeId}/correct-change-type-position")
@InvokingUserCanViewCorrection
@LicencePositionIsNotRemovedInCorrection
public class CorrectPositionChangeTypeController {

  private final CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final CorrectChangeOrderService correctChangeOrderService;

  public CorrectPositionChangeTypeController(
      CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      CorrectChangeOrderService correctChangeOrderService
  ) {
    this.correctPositionChangeTypeFormValidator = correctPositionChangeTypeFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.correctChangeOrderService = correctChangeOrderService;
  }

  @GetMapping
  public ModelAndView renderMoveChangeTypePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var change = findChange(correctChangeOrderService.getOrderableChanges(correction, licencePositionId), changeId);
    if (change.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl(correction, licencePositionId));
    }

    var currentPosition = licencePositionCorrectionService.getOrderableDatePosition(correction, licencePositionId);
    var changeTypePositionMoveOptions = changeTypePositionMoveOptions(
        licencePositionCorrectionService.getOrderableDatePositionsExcluding(correction, licencePositionId)
    );

    return correctChangeTypePositionMoveModelAndView(
        new CorrectPositionChangeTypeForm(),
        change.get(),
        currentPosition,
        correction,
        changeTypePositionMoveOptions
    );
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
    var change = findChange(correctChangeOrderService.getOrderableChanges(correction, licencePositionId), changeId);
    if (change.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl(correction, licencePositionId));
    }

    var currentPosition = licencePositionCorrectionService.getOrderableDatePosition(correction, licencePositionId);
    var changeTypePositionMoveOptions = changeTypePositionMoveOptions(
        licencePositionCorrectionService.getOrderableDatePositionsExcluding(correction, licencePositionId)
    );

    if (correctPositionChangeTypeFormValidator.hasErrors(
        form,
        bindingResult,
        List.copyOf(changeTypePositionMoveOptions.keySet())
    )) {
      return correctChangeTypePositionMoveModelAndView(
          form,
          change.get(),
          currentPosition,
          correction,
          changeTypePositionMoveOptions
      );
    }

    var selectedMove = form.getChangeTypePositionMove().getInputValue();

    if (!CorrectPositionChangeTypeForm.OTHER_DATE_OPTION.equals(selectedMove)) {
      correctChangeOrderService.moveChangeToPosition(
          correction,
          licencePositionId,
          changeId,
          UUID.fromString(selectedMove)
      );
    }

    NotificationBanner.newSuccessBannerWithHeader(
        "Position of %s updated".formatted(change.get().reference()),
        redirectAttributes
    );

    return ReverseRouter.redirectToUrl(correctionUrl(correction));
  }

  private ModelAndView correctChangeTypePositionMoveModelAndView(
      CorrectPositionChangeTypeForm form,
      OrderableChange change,
      OrderablePosition currentPosition,
      LicenceCorrection correction,
      LinkedHashMap<String, String> changeTypePositionMoveOptions
  ) {
    return new ModelAndView("lms/licence/correction/change/moveChangeTypePosition")
        .addObject("pageTitle", buildPageTitle(change))
        .addObject(
            "positionDate",
            DateUtil.formatLongDateWithOrder(currentPosition.effectiveDate(), currentPosition.effectiveDateOrder())
        )
        .addObject("positionReference", currentPosition.reference())
        .addObject("changeTypePositionMoveOptions", changeTypePositionMoveOptions)
        .addObject("form", form)
        .addObject("backLinkUrl", correctionUrl(correction));
  }

  private String correctionUrl(LicenceCorrection correction) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private String positionPageUrl(LicenceCorrection correction, UUID licencePositionId) {
    return licencePositionCorrectionService.findFirstAddedPositionCorrection(correction, licencePositionId)
        .map(LicencePositionCorrection::getId)
        .map(addedPositionCorrectionId -> ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(correction.getId(), addedPositionCorrectionId, null)))
        .orElseGet(() -> ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction.getId(), licencePositionId, null)));
  }

  private static Optional<OrderableChange> findChange(List<OrderableChange> orderableChanges, UUID changeId) {
    return orderableChanges.stream()
        .filter(orderableChange -> orderableChange.id().equals(changeId))
        .findFirst();
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

  private static String buildPageTitle(OrderableChange change) {
    return "Update position of %s".formatted(change.reference());
  }

}
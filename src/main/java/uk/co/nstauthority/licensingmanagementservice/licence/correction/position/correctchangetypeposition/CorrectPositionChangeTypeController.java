package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionOrderForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionOrderFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.OrderablePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.MoveChangeToDateResult;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.OrderableChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
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
  private final CorrectPositionOrderFormValidator correctPositionOrderFormValidator;

  public CorrectPositionChangeTypeController(
      CorrectPositionChangeTypeFormValidator correctPositionChangeTypeFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      CorrectChangeOrderService correctChangeOrderService,
      CorrectPositionOrderFormValidator correctPositionOrderFormValidator
  ) {
    this.correctPositionChangeTypeFormValidator = correctPositionChangeTypeFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.correctChangeOrderService = correctChangeOrderService;
    this.correctPositionOrderFormValidator = correctPositionOrderFormValidator;
  }

  @GetMapping
  public ModelAndView renderMoveChangeTypePosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var change = findChange(correction, licencePositionId, changeId);
    if (change.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl(correction, licencePositionId));
    }

    var currentPosition = licencePositionCorrectionService.getOrderableDatePosition(correction, licencePositionId);
    var changeTypePositionMoveOptions = changeTypePositionMoveOptions(correction, licencePositionId);

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
    var change = findChange(correction, licencePositionId, changeId);
    if (change.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl(correction, licencePositionId));
    }

    var currentPosition = licencePositionCorrectionService.getOrderableDatePosition(correction, licencePositionId);
    var changeTypePositionMoveOptions = changeTypePositionMoveOptions(correction, licencePositionId);

    if (correctPositionChangeTypeFormValidator.hasErrors(
        form,
        bindingResult,
        changeTypePositionMoveOptions.keySet()
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
      addPositionUpdatedBanner(change.get(), redirectAttributes);
      return ReverseRouter.redirectToUrl(correctionUrl(correction));
    }

    var positionDate = form.getCorrectPositionDate().getAsLocalDate().orElseThrow();
    var result = correctChangeOrderService.moveChangeToDate(
        correction,
        licencePositionId,
        changeId,
        positionDate,
        null
    );

    return redirectFor(result, correction, licencePositionId, change.get(), positionDate, redirectAttributes);
  }

  @GetMapping("/position-order")
  public ModelAndView renderNewPositionOrder(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestParam(name = "positionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate positionDate,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var change = findChange(correction, licencePositionId, changeId);
    if (change.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl(correction, licencePositionId));
    }

    var positionsOnDate = licencePositionCorrectionService.getOrderablePositionsOnDate(correction, positionDate);

    if (positionsOnDate.isEmpty()) {
      return ReverseRouter.redirectToUrl(changeTypePageUrl(correction, licencePositionId, changeId));
    }

    return newPositionOrderModelAndView(
        correction,
        licencePositionId,
        changeId,
        positionDate,
        positionsOnDate,
        PositionMoveOptionUtil.buildInsertOptions(positionsOnDate),
        new CorrectPositionOrderForm()
    );
  }

  @PostMapping("/position-order")
  public ModelAndView placeNewPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable UUID changeId,
      @RequestParam(name = "positionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate positionDate,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") CorrectPositionOrderForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var change = findChange(correction, licencePositionId, changeId);
    if (change.isEmpty()) {
      return ReverseRouter.redirectToUrl(positionPageUrl(correction, licencePositionId));
    }

    var positionsOnDate = licencePositionCorrectionService.getOrderablePositionsOnDate(correction, positionDate);

    if (positionsOnDate.isEmpty()) {
      return ReverseRouter.redirectToUrl(changeTypePageUrl(correction, licencePositionId, changeId));
    }

    var insertOptions = PositionMoveOptionUtil.buildInsertOptions(positionsOnDate);

    if (correctPositionOrderFormValidator.hasErrors(form, bindingResult, insertOptions.keySet())) {
      return newPositionOrderModelAndView(
          correction,
          licencePositionId,
          changeId,
          positionDate,
          positionsOnDate,
          insertOptions,
          form
      );
    }

    var result = correctChangeOrderService.moveChangeToDate(
        correction,
        licencePositionId,
        changeId,
        positionDate,
        PositionMove.fromFormValue(form.getPositionMove().getInputValue())
    );

    return redirectFor(result, correction, licencePositionId, change.get(), positionDate, redirectAttributes);
  }

  private ModelAndView redirectFor(
      MoveChangeToDateResult result,
      LicenceCorrection correction,
      UUID licencePositionId,
      OrderableChange change,
      LocalDate positionDate,
      RedirectAttributes redirectAttributes
  ) {
    return switch (result.outcome()) {
      case MOVED_TO_EXISTING_POSITION -> {
        addPositionUpdatedBanner(change, redirectAttributes);
        yield ReverseRouter.redirect(on(CorrectChangeOrderController.class)
            .renderCorrectChangeOrder(correction, result.positionId(), change.id()));
      }
      case MOVED_TO_NEW_POSITION -> {
        addPositionUpdatedBanner(change, redirectAttributes);
        yield ReverseRouter.redirectToUrl(correctionUrl(correction));
      }
      case NEEDS_POSITION_ORDER -> ReverseRouter.redirect(on(CorrectPositionChangeTypeController.class)
          .renderNewPositionOrder(correction.getId(), licencePositionId, change.id(), positionDate, null));
    };
  }

  private static void addPositionUpdatedBanner(OrderableChange change, RedirectAttributes redirectAttributes) {
    NotificationBanner.newSuccessBannerWithHeader(
        "Position of %s updated".formatted(change.reference()),
        redirectAttributes
    );
  }

  private ModelAndView newPositionOrderModelAndView(
      LicenceCorrection correction,
      UUID licencePositionId,
      UUID changeId,
      LocalDate positionDate,
      List<OrderablePosition> positionsOnDate,
      LinkedHashMap<String, String> insertOptions,
      CorrectPositionOrderForm form
  ) {
    return new ModelAndView("lms/licence/correction/correctPositionCorrectionOrder")
        .addObject(
            "pageTitle",
            "Where should the new position on %s go?".formatted(DateUtil.formatLongDate(positionDate))
        )
        .addObject("form", form)
        .addObject("positionMoveOptions", insertOptions)
        .addObject("currentPositionOrder", PositionMoveOptionUtil.buildCurrentOrder(positionsOnDate, null))
        .addObject("singleOutcome", false)
        .addObject("backLinkUrl", changeTypePageUrl(correction, licencePositionId, changeId));
  }

  private String changeTypePageUrl(LicenceCorrection correction, UUID licencePositionId, UUID changeId) {
    return ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
        .renderMoveChangeTypePosition(correction.getId(), licencePositionId, changeId, null));
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
        .renderCorrection(correction));
  }

  private String positionPageUrl(LicenceCorrection correction, UUID licencePositionId) {
    return licencePositionCorrectionService.findFirstAddedPositionCorrection(correction, licencePositionId)
        .map(addedPositionCorrection -> ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(correction, addedPositionCorrection)))
        .orElseGet(() -> ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, new LicencePosition(licencePositionId))));
  }

  private Optional<OrderableChange> findChange(LicenceCorrection correction, UUID licencePositionId, UUID changeId) {
    return correctChangeOrderService.getOrderableChanges(correction, licencePositionId).stream()
        .filter(orderableChange -> orderableChange.id().equals(changeId))
        .findFirst();
  }

  private LinkedHashMap<String, String> changeTypePositionMoveOptions(
      LicenceCorrection correction,
      UUID licencePositionId
  ) {
    return licencePositionCorrectionService.getOrderableDatePositionsExcluding(correction, licencePositionId).stream()
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
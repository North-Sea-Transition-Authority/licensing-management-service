package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/add-position")
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@InvokingUserCanViewCorrection
public class AddLicencePositionStagedPayloadController {

  private static final String PAGE_TITLE = "Add a position";

  private final AddLicencePositionStagedPayloadFormValidator addLicencePositionStagedPayloadValidator;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  public AddLicencePositionStagedPayloadController(
      AddLicencePositionStagedPayloadFormValidator addLicencePositionStagedPayloadValidator,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService
  ) {
    this.addLicencePositionStagedPayloadValidator = addLicencePositionStagedPayloadValidator;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  @GetMapping
  public ModelAndView renderAddLicencePositionStagedPayload(
      LicenceCorrection correction
  ) {
    return addLicencePositionStagedPayloadModelAndView(correction, new AddLicencePositionStagedPayloadForm());
  }

  @PostMapping
  ModelAndView addLicencePositionStagedPayload(
      LicenceCorrection correction,
      @ModelAttribute("form") AddLicencePositionStagedPayloadForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (addLicencePositionStagedPayloadValidator.hasErrors(form, correction, bindingResult)) {
      return addLicencePositionStagedPayloadModelAndView(correction, form);
    }

    var addedPositionId = licencePositionStagedPayloadService.addNewPosition(
        correction,
        form.getPositionDate().getAsLocalDate().orElseThrow(),
        form.getCorrectionReference().getInputValue()
    );

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position added")
        .applyTo(redirectAttributes);

    return LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(correction, addedPositionId);
  }

  private ModelAndView addLicencePositionStagedPayloadModelAndView(
      LicenceCorrection correction,
      AddLicencePositionStagedPayloadForm form
  ) {
    return new ModelAndView("lms/licence/correction/addPosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("backLinkUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction, null)));
  }
}
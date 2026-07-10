package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/add-position")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class AddLicencePositionCorrectionController {

  private static final String PAGE_TITLE = "Add a position";

  private final AddLicencePositionCorrectionFormValidator addLicencePositionCorrectionValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public AddLicencePositionCorrectionController(
      AddLicencePositionCorrectionFormValidator addLicencePositionCorrectionValidator,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.addLicencePositionCorrectionValidator = addLicencePositionCorrectionValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView renderAddLicencePositionCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    return addLicencePositionCorrectionModelAndView(correction, new AddLicencePositionCorrectionForm());
  }

  @PostMapping
  ModelAndView addLicencePositionCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") AddLicencePositionCorrectionForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (addLicencePositionCorrectionValidator.hasErrors(form, correction, bindingResult)) {
      return addLicencePositionCorrectionModelAndView(correction, form);
    }

    licencePositionCorrectionService.addNewPosition(
        correction,
        form.getPositionDate().getAsLocalDate().orElseThrow(),
        form.getCorrectionReference().getInputValue()
    );

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position added")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private ModelAndView addLicencePositionCorrectionModelAndView(
      LicenceCorrection correction,
      AddLicencePositionCorrectionForm form
  ) {
    return new ModelAndView("lms/licence/correction/addPosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("backLinkUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction.getId(), null)));
  }
}
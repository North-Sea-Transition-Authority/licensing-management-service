package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/general-details")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
public class UpdateCorrectionGeneralDetailsController {

  private static final String PAGE_TITLE = "Update correction details";

  private final UpdateCorrectionGeneralDetailsFormValidator updateCorrectionGeneralDetailsFormValidator;
  private final UpdateCorrectionGeneralDetailsService updateCorrectionGeneralDetailsService;

  public UpdateCorrectionGeneralDetailsController(
      UpdateCorrectionGeneralDetailsFormValidator updateCorrectionGeneralDetailsFormValidator,
      UpdateCorrectionGeneralDetailsService updateCorrectionGeneralDetailsService
  ) {
    this.updateCorrectionGeneralDetailsFormValidator = updateCorrectionGeneralDetailsFormValidator;
    this.updateCorrectionGeneralDetailsService = updateCorrectionGeneralDetailsService;
  }

  @GetMapping
  public ModelAndView renderUpdateGeneralDetails(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var form = UpdateCorrectionGeneralDetailsForm.from(correction);
    var allocatableUsers = updateCorrectionGeneralDetailsService.getAllocatableUsers(correction.getLicence());

    return updateGeneralDetailsModelAndView(correction, form, allocatableUsers);
  }

  @PostMapping
  ModelAndView updateGeneralDetails(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") UpdateCorrectionGeneralDetailsForm form,
      BindingResult bindingResult,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var allocatableUsers = updateCorrectionGeneralDetailsService.getAllocatableUsers(correction.getLicence());

    if (updateCorrectionGeneralDetailsFormValidator.hasErrors(form, bindingResult, allocatableUsers)) {
      return updateGeneralDetailsModelAndView(correction, form, allocatableUsers);
    }

    var allocatedToWuaId = Long.parseLong(form.getAllocatedToWuaId());

    updateCorrectionGeneralDetailsService.updateGeneralDetails(
        correction,
        form.getCorrectionReference().getInputValue(),
        form.getReason().getInputValue(),
        allocatedToWuaId
    );

    NotificationBanner.newSuccessBannerWithHeader(
        "Licence correction details updated",
        redirectAttributes
    );

    if (allocatedToWuaId != user.wuaId()) {
      return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
    }

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction.getId(), null));
  }

  private ModelAndView updateGeneralDetailsModelAndView(
      LicenceCorrection correction,
      UpdateCorrectionGeneralDetailsForm form,
      Map<String, String> allocatableUsers
  ) {
    return new ModelAndView("lms/licence/correction/updateGeneralDetails")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("form", form)
        .addObject("allocatableUsers", allocatableUsers)
        .addObject("backLinkUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction.getId(), null)));
  }
}
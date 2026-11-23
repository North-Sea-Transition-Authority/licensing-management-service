package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

/**
 * Adds a change to a position, or corrects a live change, through a single form page, whatever kind of change it is.
 * What differs between kinds of change comes from its {@link ChangeFormHandler}.
 */
@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@InvokingUserCanViewCorrection
public class ChangeFormController {

  static final String FORM_NAME = "form";

  private final ChangeFormService changeFormService;

  ChangeFormController(ChangeFormService changeFormService) {
    this.changeFormService = changeFormService;
  }

  @RequestMapping(value = "/position/{licencePositionId}/change/add", method = {RequestMethod.GET, RequestMethod.POST})
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView addToExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @RequestParam("type") AddPositionChangeType changeType,
      HttpMethod httpMethod,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes
  ) {
    var handler = changeFormService.getHandler(correction, changeType);
    changeFormService.checkNoLiveChangeOfType(handler, licencePosition);

    return handle(
        handler,
        new ChangeFormTarget.ExecutedPosition(correction, licencePosition),
        "added",
        httpMethod,
        request,
        redirectAttributes
    );
  }

  @RequestMapping(
      value = "/added-position/{licencePositionCorrectionId}/change/add",
      method = {RequestMethod.GET, RequestMethod.POST}
  )
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView addToAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @RequestParam("type") AddPositionChangeType changeType,
      HttpMethod httpMethod,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes
  ) {
    return handle(
        changeFormService.getHandler(correction, changeType),
        new ChangeFormTarget.AddedPosition(licencePositionCorrection),
        "added",
        httpMethod,
        request,
        redirectAttributes
    );
  }

  @RequestMapping(
      value = "/position/{licencePositionId}/change/{changeId}/correct",
      method = {RequestMethod.GET, RequestMethod.POST}
  )
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView correctLiveChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      HttpMethod httpMethod,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes
  ) {
    return handle(
        changeFormService.getHandler(correction, change),
        new ChangeFormTarget.LiveChange(correction, licencePosition, change),
        "corrected",
        httpMethod,
        request,
        redirectAttributes
    );
  }

  private static <F> ModelAndView handle(
      ChangeFormHandler<F> handler,
      ChangeFormTarget target,
      String pastParticiple,
      HttpMethod httpMethod,
      HttpServletRequest request,
      RedirectAttributes redirectAttributes
  ) {
    var positionPageUrl = positionPageUrl(target);

    if (HttpMethod.GET.equals(httpMethod)) {
      return handler.formPage(target).addObject("cancelUrl", positionPageUrl);
    }

    var form = handler.newForm();
    var bindingResult = bind(form, request);

    var invalidFormPage = handler.formPageIfInvalid(target, form, bindingResult);

    if (invalidFormPage.isPresent()) {
      return invalidFormPage.get()
          .addObject(BindingResult.MODEL_KEY_PREFIX + FORM_NAME, bindingResult)
          .addObject("cancelUrl", positionPageUrl);
    }

    handler.save(target, form);

    NotificationBanner.newSuccessBannerWithHeader(
        "%s %s".formatted(handler.displayName(), pastParticiple),
        redirectAttributes
    );

    return ReverseRouter.redirectToUrl(positionPageUrl);
  }

  /**
   * Binds the request to the form as {@code @ModelAttribute("form")} would, trimming strings to null as
   * {@link uk.co.nstauthority.licensingmanagementservice.mvc.DefaultPageControllerAdvice} does. The form's type depends
   * on the handler, so it cannot be declared as a parameter.
   */
  private static BindingResult bind(Object form, HttpServletRequest request) {
    var binder = new ServletRequestDataBinder(form, FORM_NAME);
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    binder.bind(request);
    return binder.getBindingResult();
  }

  private static String positionPageUrl(ChangeFormTarget target) {
    return LicencePositionCorrectionRouteUtil.getPositionPageUrl(target.correction(), target.positionId());
  }
}

package uk.co.nstauthority.licensingmanagementservice.feedback;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;

@Controller
@RequestMapping
public class FeedbackController {

  public static final String PAGE_NAME = "Feedback";

  private final FeedbackService feedbackService;
  private final FeedbackFormValidator feedbackFormValidator;
  private final UserDetailService userDetailService;

  FeedbackController(FeedbackService feedbackService,
                     FeedbackFormValidator feedbackFormValidator,
                     UserDetailService userDetailService
  ) {
    this.feedbackService = feedbackService;
    this.feedbackFormValidator = feedbackFormValidator;
    this.userDetailService = userDetailService;
  }

  @GetMapping("/feedback")
  public ModelAndView getFeedback(@ModelAttribute("form") FeedbackForm form) {
    return getFeedbackModelAndView(form);
  }

  @PostMapping("/feedback")
  public ModelAndView submitFeedback(@ModelAttribute("form") FeedbackForm form,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {

    feedbackFormValidator.validate(form, bindingResult);

    if (bindingResult.hasErrors()) {
      return getFeedbackModelAndView(form);
    }

    feedbackService.saveFeedback(form.getServiceRating(), form.getFeedback().getInputValue(), userDetailService.getUserDetail());

    NotificationBanner.newSuccessBannerWithHeader("Your feedback has been submitted", redirectAttributes);

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @GetMapping("/application/{applicationId}/feedback")
  public ModelAndView getApplicationFeedback(XyzApplication xyzApplication,
                                             @ModelAttribute("form") FeedbackForm form) {
    return getApplicationFeedbackModelAndView(form, xyzApplication);
  }

  @PostMapping("/application/{applicationId}/feedback")
  public ModelAndView submitApplicationFeedback(XyzApplication xyzApplication,
                                                @ModelAttribute("form") FeedbackForm form,
                                                BindingResult bindingResult,
                                                RedirectAttributes redirectAttributes) {
    feedbackFormValidator.validate(form, bindingResult);

    if (bindingResult.hasErrors()) {
      return getApplicationFeedbackModelAndView(form, xyzApplication);
    }

    feedbackService.saveFeedback(
        xyzApplication,
        form.getServiceRating(),
        form.getFeedback().getInputValue(),
        userDetailService.getUserDetail());

    NotificationBanner.newSuccessBannerWithHeader("Your feedback has been submitted", redirectAttributes);

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  private ModelAndView getBaseModelAndView(FeedbackForm feedbackForm) {
    return new ModelAndView("lms/feedback/feedback")
        .addObject("form", feedbackForm)
        .addObject("pageName", PAGE_NAME)
        .addObject("serviceRatings",
            DisplayableEnumOptionUtil.getDisplayableOptions(ServiceFeedbackRating.class));
  }

  private ModelAndView getApplicationFeedbackModelAndView(FeedbackForm feedbackForm, XyzApplication xyzApplication) {
    return getBaseModelAndView(feedbackForm)
        .addObject("actionUrl", ReverseRouter.route(on(FeedbackController.class)
            .submitApplicationFeedback(xyzApplication, null, null, null)));
  }

  private ModelAndView getFeedbackModelAndView(FeedbackForm feedbackForm) {
    return getBaseModelAndView(feedbackForm)
        .addObject("actionUrl", ReverseRouter.route(on(FeedbackController.class)
            .submitFeedback(null, null, null)));
  }
}
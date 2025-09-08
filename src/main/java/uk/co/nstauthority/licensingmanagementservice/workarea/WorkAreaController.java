package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.SelectScheduleWorkProgrammeApplicationLicenceTypeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/work-area")
@SessionAttributes("workAreaSession")
public class WorkAreaController {
  public static final String WORK_AREA_PAGE_NAME = "Work area";

  private final WorkAreaService workAreaService;

  public WorkAreaController(WorkAreaService workAreaService) {
    this.workAreaService = workAreaService;
  }

  @GetMapping
  public ModelAndView getWorkArea(@ModelAttribute("workAreaSession") WorkAreaSession workAreaSession, ServiceUserDetail user) {
    return getModelAndView(workAreaSession.getWorkAreaFilterForm());
  }

  @PostMapping
  ModelAndView renderWorkAreaResults(@ModelAttribute("form") WorkAreaFilterForm form,
                                     @ModelAttribute("workAreaSession") WorkAreaSession workAreaSession) {
    workAreaSession.update(form);
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @GetMapping("/clear-filters")
  public ModelAndView clearWorkAreaFilters(@ModelAttribute("workAreaSession") WorkAreaSession workAreaSession,
                                           SessionStatus sessionStatus) {
    sessionStatus.setComplete();
    workAreaSession.clearSession();
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  @ModelAttribute("workAreaSession")
  private WorkAreaSession getWorkAreaSessionWithDefaultFilters(@ModelAttribute("form") WorkAreaFilterForm form) {
    return new WorkAreaSession(form);
  }

  private @NotNull ModelAndView getModelAndView(WorkAreaFilterForm form) {
    return new ModelAndView("lms/workarea/workArea")
        .addObject("pageTitle", WORK_AREA_PAGE_NAME)
        .addObject("workAreaItems", workAreaService.getSearchResultItems(form))
        .addObject("canStartApplication", true)
        .addObject("startApplicationUrl", ReverseRouter
                .route(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).renderSelectLicenceType()))
        .addObject("form", form)
        .addObject("clearFilterUrl",
        ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null)));
  }
}

package uk.co.nstauthority.template.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.TeamQueryService;
import uk.co.nstauthority.template.xyzapplication.StartXyzApplicationController;

@Controller
@RequestMapping("/work-area")
@SessionAttributes("workAreaSession")
public class WorkAreaController {

  private final WorkAreaService workAreaService;
  private final TeamQueryService teamQueryService;

  public WorkAreaController(WorkAreaService workAreaService, TeamQueryService teamQueryService) {
    this.workAreaService = workAreaService;
    this.teamQueryService = teamQueryService;
  }

  @GetMapping
  public ModelAndView getWorkArea(@ModelAttribute("workAreaSession") WorkAreaSession workAreaSession, ServiceUserDetail user) {
    return getModelAndView(workAreaSession.getWorkAreaFilterForm(), user);
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

  private @NotNull ModelAndView getModelAndView(WorkAreaFilterForm form, ServiceUserDetail user) {
    var canStartApplication = teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), Set.of(Role.EDIT_APPLICATION));
    return new ModelAndView("xyz/workarea/workArea")
        .addObject("workAreaItems", workAreaService.getSearchResultItems(form))
        .addObject("canStartApplication", canStartApplication)
        .addObject("startApplicationUrl", ReverseRouter.route(on(StartXyzApplicationController.class).startApplication()))
        .addObject("form", form)
        .addObject("clearFilterUrl",
        ReverseRouter.route(on(WorkAreaController.class).clearWorkAreaFilters(null, null)));
  }
}

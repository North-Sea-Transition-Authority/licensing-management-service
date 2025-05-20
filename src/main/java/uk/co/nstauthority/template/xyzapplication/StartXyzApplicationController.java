package uk.co.nstauthority.template.xyzapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.template.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.template.authorisation.RolesAndTeamType;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.teams.Role;
import uk.co.nstauthority.template.teams.TeamType;
import uk.co.nstauthority.template.workarea.WorkAreaController;
import uk.co.nstauthority.template.xyzapplication.tasklist.XyzApplicationTaskListController;

@Controller
@RequestMapping("/start-application")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.EDIT_APPLICATION}, teamType = TeamType.ORGANISATION)
})
public class StartXyzApplicationController {

  private final XyzApplicationService xyzApplicationService;

  public StartXyzApplicationController(XyzApplicationService xyzApplicationService) {
    this.xyzApplicationService = xyzApplicationService;
  }

  @GetMapping
  public ModelAndView startApplication() {
    return new ModelAndView("xyz/application/startApplication")
        .addObject("actionUrl", ReverseRouter.route(on(StartXyzApplicationController.class).createApplication()))
        .addObject("backLinkUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }

  @PostMapping
  public ModelAndView createApplication() {
    var xyzApplication = xyzApplicationService.finalAllMockedApplications().getFirst();
    return ReverseRouter.redirect(on(XyzApplicationTaskListController.class).getTaskList(xyzApplication.getId(), null, null));
  }
}

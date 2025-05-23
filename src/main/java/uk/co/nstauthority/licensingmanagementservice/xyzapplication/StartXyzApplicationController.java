package uk.co.nstauthority.licensingmanagementservice.xyzapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.tasklist.XyzApplicationTaskListController;

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
    return new ModelAndView("lms/application/startApplication")
        .addObject("actionUrl", ReverseRouter.route(on(StartXyzApplicationController.class).createApplication()))
        .addObject("backLinkUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }

  @PostMapping
  public ModelAndView createApplication() {
    var xyzApplication = xyzApplicationService.finalAllMockedApplications().getFirst();
    return ReverseRouter.redirect(on(XyzApplicationTaskListController.class).getTaskList(xyzApplication.getId(), null, null));
  }
}

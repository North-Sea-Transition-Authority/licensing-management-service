package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney.StartContinuationApplicationController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.SelectScheduleWorkProgrammeApplicationLicenceTypeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class SelectApplicationTypeService {
  public ModelAndView getJourneyStartRedirectRoute(ApplicationType selectedApplicationType) {
    return switch (selectedApplicationType) {
      case SCHEDULE_AMENDMENT_APPLICATION ->
          ReverseRouter.redirect(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class).renderSelectLicenceType());
      case CONTINUATION_APPLICATION -> ReverseRouter.redirect(on(StartContinuationApplicationController.class).render());
    };
  }
}

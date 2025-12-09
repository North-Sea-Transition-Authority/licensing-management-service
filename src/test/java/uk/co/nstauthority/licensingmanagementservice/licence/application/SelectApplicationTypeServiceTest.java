package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.SelectScheduleWorkProgrammeApplicationLicenceTypeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class SelectApplicationTypeServiceTest {

  @InjectMocks
  private SelectApplicationTypeService selectApplicationTypeService;

  @Test
  void getJourneyStartRedirectRoute() {
    var result = selectApplicationTypeService.getJourneyStartRedirectRoute(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    assertThat(result).hasToString(ReverseRouter.redirect(on(SelectScheduleWorkProgrammeApplicationLicenceTypeController.class)
        .renderSelectLicenceType()).toString());
  }
}
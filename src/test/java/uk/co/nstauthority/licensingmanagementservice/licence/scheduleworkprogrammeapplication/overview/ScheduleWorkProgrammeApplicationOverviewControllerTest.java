package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.LicenceScheduleSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ContextConfiguration(classes = ScheduleWorkProgrammeApplicationOverviewController.class)
class ScheduleWorkProgrammeApplicationOverviewControllerTest extends AbstractControllerTest {

  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @MockitoBean
  private ScheduleWorkProgrammeApplicationOverviewService overviewService;

  @MockitoBean
  private LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;

  @Test
  void renderOverview_whenSubmitted_displaysApplicationContext() throws Exception {
    var licence = createLicence();
    var applicationDetailId = UUID.randomUUID();
    var submittedDatetime = Instant.parse("2024-03-15T10:30:00Z");
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var swpApp = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(applicationDetailId)
        .withStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(submittedDatetime)
        .withApplicationReference("LMS/EAA/2024/1")
        .withScheduleWorkProgrammeApplication(swpApp)
        .build();

    var applicationContext = new ScheduleWorkProgrammeApplicationContext(
        "LMS/EAA/2024/1",
        "Carbon storage licence - CS1",
        List.of(SummaryDataView.newBuilder()
            .addStringValue("Status", "Submitted")
            .addStringValue("Licence reference", "CS1")
            .addStringValue("Submitted by", "John Smith")
            .addStringValue("Submission date", "15 March 2024")
            .build())
    );

    when(scheduleWorkProgrammeApplicationService.getDetailByIdOrThrow(applicationDetailId))
        .thenReturn(applicationDetail);
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(ORGANISATION_USER_WUA_ID)))
        .thenReturn(true);
    when(overviewService.getApplicationContext(applicationDetail, licence))
        .thenReturn(applicationContext);
    when(licenceScheduleSummarySectionService.getSummarySections(applicationDetail, USER))
        .thenReturn(List.of());
    when(scheduleWorkProgrammeApplicationActionService.getAvailableUserActionItems(applicationDetail, USER))
        .thenReturn(List.of());

    mockMvc.perform(
            get(ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(applicationDetailId, null, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name(
            "lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationOverview"))
        .andExpect(model().attribute("applicationContext", applicationContext))
        .andExpect(model().attribute("summarySections", List.of()))
        .andExpect(model().attribute("accordionId", applicationDetailId))
        .andExpect(model().attribute("applicationActions", List.of()));
  }

  private Licence createLicence() {
    var licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");
    return licence;
  }
}

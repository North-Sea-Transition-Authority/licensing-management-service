package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = WorkProgrammeActivityDeletionController.class)
class WorkProgrammeActivityDeletionControllerTest extends AbstractControllerTest {

  private ServiceUserDetail organisationUser;
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private Licence licence;
  private LicenceScheduleDetail licenceScheduleDetail;

  private WorkProgrammeActivity workProgrammeActivity;

  @BeforeEach
  void setUp() {
    organisationUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(ORGANISATION_USER_WUA_ID)
        .build();

    licence = LicenceTestUtil.builder().build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(UUID.randomUUID());
    workProgrammeActivity.setLicenceScheduleDetail(licenceScheduleDetail);
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    workProgrammeActivity.setDescription("description");
    workProgrammeActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    workProgrammeActivity.setDueDate(LocalDate.of(2025, 1, 1));
    workProgrammeActivity.setComments("comments");
  }

  @SecurityTest
  void renderDeletePhasePage() throws Exception {
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("caption");

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).renderDeleteActivityPage(workProgrammeActivity.getId(), null)))
                .with(user(organisationUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/deleteWorkProgrammeActivity"))
        .andExpect(model().attribute("pageTitle", "Do you want to delete the %s activity?".formatted(workProgrammeActivity.getCategoryString())))
        .andExpect(model().attribute("summaryView", WorkProgrammeActivitySummaryView.fromWorkProgrammeActivity(workProgrammeActivity)))
        .andExpect(model().attribute("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl()))
        .andExpect(model().attribute("pageCaption", "caption"));
  }

  @Test
  void submitDeletePhasePage() throws Exception {
    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId())).thenReturn(workProgrammeActivity);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class).submitDeleteActivityPage(workProgrammeActivity.getId(), null, null)))
                .with(user(organisationUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());

    verify(workProgrammeActivityService).deleteWorkProgrammeActivity(workProgrammeActivity);
  }

}
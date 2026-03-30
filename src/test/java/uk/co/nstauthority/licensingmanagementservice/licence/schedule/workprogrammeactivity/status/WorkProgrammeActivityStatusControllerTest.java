package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivitySummaryView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = WorkProgrammeActivityStatusController.class)
class WorkProgrammeActivityStatusControllerTest extends AbstractControllerTest {

  @MockitoBean
  private WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  @MockitoBean
  private WorkProgrammeActivityStatusValidator workProgrammeActivityStatusValidator;
  
  private static final String PAGE_CAPTION = "page caption";

  private Licence licence;
  private WorkProgrammeActivity workProgrammeActivity;
  
  @BeforeEach
  void setUp() {
    when(teamQueryService.userHasRoleInTeamType(
        regulatorUser.wuaId(),
        TeamType.LICENCE_MANAGEMENT,
        Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR))
    ).thenReturn(true);
    
    licence = LicenceTestUtil.builder()
        .withId(1).withLicenceReference("P001")
        .withLicenceType(LicenceType.SEAWARD_EXPLORATION)
        .build();
    
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence)
    );
    
    workProgrammeActivity = new WorkProgrammeActivity();
    workProgrammeActivity.setId(UUID.randomUUID());
    workProgrammeActivity.setLicenceScheduleDetail(licenceScheduleDetail);
    workProgrammeActivity.setCategory(WorkProgrammeActivityCategory.WELL_TEST);
    workProgrammeActivity.setDescription("Description");
    workProgrammeActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);

    when(workProgrammeActivityService.getWorkProgrammeActivityByIdOrThrow(workProgrammeActivity.getId()))
        .thenReturn(workProgrammeActivity);
  }
  
  @SecurityTest
  void renderStatusUpdatePage() throws Exception {
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);

    var form = new WorkProgrammeActivityStatusForm();

    when(workProgrammeActivityStatusService.getStatusForm(workProgrammeActivity)).thenReturn(form);

    mockMvc.perform(
            get(ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
                .renderStatusUpdatePage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/updateWorkProgrammeActivityStatus"))
        .andExpect(model().attribute("form", form))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION))
        .andExpect(model().attribute("summaryView", WorkProgrammeActivitySummaryView.fromWorkProgrammeActivity(workProgrammeActivity)))
        .andExpect(model().attribute("statusRadioOptions", WorkProgrammeStatus.getRadioOptions()))
        .andExpect(model().attribute("licenceSearchUrl", SearchSelectorService.route(on(LicenceInternalApiRestController.class)
            .searchLicencesByReferenceAndType(licence.getType().getUrlSlug(), null)))
        )
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)))
        );
  }

  @SecurityTest
  void submitStatusUpdatePage() throws Exception {
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityStatusValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
                .renderStatusUpdatePage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());
  }

  @SecurityTest
  void submitStatusUpdatePage_invalidForm() throws Exception {
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(workProgrammeActivityStatusValidator.isValid(any(), any())).thenReturn(false);

    mockMvc.perform(
            post(ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
                .renderStatusUpdatePage(workProgrammeActivity.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/updateWorkProgrammeActivityStatus"))
        .andExpect(model().attribute("pageCaption", PAGE_CAPTION))
        .andExpect(model().attribute("summaryView", WorkProgrammeActivitySummaryView.fromWorkProgrammeActivity(workProgrammeActivity)))
        .andExpect(model().attribute("statusRadioOptions", WorkProgrammeStatus.getRadioOptions()))
        .andExpect(model().attribute("licenceSearchUrl", SearchSelectorService.route(on(LicenceInternalApiRestController.class)
            .searchLicencesByReferenceAndType(licence.getType().getUrlSlug(), null)))
        )
        .andExpect(model().attribute("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null)))
        );
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceScheduleDetailDuplicationController.class)
class LicenceScheduleDetailDuplicationControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService;

  private final Licence licence = LicenceTestUtil.builder()
      .withId(1)
      .build();

  @Test
  void renderCreateDraftScheduleUpdatePage() throws Exception {
    var pageCaption = "pageCaption";

    when(licenceActionService.getAvailableUserActionItems(licence, regulatorUser))
        .thenReturn(List.of(new ActionItemView(
            "Update licence schedule",
            2,
            false,
            ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).renderCreateDraftScheduleUpdatePage(licence.getId(), null)),
            null)));

    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceService.getLicencePageCaption(licence)).thenReturn(pageCaption);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).renderCreateDraftScheduleUpdatePage(licence.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/schedule/startScheduleUpdateJourney"))
        .andExpect(model().attribute("pageTitle", "Update an existing licence schedule"))
        .andExpect(model().attribute("pageCaption", pageCaption))
        .andExpect(model().attribute("startUrl",
            ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).createDraftScheduleUpdateAndRedirect(licence.getId(), null))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(licence.getId(), null, null, null))));
  }

  @Test
  void renderCreateDraftScheduleUpdatePage_noAuth() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceActionService.getAvailableUserActionItems(licence, regulatorUser))
        .thenReturn(List.of(new ActionItemView("test", 1, false, "test", null)));

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).renderCreateDraftScheduleUpdatePage(licence.getId(), null)))
                .with(user(regulatorUser))
        )
        .andExpect(status().isForbidden());
  }

  @Test
  void createDraftScheduleUpdateAndRedirect() throws Exception {
    when(licenceActionService.getAvailableUserActionItems(licence, regulatorUser))
        .thenReturn(List.of(new ActionItemView(
            "Update licence schedule",
            2,
            false,
            ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).renderCreateDraftScheduleUpdatePage(licence.getId(), null)),
            null)));

    var oldDetail = new LicenceScheduleDetail();
    var newDetail = new LicenceScheduleDetail();
    newDetail.setId(UUID.randomUUID());

    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(oldDetail);
    when(licenceScheduleDetailDuplicationService.createNewDraftLicenceScheduleDetailVersion(oldDetail))
        .thenReturn(newDetail);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).createDraftScheduleUpdateAndRedirect(licence.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void createDraftScheduleUpdateAndRedirect_noAuth() throws Exception {
    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);
    when(licenceActionService.getAvailableUserActionItems(licence, regulatorUser))
        .thenReturn(List.of(new ActionItemView("test", 1, false, "test", null)));

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class).createDraftScheduleUpdateAndRedirect(licence.getId(), null)))
                .with(user(regulatorUser))
                .with(csrf())
        )
        .andExpect(status().isForbidden());
  }
}
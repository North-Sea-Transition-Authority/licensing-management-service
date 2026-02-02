package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

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
import static uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam.LicenceResponsibleTeamController.PAGE_TITLE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@ContextConfiguration(classes = LicenceResponsibleTeamController.class)
class LicenceResponsibleTeamControllerTest extends AbstractControllerTest {
  @MockitoBean
  private LicenceResponsibleTeamService licenceResponsibleTeamService;

  @MockitoBean
  private LicenceResponsibleTeamValidator licenceResponsibleTeamValidator;

  private static final Long ORGANISATION_USER_WUA_ID = 2L;
  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();
  
  @Test
  void render() throws Exception {
    var licence = createLicence("CS1", LicenceType.CARBON_STORAGE);

    mockSatisfiesActionItem(licence, LicenceActionItem.MANAGE_RESPONSIBLE_TEAM);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceResponsibleTeamService.getLicenceResponsibleTeamForm(licence)).thenReturn(new LicenceResponsibleTeamForm());

    var resultActions = mockMvc.perform(
            get(ReverseRouter.route(on(LicenceResponsibleTeamController.class).render(1, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk());

    assertStandardModelAttributesArePresent(resultActions, licence);
  }

  @SecurityTest
  void render_noAuth() throws Exception {
    var licence = createLicence("CS1", LicenceType.CARBON_STORAGE);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceResponsibleTeamService.getLicenceResponsibleTeamForm(licence)).thenReturn(new LicenceResponsibleTeamForm());

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceResponsibleTeamController.class).render(1, null)))
                .with(user(USER))
        )
        .andExpect(status().isForbidden());
  }


  @Test
  void save_formIsValid() throws Exception {
    var licence = createLicence("CS1", LicenceType.CARBON_STORAGE);

    mockSatisfiesActionItem(licence, LicenceActionItem.MANAGE_RESPONSIBLE_TEAM);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceResponsibleTeamValidator.isValid(any(), any())).thenReturn(true);

    mockMvc.perform(
            post(ReverseRouter.route(on(LicenceResponsibleTeamController.class).save(1, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void save_formIsNotValid() throws Exception {
    var licence = createLicence("CS1", LicenceType.CARBON_STORAGE);

    mockSatisfiesActionItem(licence, LicenceActionItem.MANAGE_RESPONSIBLE_TEAM);

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceResponsibleTeamService.getLicenceResponsibleTeamForm(licence)).thenReturn(new LicenceResponsibleTeamForm());
    when(licenceResponsibleTeamValidator.isValid(any(), any())).thenReturn(false);

    var resultActions = mockMvc.perform(
            post(ReverseRouter.route(on(LicenceResponsibleTeamController.class).save(1, null, null, null)))
                .with(user(USER))
                .with(csrf())
        )
        .andExpect(status().isOk());

    assertStandardModelAttributesArePresent(resultActions, licence);
  }

  private void assertStandardModelAttributesArePresent(ResultActions resultActions, Licence licence) throws Exception {
    resultActions
        .andExpect(view().name("lms/licence/manageResponsibleTeam"))
        .andExpect(model().attribute("pageTitle", PAGE_TITLE))
        .andExpect(model().attribute("caption", licence.getType().getDisplayName()))
        .andExpect(model().attribute("responsibleTeamOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(LicenceTeam.fromTeamType(licence.getType()))))
        .andExpect(model().attribute("backUrl",
            ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(licence.getId(), null, null))));
  }

  private void mockSatisfiesActionItem(Licence licence, LicenceActionItem licenceActionItem) {
    when(licenceActionService.getAvailableUserActionItems(licence, USER))
        .thenReturn(List.of(licenceActionItem.toActionItemView(licence)));
  }

  private Licence createLicence(String ref, LicenceType licenceType) {
    var licence = new Licence();
    licence.setLicenceReference(ref);
    licence.setType(licenceType);
    return licence;
  }
}
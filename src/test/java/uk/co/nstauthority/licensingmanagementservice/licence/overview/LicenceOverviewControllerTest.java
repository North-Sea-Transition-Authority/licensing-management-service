package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.SecurityTest;

@ContextConfiguration(classes = LicenceOverviewController.class)
class LicenceOverviewControllerTest extends AbstractControllerTest {
  private static final Long ORGANISATION_USER_WUA_ID = 2L;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(ORGANISATION_USER_WUA_ID)
      .build();

  @SecurityTest
  void renderLicenceOverview() throws Exception {
    var licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setLicenceReference("CS1");

    when(licenceService.findLicenceByIdOrThrow(licence.getId())).thenReturn(licence);

    var actions = List.of(LicenceActionItem.MANAGE_LICENSEES.toActionItemView(licence));

    when(licenceActionService.getAvailableUserActionItems(licence, USER)).thenReturn(actions);

    mockMvc.perform(
            get(ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(licence.getId(), null, null)))
                .with(user(USER))
        )
        .andExpect(status().isOk())
        .andExpect(view().name("lms/licence/licenceOverview"))
        .andExpect(model().attribute("licenceReference", licence.getLicenceReference()))
        .andExpect(model().attribute("caption", licence.getType().getDisplayName()))
        .andExpect(model().attribute("licenceActions", actions));
  }
}
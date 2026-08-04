package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.AdministratorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RemoveAdministratorChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class RemoveAdministratorChangeControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final String PAGE_TITLE = "Are you sure you want to remove this licence administrator change?";
  private static final String VIEW_NAME = "lms/licence/correction/change/removeAdministratorChange";

  private final String positionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));

  @Test
  void renderRemoveExecutedAdminChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .renderRemoveExecutedAdminChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedAdminChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .renderRemoveExecutedAdminChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderRemoveExecutedAdminChange_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionViewService.getAdministratorChangeContext(correction, POSITION_ID))
        .thenReturn(new AdministratorChangeContext(789, 456, "Joining Admin Org", "Withdrawing Admin Org"));

    mockMvc.perform(get(ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .renderRemoveExecutedAdminChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("withdrawingAdministratorName", "Withdrawing Admin Org"),
            model().attribute("joiningAdministratorName", "Joining Admin Org"),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void removeAdministratorChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .removeAdministratorChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void removeAdministratorChange_whenEligible() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);

    mockMvc.perform(post(ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .removeAdministratorChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change removed")
                .build())
        );

    verify(licencePositionCorrectionService).removeExistingAdministratorChange(position, correction, CHANGE_ID);
  }

  @Test
  void removeAdministratorChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveAdministratorChangeController.class)
            .removeAdministratorChange(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(licencePositionCorrectionService);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}

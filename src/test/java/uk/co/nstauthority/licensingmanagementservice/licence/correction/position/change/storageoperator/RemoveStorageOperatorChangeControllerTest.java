package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RemoveStorageOperatorChangeController.class)
@ActiveProfiles("test")
class RemoveStorageOperatorChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private StorageOperatorChangeService storageOperatorChangeService;

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this storage operator change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeStorageOperatorChange";

  private static final Licence LICENCE = LicenceTestUtil.builder().withId(1).build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePositionChange CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(CHANGE_ID))
      .build();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();

  private final String positionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));

  @Test
  void renderRemoveExecutedStorageOperatorChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(RemoveStorageOperatorChangeController.class)
            .renderRemoveExecutedStorageOperatorChange(CORRECTION, POSITION, CHANGE))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedStorageOperatorChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemoveStorageOperatorChangeController.class)
            .renderRemoveExecutedStorageOperatorChange(CORRECTION, POSITION, CHANGE)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void removeStorageOperatorChange_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemoveStorageOperatorChangeController.class)
            .removeStorageOperatorChange(CORRECTION, POSITION, CHANGE, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void removeStorageOperatorChange_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(RemoveStorageOperatorChangeController.class)
            .removeStorageOperatorChange(CORRECTION, POSITION, CHANGE, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(storageOperatorChangeService);
  }

  @Test
  void renderRemoveExecutedStorageOperatorChange_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(CHANGE.getId())).thenReturn(Optional.of(CHANGE));
    when(licencePositionViewService.getStorageOperatorChangeContext(correction, POSITION_ID))
        .thenReturn(new StorageOperatorChangeContext(789, 456, "Joining Storage Operator Org", "Withdrawing Storage Operator Org"));

    mockMvc.perform(get(ReverseRouter.route(on(RemoveStorageOperatorChangeController.class)
            .renderRemoveExecutedStorageOperatorChange(correction, position, CHANGE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(CONFIRMATION_TEMPLATE),
            model().attribute("pageTitle", REMOVE_PAGE_TITLE),
            model().attribute("primaryButton", Button.warningButton("Remove storage operator change")),
            model().attribute("withdrawingStorageOperatorName", "Withdrawing Storage Operator Org"),
            model().attribute("joiningStorageOperatorName", "Joining Storage Operator Org"),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void removeStorageOperatorChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(CHANGE.getId())).thenReturn(Optional.of(CHANGE));

    mockMvc.perform(post(ReverseRouter.route(on(RemoveStorageOperatorChangeController.class)
            .removeStorageOperatorChange(correction, position, CHANGE, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Storage operator change removed")
                .build())
        );

    verify(storageOperatorChangeService).removeExistingStorageOperatorChange(position, correction, CHANGE_ID);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}
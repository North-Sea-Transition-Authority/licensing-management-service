package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionAdministratorChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionAdministratorChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private AdministratorChangeFormValidator administratorChangeFormValidator;

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final Integer ADMINISTRATOR_ID = 123;
  private static final Integer PREVIOUS_ADMINISTRATOR_ID = 456;
  private static final Integer CURRENT_ADMINISTRATOR_ID = 789;
  private static final String PAGE_TITLE = "Change licence administrator";
  private static final String VIEW_NAME = "lms/licence/correction/change/administratorChange";

  private final String executedBackLinkUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));

  private final String addedBackLinkUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));

  @Test
  void renderForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, position.getId())).thenReturn(ADMINISTRATOR_ID);
    when(organisationUnitQueryService.getOrganisationUnitNameById(ADMINISTRATOR_ID))
        .thenReturn(Optional.of("Current Admin Org"));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("backLinkUrl", executedBackLinkUrl),
            model().attribute("previousLicenceAdministratorName", "Current Admin Org"),
            model().attributeExists("organisationUnitsUrl")
        );
  }

  @Test
  void submitForExecutedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();
    form.getAdminId().setInputValue(ADMINISTRATOR_ID.toString());

    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(CURRENT_ADMINISTRATOR_ID);
    when(administratorChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(CURRENT_ADMINISTRATOR_ID))).thenReturn(false);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, position.getId())).thenReturn(1);
    when(administratorChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(1))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedBackLinkUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change added")
                .build())
        );

    verify(licencePositionCorrectionService)
        .addAdministratorChangeForExistingLicencePosition(position, correction, ADMINISTRATOR_ID);
  }

  @Test
  void submitForExecutedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();

    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(CURRENT_ADMINISTRATOR_ID);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(administratorChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(CURRENT_ADMINISTRATOR_ID))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("backLinkUrl", executedBackLinkUrl)
        );

    verifyNoInteractions(licencePositionCorrectionService);
  }

  @Test
  void renderForAddedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();

    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("backLinkUrl", addedBackLinkUrl),
            model().attribute("previousLicenceAdministratorName", ""),
            model().attributeExists("organisationUnitsUrl")
        );
  }

  @Test
  void submitForAddedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var form = new AdministratorChangeForm();
    form.getAdminId().setInputValue(ADMINISTRATOR_ID.toString());

    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(CURRENT_ADMINISTRATOR_ID);
    when(administratorChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(CURRENT_ADMINISTRATOR_ID))).thenReturn(false);
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(addedBackLinkUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change added")
                .build())
        );

    verify(licencePositionCorrectionService)
        .addAdministratorChangeForAddedLicencePosition(positionCorrection, ADMINISTRATOR_ID);
  }

  @Test
  void submitForAddedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var form = new AdministratorChangeForm();

    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(CURRENT_ADMINISTRATOR_ID);
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(administratorChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(CURRENT_ADMINISTRATOR_ID))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("backLinkUrl", addedBackLinkUrl)
        );

    verify(licencePositionCorrectionService, never())
        .addAdministratorChangeForAddedLicencePosition(any(), anyInt());
  }

  @Test
  void renderForCorrectingChange_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var changeId = UUID.randomUUID().toString();

    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(ADMINISTRATOR_ID);
    when(licencePositionService.getPreviousAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(PREVIOUS_ADMINISTRATOR_ID);
    when(organisationUnitQueryService.getOrganisationUnitNameById(PREVIOUS_ADMINISTRATOR_ID))
        .thenReturn(Optional.of("Previous Admin Org"));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForCorrectingChange(CORRECTION_ID, POSITION_ID, changeId, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("backLinkUrl", executedBackLinkUrl),
            model().attribute("previousLicenceAdministratorName", "Previous Admin Org"),
            model().attributeExists("organisationUnitsUrl")
        );
  }

  @Test
  void submitForCorrectingChange_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var changeId = UUID.randomUUID().toString();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();
    form.getAdminId().setInputValue(ADMINISTRATOR_ID.toString());

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(CURRENT_ADMINISTRATOR_ID);
    when(licencePositionService.getPreviousAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(PREVIOUS_ADMINISTRATOR_ID);
    when(administratorChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(CURRENT_ADMINISTRATOR_ID), eq(PREVIOUS_ADMINISTRATOR_ID)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, changeId, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedBackLinkUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change corrected")
                .build())
        );

    verify(licencePositionCorrectionService)
        .correctExistingAdministratorChange(position, correction, changeId, ADMINISTRATOR_ID);
  }

  @Test
  void submitForCorrectingChange_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var changeId = UUID.randomUUID().toString();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.getCurrentAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(CURRENT_ADMINISTRATOR_ID);
    when(licencePositionService.getPreviousAdministratorIdForCorrection(correction, POSITION_ID))
        .thenReturn(PREVIOUS_ADMINISTRATOR_ID);
    when(administratorChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(CURRENT_ADMINISTRATOR_ID), eq(PREVIOUS_ADMINISTRATOR_ID))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, changeId, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("backLinkUrl", executedBackLinkUrl)
        );

    verify(licencePositionCorrectionService, never())
        .correctExistingAdministratorChange(any(), any(), any(), anyInt());
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

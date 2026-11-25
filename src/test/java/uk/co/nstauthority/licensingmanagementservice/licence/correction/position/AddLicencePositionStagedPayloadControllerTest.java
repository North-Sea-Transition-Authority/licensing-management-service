package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrectionInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = AddLicencePositionStagedPayloadController.class)
@ActiveProfiles("test")
class AddLicencePositionStagedPayloadControllerTest extends AbstractControllerTest {

  @MockitoBean
  private AddLicencePositionStagedPayloadFormValidator addLicencePositionStagedPayloadValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String REGULATOR_REFERENCE = "TEST-REF";
  private static final String PAGE_TITLE = "Add a position";
  private static final String VIEW_NAME = "lms/licence/correction/addPosition";

  private final String backLinkUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION, null));

  @Test
  void renderAddLicencePositionStagedPayload_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .renderAddLicencePositionStagedPayload(CORRECTION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderAddLicencePositionStagedPayload_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .renderAddLicencePositionStagedPayload(correction)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderAddLicencePositionStagedPayload_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .renderAddLicencePositionStagedPayload(CORRECTION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("backLinkUrl", backLinkUrl)
        );
  }

  @Test
  void renderAddLicencePositionStagedPayload_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .renderAddLicencePositionStagedPayload(CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );
  }

  @Test
  void addLicencePositionStagedPayload_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .addLicencePositionStagedPayload(CORRECTION, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void addLicencePositionStagedPayload_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .addLicencePositionStagedPayload(correction, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void addLicencePositionStagedPayload_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var form = new AddLicencePositionStagedPayloadForm();
    form.getCorrectionReference().setInputValue(REGULATOR_REFERENCE);
    form.getPositionDate().setDate(POSITION_DATE);

    var addedPositionId = UUID.randomUUID();
    when(addLicencePositionStagedPayloadValidator.hasErrors(eq(form), eq(correction), any(BindingResult.class)))
        .thenReturn(false);
    when(licencePositionStagedPayloadService.addNewPosition(correction, POSITION_DATE, REGULATOR_REFERENCE))
        .thenReturn(addedPositionId);

    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .addLicencePositionStagedPayload(CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, addedPositionId)),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence correction position added")
                .build())
        );
  }

  @Test
  void addLicencePositionStagedPayload_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var form = new AddLicencePositionStagedPayloadForm();

    when(addLicencePositionStagedPayloadValidator.hasErrors(eq(form), eq(correction), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .addLicencePositionStagedPayload(CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("form", form),
            model().attribute("backLinkUrl", backLinkUrl)
        );

    verifyNoInteractions(licencePositionStagedPayloadService);
  }

  @Test
  void addLicencePositionStagedPayload_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionStagedPayloadController.class)
            .addLicencePositionStagedPayload(CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );

    verifyNoInteractions(addLicencePositionStagedPayloadValidator);
    verifyNoInteractions(licencePositionStagedPayloadService);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
    return CORRECTION;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = AddLicencePositionCorrectionController.class)
@ActiveProfiles({"test", "enable-lms2"})
class AddLicencePositionCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private AddLicencePositionCorrectionFormValidator addLicencePositionCorrectionValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String REGULATOR_REFERENCE = "TEST-REF";
  private static final String PAGE_TITLE = "Add a position";
  private static final String VIEW_NAME = "lms/licence/correction/addPosition";

  private final String backLinkUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION_ID, null));

  @Test
  void renderAddLicencePositionCorrection_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .renderAddLicencePositionCorrection(CORRECTION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderAddLicencePositionCorrection_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .renderAddLicencePositionCorrection(CORRECTION_ID, null)))
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
  void renderAddLicencePositionCorrection_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .renderAddLicencePositionCorrection(CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void addLicencePositionCorrection_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .addLicencePositionCorrection(CORRECTION_ID, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void addLicencePositionCorrection_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var form = new AddLicencePositionCorrectionForm();
    form.getCorrectionReference().setInputValue(REGULATOR_REFERENCE);
    form.getPositionDate().setDate(POSITION_DATE);

    when(addLicencePositionCorrectionValidator.hasErrors(eq(form), eq(correction), any(BindingResult.class)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .addLicencePositionCorrection(CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(backLinkUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence correction position added")
                .build())
        );

    verify(licencePositionCorrectionService).addNewPosition(correction, POSITION_DATE, REGULATOR_REFERENCE);
  }

  @Test
  void addLicencePositionCorrection_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var form = new AddLicencePositionCorrectionForm();

    when(addLicencePositionCorrectionValidator.hasErrors(eq(form), eq(correction), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .addLicencePositionCorrection(CORRECTION_ID, null, null, null, null)))
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

    verifyNoInteractions(licencePositionCorrectionService);
  }

  @Test
  void addLicencePositionCorrection_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
            .addLicencePositionCorrection(CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(addLicencePositionCorrectionValidator);
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
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = CorrectPositionDateController.class)
@ActiveProfiles({"test", "enable-lms2"})
class CorrectPositionDateControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectPositionDateFormValidator correctPositionDateFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 1);
  private static final String PAGE_TITLE = "Correct the date of a licence position";
  private static final String VIEW_NAME = "lms/licence/correction/correctPositionCorrectionDate";
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();


  private final String backLinkUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderCorrection(CORRECTION_ID, null));

  @Test
  void renderCorrectPositionDate_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionDateController.class)
            .renderCorrectLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrectPositionDate_whenAllocatedToUser() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);

    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionDateController.class)
            .renderCorrectLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("currentPositionDate", POSITION.getFormattedPositionDate()),
            model().attribute("regulatorReference", POSITION.getLicenceTransaction().getRegulatorReference()),
            model().attribute("backLinkUrl", backLinkUrl)
        );
  }

  @Test
  void renderCorrectPositionDate_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(CorrectPositionDateController.class)
            .renderCorrectLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void correctPositionDate_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionDateController.class)
            .correctLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void correctPositionDate_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var form = new CorrectPositionDateForm();
    form.getCorrectPositionDate().setDate(POSITION_DATE);

    when(correctPositionDateFormValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionDateController.class)
            .correctLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(backLinkUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence position correction date updated")
                .build())
        );

    verify(licencePositionCorrectionService).correctPositionDate(correction, POSITION, POSITION_DATE);
  }

  @Test
  void correctPositionDate_whenInvalid() throws Exception {
    givenCorrectionAllocatedToUser();
    var form = new CorrectPositionDateForm();

    when(correctPositionDateFormValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionDateController.class)
            .correctLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("form", form),
            model().attribute("currentPositionDate", POSITION.getFormattedPositionDate()),
            model().attribute("regulatorReference", POSITION.getLicenceTransaction().getRegulatorReference()),
            model().attribute("backLinkUrl", backLinkUrl)
        );

    verifyNoInteractions(licencePositionCorrectionService);
  }

  @Test
  void correctPositionDate_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(CorrectPositionDateController.class)
            .correctLicencePositionCorrectionDate(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(correctPositionDateFormValidator);
    verifyNoInteractions(licencePositionService);
    verifyNoInteractions(licencePositionCorrectionService);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    return CORRECTION;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}
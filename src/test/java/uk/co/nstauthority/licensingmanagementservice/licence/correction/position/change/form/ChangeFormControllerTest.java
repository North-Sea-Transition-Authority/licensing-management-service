package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = ChangeFormController.class)
@ActiveProfiles("test")
class ChangeFormControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String TEMPLATE_NAME = "lms/licence/correction/change/administratorChange";
  private static final AddPositionChangeType CHANGE_TYPE = AddPositionChangeType.ADMINISTRATOR;
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final CorrectionPosition EXECUTED_POSITION = new CorrectionPosition.Executed(CORRECTION, POSITION);
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.randomUUID())
      .withLicencePosition(POSITION)
      .build();

  @MockitoBean
  private ChangeFormService changeFormService;

  @SuppressWarnings("unchecked")
  private final ChangeFormHandler<AdministratorChangeForm> handler = mock(ChangeFormHandler.class);

  private final String positionUrl =
      LicencePositionCorrectionRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId());

  @Test
  void addToPosition_whenNotLoggedIn_thenRedirectsToLogin() throws Exception {
    mockMvc.perform(get(addToPositionUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void addToPosition_whenNotAllocatedToUser_thenForbidden() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(addToPositionUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(changeFormService);
  }

  @Test
  void addToPosition_whenCorrectionIsNotInProgress_thenForbidden() throws Exception {
    var completeCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(completeCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(completeCorrection));

    mockMvc.perform(get(addToPositionUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)));

    verifyNoInteractions(changeFormService);
  }

  @Test
  void addToPosition_whenPositionIsRemovedInCorrection_thenForbidden() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(licencePositionCorrectionService.isPositionRemovedInCorrection(CORRECTION, POSITION)).thenReturn(true);

    mockMvc.perform(get(addToPositionUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(changeFormService);
  }

  @ParameterizedTest
  @EnumSource(value = HttpStatus.class, names = {"NOT_FOUND", "FORBIDDEN"})
  void addToPosition_whenChangeTypeHasNoAvailableForm_thenRespondsWithServiceStatus(HttpStatus httpStatus)
      throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    when(changeFormService.getHandler(CORRECTION, CHANGE_TYPE)).thenThrow(new ResponseStatusException(httpStatus));

    mockMvc.perform(get(addToPositionUrl())
            .with(user(regulatorUser)))
        .andExpect(status().is(httpStatus.value()));
  }

  @Test
  void addToPosition_whenPositionHasLiveChangeOfType_thenConflict() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    givenHandlerForChangeType();
    doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
        .when(changeFormService).checkNoLiveChangeOfType(handler, EXECUTED_POSITION);

    mockMvc.perform(get(addToPositionUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isConflict());

    verify(handler, never()).formPage(any());
  }

  @Test
  void addToPosition_whenGet_thenShowsFormPage() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    givenHandlerForChangeType();
    var target = new ChangeFormTarget.NewChange(EXECUTED_POSITION);
    when(handler.formPage(target)).thenReturn(formPage(new AdministratorChangeForm()));

    mockMvc.perform(get(addToPositionUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(TEMPLATE_NAME),
            model().attribute("cancelUrl", positionUrl)
        );

    verify(handler, never()).save(any(), any());
  }

  @Test
  void addToPosition_whenPostIsInvalid_thenShowsFormPageWithErrors() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    givenHandlerForChangeType();
    var target = new ChangeFormTarget.NewChange(EXECUTED_POSITION);
    var form = new AdministratorChangeForm();
    when(handler.newForm()).thenReturn(form);
    when(handler.formPageIfInvalid(eq(target), eq(form), any(BindingResult.class))).thenAnswer(invocation -> {
      invocation.<BindingResult>getArgument(2).rejectValue("id.inputValue", "required", "Select an administrator");
      return Optional.of(formPage(form));
    });

    mockMvc.perform(post(addToPositionUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(TEMPLATE_NAME),
            model().attribute("cancelUrl", positionUrl),
            model().attributeHasFieldErrors(ChangeFormController.FORM_NAME, "id.inputValue")
        );

    verify(handler, never()).save(any(), any());
  }

  @Test
  void addToPosition_whenPostIsValid_thenSavesBoundFormAndRedirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPosition();
    givenHandlerForChangeType();
    var target = new ChangeFormTarget.NewChange(EXECUTED_POSITION);
    var form = new AdministratorChangeForm();
    when(handler.newForm()).thenReturn(form);
    when(handler.formPageIfInvalid(eq(target), eq(form), any(BindingResult.class))).thenReturn(Optional.empty());
    when(handler.displayName()).thenReturn("Licence administrator change");

    mockMvc.perform(post(addToPositionUrl())
            .param("id.inputValue", " 123 ")
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change added")
                .build())
        );

    verify(handler).save(target, form);
    assertThat(form.getId().getInputValue()).isEqualTo("123");
  }

  @Test
  void correctLiveChange_whenNotAllocatedToUser_thenForbidden() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(correctLiveChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(changeFormService);
  }

  @Test
  void correctLiveChange_whenChangeHasNoForm_thenNotFound() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    when(changeFormService.getHandler(CORRECTION, LIVE_CHANGE))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(get(correctLiveChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void correctLiveChange_whenGet_thenShowsFormPage() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    givenHandlerForLiveChange();
    var target = new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE);
    when(handler.formPage(target)).thenReturn(formPage(new AdministratorChangeForm()));

    mockMvc.perform(get(correctLiveChangeUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(TEMPLATE_NAME),
            model().attribute("cancelUrl", positionUrl)
        );
  }

  @Test
  void correctLiveChange_whenPostIsValid_thenSavesAndRedirectsWithCorrectedBanner() throws Exception {
    givenCorrectionAllocatedToUser();
    givenLiveChange();
    givenHandlerForLiveChange();
    var target = new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE);
    var form = new AdministratorChangeForm();
    when(handler.newForm()).thenReturn(form);
    when(handler.formPageIfInvalid(eq(target), eq(form), any(BindingResult.class))).thenReturn(Optional.empty());
    when(handler.displayName()).thenReturn("Licence administrator change");

    mockMvc.perform(post(correctLiveChangeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change corrected")
                .build())
        );

    verify(handler).save(target, form);
  }

  private ModelAndView formPage(AdministratorChangeForm form) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("pageTitle", "Change licence administrator")
        .addObject("form", form)
        .addObject("previousLicenceAdministratorName", "Previous administrator")
        .addObject("preselectedAdministrator", Map.of())
        .addObject("organisationUnitsUrl", "/organisation-units");
  }

  private String addToPositionUrl() {
    return ReverseRouter.route(on(ChangeFormController.class)
        .addToPosition(EXECUTED_POSITION, CHANGE_TYPE, null, null, null));
  }

  private String correctLiveChangeUrl() {
    return ReverseRouter.route(on(ChangeFormController.class)
        .correctLiveChange(CORRECTION, POSITION, LIVE_CHANGE, null, null, null));
  }

  private void givenHandlerForChangeType() {
    when(changeFormService.getHandler(CORRECTION, CHANGE_TYPE)).thenAnswer(invocation -> handler);
  }

  private void givenHandlerForLiveChange() {
    when(changeFormService.getHandler(CORRECTION, LIVE_CHANGE)).thenAnswer(invocation -> handler);
  }

  private void givenExecutedPosition() {
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    givenCorrectionPosition(EXECUTED_POSITION);
  }

  private void givenLiveChange() {
    givenExecutedPosition();
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}

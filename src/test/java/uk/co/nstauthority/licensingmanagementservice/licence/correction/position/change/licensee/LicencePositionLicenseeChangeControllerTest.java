package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceFormService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionLicenseeChangeController.class)
@ActiveProfiles("test")
class LicencePositionLicenseeChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicenceFormService licenceFormService;

  @MockitoBean
  private LicenseeChangeFormValidator licenseeChangeFormValidator;

  @MockitoBean
  private LicenseeChangeService licenseeChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .build();
  private static final List<Integer> CURRENT_JOINING_LICENSEES = List.of(4, 5, 6);
  private static final List<Integer> CURRENT_WITHDRAWING_LICENSEES = List.of(2, 3);
  private static final List<Integer> PREVIOUS_LICENSEES = List.of(1, 2, 3);
  private static final LicenseeChangeContext LICENSEE_CHANGE_CONTEXT = new LicenseeChangeContext(
      CURRENT_JOINING_LICENSEES, CURRENT_WITHDRAWING_LICENSEES, PREVIOUS_LICENSEES, List.of("licenseeOrg1", "licenseeOrg2"));
  private static final String PAGE_TITLE = "Change licensees";
  private static final String VIEW_NAME = "lms/licence/correction/change/licenseeChange";

  private final String executedCancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION));

  private final String addedCancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION));

  private final String executedRedirectUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION));

  private final String addedRedirectUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION));

  @Test
  void renderForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void submitForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForAddedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitForAddedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForAddedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void submitForAddedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_previousLicenseeNames_hasContent() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var previousLicenseeNames = LICENSEE_CHANGE_CONTEXT.previousLicenseeNames();

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(LICENSEE_CHANGE_CONTEXT);


    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
        .renderForExecutedPosition(CORRECTION, POSITION)))
        .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("previousLicenseeNames", LICENSEE_CHANGE_CONTEXT.previousLicenseeNames()),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("cancelUrl", executedCancelUrl),
            model().attributeExists("preselectedJoiningOrgUnits"),
            model().attributeExists("preselectedWithdrawingOrgUnits"),
            model().attribute("joiningOrganisationUnitSearchEndpoint",
                SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
                    .searchOrganisationUnitsWithConstraints(null, null, previousLicenseeNames))),
            model().attribute("withdrawingOrganisationUnitSearchEndpoint",
                SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
                .searchOrganisationUnitsWithConstraints(null, previousLicenseeNames, null)))
        );
  }
  @Test
  void renderForExecutedPosition_previousLicenseeNames_isEmpty() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var licenseeChangeContext = new LicenseeChangeContext(
        CURRENT_JOINING_LICENSEES,
        CURRENT_WITHDRAWING_LICENSEES,
        List.of(),
        List.of()
    );

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(licenseeChangeContext);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("joiningOrganisationUnitSearchEndpoint",
                SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))),
            model().attribute("withdrawingOrganisationUnitSearchEndpoint", ""));
  }

  @Test
  void renderForExecutedPosition_prepopulatesForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var expectedForm = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("4", "5", "6"))
        .setWithdrawingOrganisationIds(List.of("2", "3"));

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(LICENSEE_CHANGE_CONTEXT);

    var model = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andReturn()
        .getModelAndView()
        .getModel();

    assertThat(model.get("form")).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void submitForExecutedPosition_formInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var form = new LicenseeChangeForm();

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(LICENSEE_CHANGE_CONTEXT);
    when(licenseeChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(LICENSEE_CHANGE_CONTEXT.previousLicenseeIds())))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", executedCancelUrl)
        );
  }

  @Test
  void submitForExecutedPosition_formValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("4", "5", "6"))
        .setWithdrawingOrganisationIds(List.of("2", "3"));

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(
        LICENSEE_CHANGE_CONTEXT);
    when(licenseeChangeFormValidator.hasErrors(eq(form), any(BindingResult.class),
        eq(LICENSEE_CHANGE_CONTEXT.previousLicenseeIds())))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedRedirectUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licensee change added")
                .build())
        );
    verify(licenseeChangeService).addLicenseeChangeForExistingLicencePosition(
        position,
        correction,
        CURRENT_JOINING_LICENSEES,
        CURRENT_WITHDRAWING_LICENSEES
    );
  }

  @Test
  void renderForAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licenseeOperation = new LicenseeOperation(
        LicenseeOperation.LICENSEE_OPERATION_ID,
        CURRENT_JOINING_LICENSEES,
        CURRENT_WITHDRAWING_LICENSEES
    );
    var operation = LicencePositionChangeOperation.newLicencePositionAddOperation()
        .withOperationId(licenseeOperation.id())
        .withOperation(licenseeOperation)
        .build();
    var addChange = LicencePositionChangeType.addChange()
        .withChangeId(UUID.randomUUID().toString())
        .withChangeOrder(1)
        .withOperations(List.of(operation))
        .build();
    var existing = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload().
            withChanges(List.of(addChange))
            .withLicencePositionId(String.valueOf(POSITION_ID))
            .build())
        .build();
    var expectedForm = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("4", "5", "6"))
        .setWithdrawingOrganisationIds(List.of("2", "3"));
    var previousLicenseeNames = LICENSEE_CHANGE_CONTEXT.previousLicenseeNames();

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(existing));
    when(licencePositionViewService.getLicenseeChangeContext(eq(correction), any())).thenReturn(LICENSEE_CHANGE_CONTEXT);

    var model = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("previousLicenseeNames", previousLicenseeNames),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("cancelUrl", addedCancelUrl),
            model().attributeExists("preselectedJoiningOrgUnits"),
            model().attributeExists("preselectedWithdrawingOrgUnits"),
            model().attribute("joiningOrganisationUnitSearchEndpoint",
                SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
                    .searchOrganisationUnitsWithConstraints(null, null, previousLicenseeNames))),
            model().attribute("withdrawingOrganisationUnitSearchEndpoint",
                SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
                    .searchOrganisationUnitsWithConstraints(null, previousLicenseeNames, null)))
        )
        .andReturn()
        .getModelAndView()
        .getModel();

    assertThat(model.get("form")).usingRecursiveComparison().isEqualTo(expectedForm);
  }

  @Test
  void submitForAddedPosition_formInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withLicencePositionId(String.valueOf(POSITION_ID))
            .build())
        .build();
    var form = new LicenseeChangeForm();

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(licencePositionCorrection));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(LICENSEE_CHANGE_CONTEXT);
    when(licenseeChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(LICENSEE_CHANGE_CONTEXT.previousLicenseeIds())))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", addedCancelUrl)
        );
  }
  @Test
  void submitForAddedPosition_formValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withLicencePositionId(String.valueOf(POSITION_ID))
            .build())
        .build();
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("4", "5", "6"))
        .setWithdrawingOrganisationIds(List.of("2", "3"));

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(licencePositionCorrection));
    when(licencePositionViewService.getLicenseeChangeContext(correction, POSITION_ID)).thenReturn(LICENSEE_CHANGE_CONTEXT);
    when(licenseeChangeFormValidator.hasErrors(eq(form), any(BindingResult.class),eq(LICENSEE_CHANGE_CONTEXT.previousLicenseeIds())))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(addedRedirectUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licensee change added")
                .build())
        );

    verify(licenseeChangeService).addLicenseeChangeForAddedLicencePosition(
        licencePositionCorrection,
        CURRENT_JOINING_LICENSEES,
        CURRENT_WITHDRAWING_LICENSEES
    );
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
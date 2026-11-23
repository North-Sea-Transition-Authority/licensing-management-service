package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrectionInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.explorationoperator.ExplorationOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionExplorationOperatorChangeController.class)
@ActiveProfiles("test")
class LicencePositionExplorationOperatorChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @MockitoBean
  private OrganisationChangeFormValidator organisationChangeFormValidator;

  @MockitoBean
  private OrganisationChangeService organisationChangeService;
  
  private static final String VIEW_NAME = "lms/licence/correction/change/addOrganisationChange";
  private static final String OPERATOR_TYPE = "exploration operator";
  private static final String PAGE_TITLE = "Change exploration operator";
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.CARBON_STORAGE).build();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Integer PREVIOUS_EXPLORATION_OPERATOR_ID = 456;
  private static final Integer CURRENT_EXPLORATION_OPERATOR_ID = 789;
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
      .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
          .withLicencePositionId(POSITION_ID.toString())
          .build())
      .build();
  private static final String EXECUTED_CANCEL_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));
  private static final String EXECUTED_REDIRECT_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));
  private static final String ADDED_CANCEL_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null));
  private static final String ADDED_REDIRECT_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null));

  @Test
  void renderForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"CARBON_STORAGE"}, mode = EnumSource.Mode.EXCLUDE)
  void renderForExecutedPosition_whenNotCarbonStorageLicence_forbidden(LicenceType licenceType) throws Exception {
    var productionLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(productionLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForExecutedPosition(correction, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void submitForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"CARBON_STORAGE"}, mode = EnumSource.Mode.EXCLUDE)
  void submitForExecutedPosition_whenNotCarbonStorageLicence_forbidden(LicenceType licenceType) throws Exception {
    var productionLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(productionLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void submitForExecutedPosition_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForExecutedPosition(correction, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderForAddedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForAddedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"CARBON_STORAGE"}, mode = EnumSource.Mode.EXCLUDE)
  void renderForAddedPosition_whenNotCarbonStorageLicence_forbidden(LicenceType licenceType) throws Exception {
    var productionLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(productionLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }


  @Test
  void submitForAddedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitForAddedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"CARBON_STORAGE"}, mode = EnumSource.Mode.EXCLUDE)
  void submitForAddedPosition_whenNotCarbonStorageLicence_forbidden(LicenceType licenceType) throws Exception {
    var productionLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(productionLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var changeContext = new ExplorationOperatorChangeContext(
        CURRENT_EXPLORATION_OPERATOR_ID,
        PREVIOUS_EXPLORATION_OPERATOR_ID,
        "Current Exploration Operator Org",
        "Previous Exploration Operator Org"
    );
    var form = new ExplorationOperatorChangeForm(CURRENT_EXPLORATION_OPERATOR_ID.toString());

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(organisationChangeService.getOrganisationChangeContext(correction, position.getId(), ExplorationOperatorOperation.class))
        .thenReturn(changeContext);
    when(organisationChangeService.populateExistingOrganisationForm(eq(CURRENT_EXPLORATION_OPERATOR_ID), eq(correction), eq(position), any()))
        .thenReturn(form);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption(CURRENT_EXPLORATION_OPERATOR_ID.toString()))
        .thenReturn(Map.of(CURRENT_EXPLORATION_OPERATOR_ID.toString(),"Current Exploration Operator Org"));
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForExecutedPosition(correction, position)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("previousOperatorName", "Previous Exploration Operator Org"),
            model().attribute("form", form),
            model().attribute("cancelUrl", EXECUTED_CANCEL_URL),
            model().attribute("organisationUnitsUrl",
                SearchSelectorService.route(on(OrganisationUnitRestController.class)
                    .searchOrganisationUnits(null))),
            model().attributeExists("preselectedOperator"),
            content().string(containsString("Previous Exploration Operator Org")),
            content().string(containsString("Current Exploration Operator Org"))
        );
  }

  @Test
  void submitForExecutedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new ExplorationOperatorChangeForm();
    form.getId().setInputValue(CURRENT_EXPLORATION_OPERATOR_ID.toString());

    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_EXPLORATION_OPERATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(false);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, ExplorationOperatorOperation.class))
        .thenReturn(new ExplorationOperatorChangeContext(
            CURRENT_EXPLORATION_OPERATOR_ID,
            PREVIOUS_EXPLORATION_OPERATOR_ID,
            "Current Exploration Operator Org",
            "Previous Exploration Operator Org"
        ));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForExecutedPosition(correction, position, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(EXECUTED_REDIRECT_URL),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Exploration operator change added")
                .build())
        );

    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation()
        .withOperator(CURRENT_EXPLORATION_OPERATOR_ID)
        .build();
    verify(organisationChangeService).addOrganisationChangeForExistingLicencePosition(
        position,
        correction,
        ExplorationOperatorOperation.class,
        explorationOperatorOperation
    );
  }

  @Test
  void submitForExecutedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new ExplorationOperatorChangeForm();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, ExplorationOperatorOperation.class))
        .thenReturn(new ExplorationOperatorChangeContext(
            CURRENT_EXPLORATION_OPERATOR_ID,
            PREVIOUS_EXPLORATION_OPERATOR_ID,
            "",
            ""
        ));
    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_EXPLORATION_OPERATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForExecutedPosition(correction, position, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", EXECUTED_CANCEL_URL)
        );

    verify(organisationChangeService, never()).addOrganisationChangeForExistingLicencePosition(any(), any(), any(), any());
  }

  @Test
  void renderForAddedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var changeContext = new ExplorationOperatorChangeContext(
            CURRENT_EXPLORATION_OPERATOR_ID,
            PREVIOUS_EXPLORATION_OPERATOR_ID,
            "Current Exploration Operator Org",
            "Previous Exploration Operator Org"
        );
    var form = new ExplorationOperatorChangeForm(CURRENT_EXPLORATION_OPERATOR_ID.toString());

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(POSITION_CORRECTION));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, ExplorationOperatorOperation.class))
        .thenReturn(changeContext);
    when(organisationChangeService.populateAddedOrganisationForm(eq(CURRENT_EXPLORATION_OPERATOR_ID), eq(POSITION_CORRECTION), any()))
        .thenReturn(form);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption(CURRENT_EXPLORATION_OPERATOR_ID.toString()))
        .thenReturn(Map.of(CURRENT_EXPLORATION_OPERATOR_ID.toString(),"Current Exploration Operator Org"));
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .renderForAddedPosition(correction, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("previousOperatorName", "Previous Exploration Operator Org"),
            model().attribute("form", form),
            model().attribute("cancelUrl", ADDED_CANCEL_URL),
            model().attribute("organisationUnitsUrl",
                SearchSelectorService.route(on(OrganisationUnitRestController.class)
                    .searchOrganisationUnits(null))),
            model().attributeExists("preselectedOperator"),
            content().string(containsString("Previous Exploration Operator Org")),
            content().string(containsString("Current Exploration Operator Org"))
        );
  }

  @Test
  void submitForAddedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var form = new ExplorationOperatorChangeForm();
    form.getId().setInputValue(CURRENT_EXPLORATION_OPERATOR_ID.toString());

    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_EXPLORATION_OPERATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(false);
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(POSITION_CORRECTION));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, ExplorationOperatorOperation.class))
        .thenReturn(
            new ExplorationOperatorChangeContext(CURRENT_EXPLORATION_OPERATOR_ID, PREVIOUS_EXPLORATION_OPERATOR_ID, "", ""));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForAddedPosition(correction, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ADDED_REDIRECT_URL),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Exploration operator change added")
                .build())
        );
    var explorationOperatorOperation = LicenceOperation.newExplorationOperatorOperation()
        .withOperator(CURRENT_EXPLORATION_OPERATOR_ID)
        .build();
    verify(organisationChangeService).addOrganisationChangeForAddedLicencePosition(
        POSITION_CORRECTION,
        ExplorationOperatorOperation.class,
        explorationOperatorOperation
    );
  }

  @Test
  void submitForAddedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var form = new ExplorationOperatorChangeForm();

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(POSITION_CORRECTION));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, ExplorationOperatorOperation.class))
        .thenReturn(new ExplorationOperatorChangeContext(CURRENT_EXPLORATION_OPERATOR_ID, PREVIOUS_EXPLORATION_OPERATOR_ID, "", ""));
    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_EXPLORATION_OPERATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionExplorationOperatorChangeController.class)
            .submitForAddedPosition(correction, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", ADDED_CANCEL_URL)
        );

    verify(organisationChangeService, never()).addOrganisationChangeForAddedLicencePosition(any(), any(), any());
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
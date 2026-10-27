package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator.StorageOperatorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionStorageOperatorChangeController.class)
@ActiveProfiles("test")
class LicencePositionStorageOperatorChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @MockitoBean
  private StorageOperatorChangeFormValidator storageOperatorChangeFormValidator;

  @MockitoBean
  private StorageOperatorChangeService storageOperatorChangeService;

  private static final String VIEW_NAME = "lms/licence/correction/change/storageOperatorChange";
  private static final String PAGE_TITLE = "Change storage operator";
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.CARBON_STORAGE).build();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Integer PREVIOUS_STORAGE_OPERATOR_ID = 456;
  private static final Integer CURRENT_STORAGE_OPERATOR_ID = 789;
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
      .renderLicencePosition(CORRECTION, POSITION));
  private static final String EXECUTED_REDIRECT_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION));
  private static final String ADDED_CANCEL_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION));
  private static final String ADDED_REDIRECT_URL = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION));

  @Test
  void renderForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
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

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }


  @Test
  void submitForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
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

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForAddedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForAddedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
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

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }


  @Test
  void submitForAddedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void submitForAddedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
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

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var changeContext = new StorageOperatorChangeContext(
        CURRENT_STORAGE_OPERATOR_ID,
        PREVIOUS_STORAGE_OPERATOR_ID,
        "Current Storage Operator Org",
        "Previous Storage Operator Org"
    );

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getStorageOperatorChangeContext(correction, position.getId()))
        .thenReturn(changeContext);
    when(storageOperatorChangeService.populateExistingStorageOperatorForm(changeContext, correction, position))
        .thenReturn(new StorageOperatorChangeForm());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForExecutedPosition(correction, position)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("previousStorageOperatorName", "Previous Storage Operator Org"),
            model().attributeExists("form"),
            model().attribute("cancelUrl", EXECUTED_CANCEL_URL),
            model().attribute("organisationUnitsUrl",
                SearchSelectorService.route(on(OrganisationUnitRestController.class)
                    .searchOrganisationUnits(null))),
            model().attributeExists("preselectedStorageOperator")
        );
  }

  @Test
  void submitForExecutedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new StorageOperatorChangeForm();
    form.getOperatorId().setInputValue(CURRENT_STORAGE_OPERATOR_ID.toString());

    when(storageOperatorChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_STORAGE_OPERATOR_ID)))
        .thenReturn(false);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getStorageOperatorChangeContext(correction, POSITION_ID))
        .thenReturn(new StorageOperatorChangeContext(
            CURRENT_STORAGE_OPERATOR_ID,
            PREVIOUS_STORAGE_OPERATOR_ID,
            "Current Storage Operator Org",
            "Previous Storage Operator Org"
        ));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .submitForExecutedPosition(correction, position, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(EXECUTED_REDIRECT_URL),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Storage operator change added")
                .build())
        );

    verify(storageOperatorChangeService).addStorageOperatorChangeForExistingLicencePosition(
        position,
        correction,
        CURRENT_STORAGE_OPERATOR_ID
    );
  }

  @Test
  void submitForExecutedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new StorageOperatorChangeForm();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionViewService.getStorageOperatorChangeContext(correction, POSITION_ID))
        .thenReturn(new StorageOperatorChangeContext(
            CURRENT_STORAGE_OPERATOR_ID,
            PREVIOUS_STORAGE_OPERATOR_ID,
            "",
            ""));
    when(storageOperatorChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_STORAGE_OPERATOR_ID)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
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

    verifyNoInteractions(storageOperatorChangeService);
  }

  @Test
  void renderForAddedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var changeContext = new StorageOperatorChangeContext(
        CURRENT_STORAGE_OPERATOR_ID,
        PREVIOUS_STORAGE_OPERATOR_ID,
        "Current Storage Operator Org",
        "Previous Storage Operator Org"
    );

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(POSITION_CORRECTION));
    when(licencePositionViewService.getStorageOperatorChangeContext(eq(correction), any()))
        .thenReturn(changeContext);
    when(storageOperatorChangeService.populateAddedStorageOperatorForm(changeContext, POSITION_CORRECTION))
        .thenReturn(new StorageOperatorChangeForm());
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .renderForAddedPosition(correction, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("previousStorageOperatorName", "Previous Storage Operator Org"),
            model().attributeExists("form"),
            model().attribute("cancelUrl", ADDED_CANCEL_URL),
            model().attributeExists("organisationUnitsUrl"),
            model().attributeExists("preselectedStorageOperator")
        );
  }

  @Test
  void submitForAddedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();

    var form = new StorageOperatorChangeForm();
    form.getOperatorId().setInputValue(CURRENT_STORAGE_OPERATOR_ID.toString());

    when(storageOperatorChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_STORAGE_OPERATOR_ID)))
        .thenReturn(false);
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(POSITION_CORRECTION));
    when(licencePositionViewService.getStorageOperatorChangeContext(eq(correction), any()))
        .thenReturn(new StorageOperatorChangeContext(CURRENT_STORAGE_OPERATOR_ID, PREVIOUS_STORAGE_OPERATOR_ID, "", ""));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
            .submitForAddedPosition(correction, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ADDED_REDIRECT_URL),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Storage operator change added")
                .build())
        );

    verify(storageOperatorChangeService).addStorageOperatorChangeForAddedLicencePosition(POSITION_CORRECTION, CURRENT_STORAGE_OPERATOR_ID);
  }

  @Test
  void submitForAddedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var form = new StorageOperatorChangeForm();

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(POSITION_CORRECTION));
    when(licencePositionViewService.getStorageOperatorChangeContext(eq(correction), any()))
        .thenReturn(new StorageOperatorChangeContext(CURRENT_STORAGE_OPERATOR_ID, PREVIOUS_STORAGE_OPERATOR_ID, "", ""));
    when(storageOperatorChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_STORAGE_OPERATOR_ID)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionStorageOperatorChangeController.class)
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

    verifyNoInteractions(storageOperatorChangeService);
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
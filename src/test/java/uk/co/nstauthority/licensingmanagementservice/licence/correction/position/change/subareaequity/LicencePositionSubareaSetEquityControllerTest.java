package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionSubareaSetEquityController.class)
@ActiveProfiles("test")
class LicencePositionSubareaSetEquityControllerTest extends AbstractControllerTest {

  private static final String FORM_VIEW = "lms/licence/correction/setEquity";
  private static final String SUMMARY_VIEW = "lms/licence/correction/setEquitySummary";

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(10)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaOption SUBAREA_OPTION = new SubareaOption(
      FeatureTestUtil.builder().withId(BLOCK_FEATURE_ID).withFeatureName("30/1").build(),
      SUBAREA
  );
  private static final String SUBAREA_KEY = SUBAREA_OPTION.key();
  private static final String PAGE_CAPTION = "P/1 - Block 30/1 - Subarea A";

  @MockitoBean
  private LicencePositionSetEquityFormValidator validator;

  @MockitoBean
  private SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;

  @MockitoBean
  private SetEquityCorrectionService setEquityCorrectionService;

  @MockitoBean
  private SubareaEquityOptionsService subareaEquityOptionsService;

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Test
  void renderForExecutedPosition_whenSubareaGoesIntoChange_rendersFormWithSubareaCaption() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attributeExists("form", "licenseeOrgUnitUrl", "preselectedTransferTo"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForExecutedPosition(CORRECTION, POSITION))));
  }

  @Test
  void renderForExecutedPosition_whenSubareaUnknown_notFound() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(subareaEquityOptionsService.getSubareaOptionOrThrow(CORRECTION, POSITION, SUBAREA_KEY))
        .thenThrow(new LmsEntityNotFoundException("subarea", SUBAREA_KEY));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void submitForExecutedPosition_whenValid_addsOrganisationToSubareaAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    var committedOperation = subareaSetEquityOperation(1, 40);
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperationsForExecutedPosition(CORRECTION, POSITION, SUBAREA_OPTION))
        .thenReturn(List.of(committedOperation));

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of(1)))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY))));

    verify(subareaSetEquityCorrectionService).commitSubareaSetEquityForExecutedPosition(
        CORRECTION,
        POSITION,
        SUBAREA_OPTION,
        List.of(committedOperation, subareaSetEquityOperation(123, 60)));
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperationsForExecutedPosition(CORRECTION, POSITION, SUBAREA_OPTION))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForExecutedPosition(CORRECTION, POSITION))));

    verify(subareaSetEquityCorrectionService)
        .getCommittedSubareaSetEquityOperationsForExecutedPosition(CORRECTION, POSITION, SUBAREA_OPTION);
    verifyNoMoreInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void renderSummaryForExecutedPosition_rendersTheSubareasOrganisations() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    var committedOperations = List.of(subareaSetEquityOperation(1, 40), subareaSetEquityOperation(2, 60));
    var views = List.of(
        new SetEquityRow("Org One", BigDecimal.valueOf(40)),
        new SetEquityRow("Org Two", BigDecimal.valueOf(60)));
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperationsForExecutedPosition(CORRECTION, POSITION, SUBAREA_OPTION))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations)).thenReturn(views);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(SUMMARY_VIEW),
            model().attribute("pageTitle", "Add subarea equity"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("setEquityViews", views),
            model().attribute("totalEquity", BigDecimal.valueOf(100)),
            model().attribute("removeUrls", List.of(
                ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                    .removeForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY, 1)),
                ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                    .removeForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY, 2)))),
            model().attribute("addOrganisationUrl", ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                .renderForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY))),
            model().attribute("saveAndContinueUrl", ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                .submitSummaryForExecutedPosition(CORRECTION, POSITION))),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION, null))));
  }

  @Test
  void removeForExecutedPosition_removesTheOrganisationFromTheSubareaAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperationsForExecutedPosition(CORRECTION, POSITION, SUBAREA_OPTION))
        .thenReturn(List.of(subareaSetEquityOperation(1, 40), subareaSetEquityOperation(2, 60)));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .removeForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY, 1)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION, POSITION, SUBAREA_KEY))));

    verify(subareaSetEquityCorrectionService).commitSubareaSetEquityForExecutedPosition(
        CORRECTION, POSITION, SUBAREA_OPTION, List.of(subareaSetEquityOperation(2, 60)));
  }

  @Test
  void submitSummaryForExecutedPosition_redirectsToLicencePosition() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitSummaryForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION, POSITION, null))));

    verifyNoInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void renderForAddedPosition_whenSubareaGoesIntoChange_rendersFormWithSubareaCaption() throws Exception {
    givenCorrectionAllocatedToUser();
    givenAddedPositionWithSubarea();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attributeExists("form", "licenseeOrgUnitUrl", "preselectedTransferTo"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))));
  }

  @Test
  void submitForAddedPosition_whenValid_addsOrganisationToSubareaAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    givenAddedPositionWithSubarea();
    when(subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperations(POSITION_CORRECTION, SUBAREA_OPTION))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("100");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY))));

    verify(subareaSetEquityCorrectionService).commitSubareaSetEquity(
        POSITION_CORRECTION, SUBAREA_OPTION, List.of(subareaSetEquityOperation(123, 100)));
  }

  @Test
  void submitForAddedPosition_whenOrganisationAlreadyAddedToSubarea_rendersFormAndDoesNotPersist() throws Exception {
    givenCorrectionAllocatedToUser();
    givenAddedPositionWithSubarea();
    when(subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperations(POSITION_CORRECTION, SUBAREA_OPTION))
        .thenReturn(List.of(subareaSetEquityOperation(123, 40)));

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of(123)))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))));

    verify(subareaSetEquityCorrectionService).getCommittedSubareaSetEquityOperations(POSITION_CORRECTION, SUBAREA_OPTION);
    verifyNoMoreInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void renderSummaryForAddedPosition_rendersTheSubareasOrganisations() throws Exception {
    givenCorrectionAllocatedToUser();
    givenAddedPositionWithSubarea();
    var committedOperations = List.of(subareaSetEquityOperation(1, 40));
    var views = List.of(new SetEquityRow("Org One", BigDecimal.valueOf(40)));
    when(subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperations(POSITION_CORRECTION, SUBAREA_OPTION))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations)).thenReturn(views);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(SUMMARY_VIEW),
            model().attribute("pageTitle", "Add subarea equity"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("setEquityViews", views),
            model().attribute("totalEquity", BigDecimal.valueOf(40)),
            model().attribute("removeUrls", List.of(
                ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                    .removeForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY, 1)))),
            model().attribute("addOrganisationUrl", ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                .renderForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY))),
            model().attribute("saveAndContinueUrl", ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                .submitSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION))),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null))));
  }

  @Test
  void removeForAddedPosition_removesTheOrganisationFromTheSubareaAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    givenAddedPositionWithSubarea();
    when(subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperations(POSITION_CORRECTION, SUBAREA_OPTION))
        .thenReturn(List.of(subareaSetEquityOperation(1, 40), subareaSetEquityOperation(2, 60)));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .removeForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY, 2)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY))));

    verify(subareaSetEquityCorrectionService).commitSubareaSetEquity(
        POSITION_CORRECTION, SUBAREA_OPTION, List.of(subareaSetEquityOperation(1, 40)));
  }

  @Test
  void submitSummaryForAddedPosition_redirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null))));

    verifyNoInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void renderForAddedPosition_whenLicenceIsCarbonStorage_forbidden() throws Exception {
    var carbonStorageCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build())
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(carbonStorageCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(carbonStorageCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForAddedPosition(carbonStorageCorrection, POSITION_CORRECTION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaEquityOptionsService);
  }

  @Test
  void renderForAddedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION, SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaEquityOptionsService);
  }

  private void givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
  }

  private void givenExecutedPositionWithSubarea() {
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(subareaEquityOptionsService.getSubareaOptionOrThrow(CORRECTION, POSITION, SUBAREA_KEY)).thenReturn(SUBAREA_OPTION);
  }

  private void givenAddedPositionWithSubarea() {
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
    when(subareaEquityOptionsService.getSubareaOptionOrThrow(POSITION_CORRECTION, SUBAREA_KEY)).thenReturn(SUBAREA_OPTION);
  }

  private static SubareaSetEquityOperation subareaSetEquityOperation(int transferTo, int equity) {
    return new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, transferTo, BigDecimal.valueOf(equity));
  }
}
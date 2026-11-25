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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
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
  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withId(POSITION_STAGED_PAYLOAD_ID)
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
  void renderForPosition_whenSubareaGoesIntoChange_rendersFormWithSubareaCaption() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attributeExists("form", "licenseeOrgUnitUrl", "preselectedTransferTo"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));
  }

  @Test
  void renderForPosition_whenSubareaUnknown_notFound() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(CORRECTION, POSITION));
    when(subareaEquityOptionsService.getSubareaOptionOrThrow(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY))
        .thenThrow(new LmsEntityNotFoundException("subarea", SUBAREA_KEY));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void renderForPosition_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, POSITION), null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void submitForPosition_whenValid_addsOrganisationToSubareaAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    var committedOperation = subareaSetEquityOperation(1, 40);
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION))
        .thenReturn(List.of(committedOperation));

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of(1)))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY))));

    verify(subareaSetEquityCorrectionService).commitSubareaSetEquity(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION, List.of(committedOperation, subareaSetEquityOperation(123, 60)));
  }

  @Test
  void submitForPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));

    verify(subareaSetEquityCorrectionService)
        .getCommittedSubareaSetEquityOperations(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION);
    verifyNoMoreInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void submitForPosition_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, POSITION), null, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderSummaryForPosition_rendersTheSubareasOrganisations() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    var committedOperations = List.of(subareaSetEquityOperation(1, 40), subareaSetEquityOperation(2, 60));
    var views = List.of(
        new SetEquityRow("Org One", BigDecimal.valueOf(40)),
        new SetEquityRow("Org Two", BigDecimal.valueOf(60)));
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations)).thenReturn(views);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY)))
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
                    .removeForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY, 1)),
                ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                    .removeForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY, 2)))),
            model().attribute("addOrganisationUrl", ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY))),
            model().attribute("saveAndContinueUrl", ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
                .submitSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId())));
  }

  @Test
  void removeForPosition_removesTheOrganisationFromTheSubareaAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    givenExecutedPositionWithSubarea();
    when(subareaSetEquityCorrectionService
        .getCommittedSubareaSetEquityOperations(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION))
        .thenReturn(List.of(subareaSetEquityOperation(1, 40), subareaSetEquityOperation(2, 60)));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .removeForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY, 1)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_KEY))));

    verify(subareaSetEquityCorrectionService).commitSubareaSetEquity(new CorrectionPosition.Executed(CORRECTION, POSITION), SUBAREA_OPTION, List.of(subareaSetEquityOperation(2, 60)));
  }

  @Test
  void submitSummaryForPosition_redirectsToLicencePosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(CORRECTION, POSITION));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION))))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId())));

    verifyNoInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void submitForPosition_whenStagedPositionAndOrganisationAlreadyAddedToSubarea_rendersFormAndDoesNotPersist() throws Exception {
    givenCorrectionAllocatedToUser();
    givenAddedPositionWithSubarea();
    when(subareaSetEquityCorrectionService.getCommittedSubareaSetEquityOperations(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), SUBAREA_OPTION))
        .thenReturn(List.of(subareaSetEquityOperation(123, 40)));

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of(123)))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), SUBAREA_KEY, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(FORM_VIEW),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD)))));

    verify(subareaSetEquityCorrectionService).getCommittedSubareaSetEquityOperations(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), SUBAREA_OPTION);
    verifyNoMoreInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void submitSummaryForPosition_whenStagedPosition_redirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .submitSummaryForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD))))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION_STAGED_PAYLOAD.getPositionId())));

    verifyNoInteractions(subareaSetEquityCorrectionService);
  }

  @Test
  void renderForPosition_whenStagedPositionAndLicenceIsCarbonStorage_forbidden() throws Exception {
    var carbonStorageCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build())
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(carbonStorageCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(carbonStorageCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), SUBAREA_KEY)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(subareaEquityOptionsService);
  }

  @Test
  void renderForPosition_whenStagedPositionAndNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), SUBAREA_KEY)))
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
    var correctionPosition = givenCorrectionPosition(new CorrectionPosition.Executed(CORRECTION, POSITION));
    when(subareaEquityOptionsService.getSubareaOptionOrThrow(correctionPosition, SUBAREA_KEY)).thenReturn(SUBAREA_OPTION);
  }

  private void givenAddedPositionWithSubarea() {
    var correctionPosition = givenCorrectionPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD));
    when(subareaEquityOptionsService.getSubareaOptionOrThrow(correctionPosition, SUBAREA_KEY)).thenReturn(SUBAREA_OPTION);
  }

  private static SubareaSetEquityOperation subareaSetEquityOperation(int transferTo, int equity) {
    return new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, transferTo, BigDecimal.valueOf(equity));
  }
}
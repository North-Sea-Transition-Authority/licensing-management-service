package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionSetEquityController.class)
class LicencePositionSetEquityControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionSetEquityFormValidator validator;

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @MockitoBean
  private SetEquityCorrectionService setEquityCorrectionService;

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(10)
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .withLicenceReference("CS/1")
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

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
    return CORRECTION;
  }

  @Test
  void renderForPosition_whenStagedPositionAndAllocated_rendersForm() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl", "preselectedTransferTo"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD)))));
  }

  @Test
  void submitForPosition_whenStagedPositionAndOrganisationAlreadyAdded_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD));
    var committedOperations = List.of(new SetEquityOperation(123, BigDecimal.valueOf(40)));
    when(setEquityCorrectionService.getCommittedSetEquityOperations(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD)))
        .thenReturn(committedOperations);

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of(123)))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()));

    verify(setEquityCorrectionService, never()).commitSetEquity(any(CorrectionPosition.class), anyList());
  }

  @Test
  void submitSummaryForPosition_whenStagedPosition_redirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitSummaryForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD))))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION_STAGED_PAYLOAD.getPositionId())));
  }

  @Test
  void renderSummaryForPosition_whenStagedPositionAndNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(
        Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForPosition_whenStagedPositionAndLicenceIsNotCarbonStorage_forbidden() throws Exception {
    var nonCarbonStorageLicence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.GAS_STORAGE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(nonCarbonStorageLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForPosition_whenAllocated_rendersFormWithAddChangeBackLink() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(CORRECTION, POSITION));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));
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
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, POSITION))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void submitForPosition_whenValid_persistsToCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, POSITION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(new CorrectionPosition.Executed(correction, POSITION)))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("40");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));

    verify(setEquityCorrectionService).commitSetEquity(new CorrectionPosition.Executed(correction, POSITION), List.of(new SetEquityOperation(123, form.getEquity().getAsBigDecimal().orElseThrow())));
  }

  @Test
  void submitForPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, POSITION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(new CorrectionPosition.Executed(correction, POSITION)))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));

    verify(setEquityCorrectionService, never()).commitSetEquity(any(CorrectionPosition.class), anyList());
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
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, POSITION), null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderSummaryForPosition_rendersViewsFromCorrection() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, POSITION));
    var committedOperations = List.of(new SetEquityOperation(1, BigDecimal.valueOf(40)));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(new CorrectionPosition.Executed(correction, POSITION)))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations))
        .thenReturn(List.of(new SetEquityRow("Org One", BigDecimal.valueOf(40))));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquitySummary"),
            model().attribute("totalEquity", BigDecimal.valueOf(40)),
            model().attributeExists("setEquityViews", "removeUrls", "addOrganisationUrl", "saveAndContinueUrl"));
  }

  @Test
  void removeForPosition_removesFromCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, POSITION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(new CorrectionPosition.Executed(correction, POSITION)))
        .thenReturn(List.of(
            new SetEquityOperation(1, BigDecimal.valueOf(40)),
            new SetEquityOperation(2, BigDecimal.valueOf(60))));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .removeForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), 1)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));

    verify(setEquityCorrectionService).commitSetEquity(new CorrectionPosition.Executed(correction, POSITION), List.of(new SetEquityOperation(2, BigDecimal.valueOf(60))));
  }

  @Test
  void submitSummaryForPosition_redirectsToLicencePosition() throws Exception {
    givenCorrectionAllocatedToUser();
    givenCorrectionPosition(new CorrectionPosition.Executed(CORRECTION, POSITION));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION))))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId())));

    verifyNoInteractions(setEquityCorrectionService);
  }

}
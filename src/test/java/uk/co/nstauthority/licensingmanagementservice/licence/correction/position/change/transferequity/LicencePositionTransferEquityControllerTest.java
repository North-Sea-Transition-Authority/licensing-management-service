package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionTransferEquityController.class)
@ActiveProfiles("test")
class LicencePositionTransferEquityControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionTransferEquityFormValidator validator;

  @MockitoBean
  private TransferEquityWithdrawFormValidator withdrawValidator;

  @MockitoBean
  private TransferEquityCorrectionService transferEquityCorrectionService;

  private static final String LICENCE_REFERENCE = "CS/2026/1";
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .withLicenceReference(LICENCE_REFERENCE)
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final UUID STAGED_POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withId(POSITION_STAGED_PAYLOAD_ID)
      .withLicenceCorrection(CORRECTION)
      .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
          .withLicencePositionId(STAGED_POSITION_ID.toString())
          .build())
      .build();

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  @Test
  void renderForPosition_whenStagedPositionAndAllocated_rendersFormWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionStagedPayload = addedPositionStagedPayload(correction);
    givenCorrectionPosition(new CorrectionPosition.Staged(positionStagedPayload));
    var equityHoldings = Map.of(1, BigDecimal.valueOf(60));
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(equityHoldings);
    var licenseeOptions = Map.of("1", "Org One (60%)");
    when(transferEquityCorrectionService.getLicenseeOptions(equityHoldings)).thenReturn(licenseeOptions);
    var transferToOptions = Map.of("1", "Org One (60%)", "2", "Org Two (0%)");
    when(transferEquityCorrectionService.getTransferToOptions(equityHoldings)).thenReturn(transferToOptions);

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOptions", licenseeOptions),
            model().attribute("transferToOptions", transferToOptions),
            model().attribute("backLinkUrl", addedChangeChooserUrl()));
  }

  @Test
  void submitForPosition_whenStagedPositionAndValidAndTransferorRetainsEquity_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionStagedPayload = addedPositionStagedPayload(correction);
    givenCorrectionPosition(new CorrectionPosition.Staged(positionStagedPayload));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));
    var equityHoldings = Map.of(1, BigDecimal.valueOf(60));
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(equityHoldings);

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(equityHoldings))).thenReturn(false);

    mockMvc.perform(post(addedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedSummaryUrl()));

    verify(transferEquityCorrectionService).addTransferEquity(new CorrectionPosition.Staged(positionStagedPayload), form);
  }

  @Test
  void submitForPosition_whenStagedPositionAndValidAndTransferorHoldsNoEquity_redirectsToWithdraw() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionStagedPayload = addedPositionStagedPayload(correction);
    givenCorrectionPosition(new CorrectionPosition.Staged(positionStagedPayload));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));
    var equityHoldings = Map.of(1, BigDecimal.ZERO);
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(equityHoldings);

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(equityHoldings))).thenReturn(false);

    mockMvc.perform(post(addedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedWithdrawUrl(0)));

    verify(transferEquityCorrectionService).addTransferEquity(new CorrectionPosition.Staged(positionStagedPayload), form);
  }

  @Test
  void renderWithdrawForPosition_whenStagedPositionAndUnanswered_preselectsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionStagedPayload = addedPositionStagedPayload(correction);
    var operation = transferOp(1, 2, 100, null);
    givenCorrectionPosition(new CorrectionPosition.Staged(positionStagedPayload));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(List.of(operation));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(operation)))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), null)));

    mockMvc.perform(get(addedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("form", hasProperty("withdrawalDecision", is(nullValue()))));
  }

  @Test
  void renderForPosition_whenStagedPositionAndNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(Optional.empty());

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForPosition_whenStagedPositionAndLicenceIsNotCarbonStorage_forbidden() throws Exception {
    var nonCarbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.GAS_STORAGE).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(nonCarbonStorageLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForPosition_whenNoTransfersYet_rendersFormWithAddChangeBackLink() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    var equityHoldings = Map.of(1, BigDecimal.valueOf(60));
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(equityHoldings);
    var licenseeOptions = Map.of("1", "Org One (60%)");
    when(transferEquityCorrectionService.getLicenseeOptions(equityHoldings)).thenReturn(licenseeOptions);
    var transferToOptions = Map.of("1", "Org One (60%)", "2", "Org Two (0%)");
    when(transferEquityCorrectionService.getTransferToOptions(equityHoldings)).thenReturn(transferToOptions);

    mockMvc.perform(get(executedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOptions", licenseeOptions),
            model().attribute("transferToOptions", transferToOptions),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));
  }

  @Test
  void renderForPosition_whenTransfersAlreadyAdded_backLinkIsSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));

    mockMvc.perform(get(executedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOptions", Map.of()),
            model().attribute("transferToOptions", Map.of()),
            model().attribute("backLinkUrl", executedSummaryUrl()));
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
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, POSITION))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void submitForPosition_whenTransferorHoldsNoEquity_redirectsToWithdraw() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));
    var equityHoldings = Map.of(1, BigDecimal.ZERO);
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(equityHoldings);

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(equityHoldings))).thenReturn(false);

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedWithdrawUrl(0)));

    verify(transferEquityCorrectionService).addTransferEquity(new CorrectionPosition.Executed(correction, licencePosition), form);
  }

  @Test
  void submitForPosition_whenTransferorRetainsEquity_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));
    var equityHoldings = Map.of(1, BigDecimal.valueOf(60));
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(equityHoldings);

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(equityHoldings))).thenReturn(false);

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService).addTransferEquity(new CorrectionPosition.Executed(correction, licencePosition), form);
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
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, POSITION), null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void submitWithdrawForPosition_whenValid_persistsRetentionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));

    var form = withdrawForm("WITHDRAW");
    when(withdrawValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(executedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService)
        .setTransferEquityRetention(new CorrectionPosition.Executed(correction, licencePosition), 0, false);
  }

  @Test
  void renderSummaryForPosition_rendersSummaryWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operations = List.of(transferOp(1, 2, 100, true));
    var views = List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), true));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(operations);
    when(transferEquityCorrectionService.getTransferEquityViews(operations)).thenReturn(views);
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(Map.of(1, BigDecimal.ZERO));

    mockMvc.perform(get(executedSummaryUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquitySummary"),
            model().attribute("pageTitle", "Transfer equity"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("transferEquityViews", views),
            model().attribute("addTransferUrl", executedFormUrl()),
            model().attribute("backLinkUrl", executedPositionUrl()),
            model().attribute("saveAndContinueUrl", executedPositionUrl()),
            model().attribute("removeUrls", List.of(executedRemoveUrl(0))),
            model().attribute("withdrawUrls", List.of(executedWithdrawUrl(0))),
            model().attribute("withdrawApplicable", List.of(true)));
  }

  @Test
  void submitForPosition_whenInvalid_rendersFormWithTransferFromOptionsAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    var equityHoldings = Map.of(1, BigDecimal.valueOf(60));
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(equityHoldings);

    var form = new LicencePositionTransferEquityForm();
    form.setTransferFrom("1");
    form.setTransferTo("2");
    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(equityHoldings))).thenReturn(true);
    var licenseeOptions = Map.of("1", "Org One (60%)");
    when(transferEquityCorrectionService.getLicenseeOptions(equityHoldings)).thenReturn(licenseeOptions);
    var transferToOptions = Map.of("1", "Org One (60%)", "2", "Org Two (0%)");
    when(transferEquityCorrectionService.getTransferToOptions(equityHoldings)).thenReturn(transferToOptions);

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", form),
            model().attribute("licenseeOptions", licenseeOptions),
            model().attribute("transferToOptions", transferToOptions),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)))));

    verify(transferEquityCorrectionService, never())
        .addTransferEquity(any(CorrectionPosition.class), any());
  }

  @Test
  void renderWithdrawForPosition_rendersWithdrawPageWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operation = transferOp(1, 2, 100, true);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of(operation));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(operation)))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), true)));

    mockMvc.perform(get(executedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquityWithdraw"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", hasProperty("withdrawalDecision", is("RETAIN"))),
            model().attribute("organisationName", "From Org"),
            model().attribute("withdrawalOptions", TransferEquityWithdrawalDecision.getOptions()),
            model().attribute("submitUrl", executedWithdrawUrl(0)),
            model().attribute("backLinkUrl", executedSummaryUrl()));
  }

  @Test
  void renderWithdrawForPosition_whenIndexOutOfRange_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of());

    mockMvc.perform(get(executedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));
  }

  @Test
  void submitWithdrawForPosition_whenIndexOutOfRange_redirectsToSummaryAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of());

    var form = withdrawForm("RETAIN");

    mockMvc.perform(post(executedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService, never())
        .setTransferEquityRetention(any(CorrectionPosition.class), anyInt(), anyBoolean());
  }

  @Test
  void submitWithdrawForPosition_whenInvalid_rendersWithdrawPageAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operation = transferOp(1, 2, 100, null);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(List.of(operation));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(operation)))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), null)));

    var form = new TransferEquityWithdrawForm();
    when(withdrawValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);

    mockMvc.perform(post(executedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquityWithdraw"),
            model().attribute("form", form),
            model().attribute("organisationName", "From Org"),
            model().attribute("submitUrl", executedWithdrawUrl(0)),
            model().attribute("backLinkUrl", executedSummaryUrl()));

    verify(transferEquityCorrectionService, never())
        .setTransferEquityRetention(any(CorrectionPosition.class), anyInt(), anyBoolean());
  }

  @Test
  void removeForPosition_removesFromCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));

    mockMvc.perform(post(executedRemoveUrl(0)).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService).removeTransferEquity(new CorrectionPosition.Executed(correction, licencePosition), 0);
  }

  @Test
  void renderSummaryForPosition_whenTransferorRetainsEquity_marksWithdrawalNotApplicable() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operations = List.of(transferOp(1, 2, 40, null));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(operations);
    when(transferEquityCorrectionService.getTransferEquityViews(operations))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(40), null)));
    when(transferEquityCorrectionService.getEquityHoldings(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(Map.of(1, BigDecimal.valueOf(60)));

    mockMvc.perform(get(executedSummaryUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(model().attribute("withdrawApplicable", List.of(false)));
  }

  private LicencePositionTransferEquityForm transferForm() {
    var form = new LicencePositionTransferEquityForm();
    form.setTransferFrom("1");
    form.setTransferTo("2");
    form.getEquity().setInputValue("100");
    return form;
  }

  private TransferEquityWithdrawForm withdrawForm(String decision) {
    var form = new TransferEquityWithdrawForm();
    form.setWithdrawalDecision(decision);
    return form;
  }

  private static TransferEquityOperation transferOp(int from, int to, int equity, Boolean retain) {
    return new TransferEquityOperation(from, to, BigDecimal.valueOf(equity), retain);
  }

  private LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .build();
  }

  private LicencePositionStagedPayload addedPositionStagedPayload(LicenceCorrection correction) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(STAGED_POSITION_ID.toString())
            .build())
        .build();
  }

  private static String addedFormUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD)));
  }

  private static String addedSummaryUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD)));
  }

  private static String addedWithdrawUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderWithdrawForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), index));
  }

  private static String addedRemoveUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .removeForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD), index));
  }

  private static String addedChangeChooserUrl() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForPosition(new CorrectionPosition.Staged(POSITION_STAGED_PAYLOAD)));
  }

  private static String addedPositionUrl() {
    return LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, STAGED_POSITION_ID);
  }

  private static String executedFormUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)));
  }

  private static String executedSummaryUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)));
  }

  private static String executedWithdrawUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderWithdrawForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), index));
  }

  private static String executedRemoveUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .removeForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION), index));
  }

  private static String executedPositionUrl() {
    return LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId());
  }
}
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionTransferEquityController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionTransferEquityControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionTransferEquityFormValidator validator;

  @MockitoBean
  private TransferEquityWithdrawFormValidator withdrawValidator;

  @MockitoBean
  private OrganisationUnitQueryService  organisationUnitQueryService;

  @MockitoBean
  private TransferEquityCorrectionService transferEquityCorrectionService;

  private static final String LICENCE_REFERENCE = "CS/2026/1";
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .withLicenceReference(LICENCE_REFERENCE)
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  @Test
  void renderForAddedPosition_whenAllocated_rendersFormWithAllAttributes() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOrgUnitUrl", licenseeOrgUnitUrl()),
            model().attribute("preselectedTransferFrom", Map.of()),
            model().attribute("preselectedTransferTo", Map.of()),
            model().attribute("backLinkUrl", addedChangeChooserUrl()));
  }

  @Test
  void renderForAddedPosition_whenTransfersAlreadyAdded_backLinkIsSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOrgUnitUrl", licenseeOrgUnitUrl()),
            model().attribute("preselectedTransferFrom", Map.of()),
            model().attribute("preselectedTransferTo", Map.of()),
            model().attribute("backLinkUrl", addedSummaryUrl()));
  }

  @Test
  void submitForAddedPosition_whenValidAndTransferorRetainsEquity_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));
    when(transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection))
        .thenReturn(Map.of(1, BigDecimal.valueOf(60)));

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(addedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedSummaryUrl()));

    verify(transferEquityCorrectionService).addTransferEquity(positionCorrection, form);
  }

  @Test
  void submitForAddedPosition_whenValidAndTransferorHoldsNoEquity_redirectsToWithdraw() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));
    when(transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection))
        .thenReturn(Map.of(1, BigDecimal.ZERO));

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(addedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedWithdrawUrl(0)));

    verify(transferEquityCorrectionService).addTransferEquity(positionCorrection, form);
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersFormWithPreselectedOrganisationsAndDoesNotPersist() throws Exception {
    givenCorrectionAllocatedToUser();

    var form = new LicencePositionTransferEquityForm();
    form.setTransferFrom("1");
    form.setTransferTo("2");
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("1")).thenReturn(Map.of("1", "Org One"));
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("2")).thenReturn(Map.of("2", "Org Two"));

    mockMvc.perform(post(addedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", form),
            model().attribute("licenseeOrgUnitUrl", licenseeOrgUnitUrl()),
            model().attribute("preselectedTransferFrom", Map.of("1", "Org One")),
            model().attribute("preselectedTransferTo", Map.of("2", "Org Two")),
            model().attribute("backLinkUrl", addedChangeChooserUrl()));

    verify(transferEquityCorrectionService, never()).addTransferEquity(any(), eq(form));
  }

  @Test
  void renderWithdrawForAddedPosition_rendersWithdrawPageWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operation = transferOp(1, 2, 100, Boolean.TRUE);
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(operation));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(operation)))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), true)));

    mockMvc.perform(get(addedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquityWithdraw"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", hasProperty("withdrawalDecision", is("RETAIN"))),
            model().attribute("organisationName", "From Org"),
            model().attribute("withdrawalOptions", TransferEquityWithdrawalDecision.getOptions()),
            model().attribute("submitUrl", addedWithdrawUrl(0)),
            model().attribute("backLinkUrl", addedSummaryUrl()));
  }

  @Test
  void renderWithdrawForAddedPosition_whenUnanswered_preselectsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operation = transferOp(1, 2, 100, null);
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(operation));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(operation)))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), null)));

    mockMvc.perform(get(addedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("form", hasProperty("withdrawalDecision", is(nullValue()))));
  }

  @Test
  void renderWithdrawForAddedPosition_whenIndexOutOfRange_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of());

    mockMvc.perform(get(addedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedSummaryUrl()));
  }

  @Test
  void submitWithdrawForAddedPosition_whenValid_persistsRetentionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));

    var form = withdrawForm("RETAIN");
    when(withdrawValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(addedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedSummaryUrl()));

    verify(transferEquityCorrectionService).setTransferEquityRetention(positionCorrection, 0, true);
  }

  @Test
  void submitWithdrawForAddedPosition_whenInvalid_rendersWithdrawPageAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operation = transferOp(1, 2, 100, null);
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of(operation));
    when(transferEquityCorrectionService.getTransferEquityViews(List.of(operation)))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), null)));

    var form = new TransferEquityWithdrawForm();
    when(withdrawValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);

    mockMvc.perform(post(addedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquityWithdraw"),
            model().attribute("form", form),
            model().attribute("organisationName", "From Org"),
            model().attribute("submitUrl", addedWithdrawUrl(0)),
            model().attribute("backLinkUrl", addedSummaryUrl()));

    verify(transferEquityCorrectionService, never()).setTransferEquityRetention(eq(positionCorrection), eq(0), anyBoolean());
  }

  @Test
  void renderSummaryForAddedPosition_rendersSummaryWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operations = List.of(transferOp(1, 2, 100, true));
    var views = List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), true));
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(operations);
    when(transferEquityCorrectionService.getTransferEquityViews(operations)).thenReturn(views);
    when(transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection))
        .thenReturn(Map.of(1, BigDecimal.ZERO));

    mockMvc.perform(get(addedSummaryUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquitySummary"),
            model().attribute("pageTitle", "Transfer equity"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("transferEquityViews", views),
            model().attribute("addTransferUrl", addedFormUrl()),
            model().attribute("backLinkUrl", addedPositionUrl()),
            model().attribute("saveAndContinueUrl", addedPositionUrl()),
            model().attribute("removeUrls", List.of(addedRemoveUrl(0))),
            model().attribute("withdrawUrls", List.of(addedWithdrawUrl(0))),
            model().attribute("withdrawApplicable", List.of(true)));
  }

  @Test
  void renderSummaryForAddedPosition_whenTransferorRetainsEquity_marksWithdrawalNotApplicable() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operations = List.of(transferOp(1, 2, 40, null));
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(operations);
    when(transferEquityCorrectionService.getTransferEquityViews(operations))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(40), null)));
    when(transferEquityCorrectionService.getEquityHoldingsForAddedPosition(correction, positionCorrection))
        .thenReturn(Map.of(1, BigDecimal.valueOf(60)));

    mockMvc.perform(get(addedSummaryUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(model().attribute("withdrawApplicable", List.of(false)));
  }

  @Test
  void removeForAddedPosition_removesFromCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);

    mockMvc.perform(post(addedRemoveUrl(0)).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedSummaryUrl()));

    verify(transferEquityCorrectionService).removeTransferEquity(positionCorrection, 0);
  }

  @Test
  void renderForAddedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(Optional.empty());

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForAddedPosition_whenLicenceIsNotCarbonStorage_forbidden() throws Exception {
    var nonCarbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.GAS_STORAGE).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(nonCarbonStorageLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));

    mockMvc.perform(get(addedFormUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForExecutedPosition_whenAllocated_rendersFormWithPositionBackLink() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(get(executedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOrgUnitUrl", licenseeOrgUnitUrl()),
            model().attribute("preselectedTransferFrom", Map.of()),
            model().attribute("preselectedTransferTo", Map.of()),
            model().attribute("backLinkUrl", executedPositionUrl()));
  }

  @Test
  void renderForExecutedPosition_whenTransfersAlreadyAdded_backLinkIsSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));

    mockMvc.perform(get(executedFormUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", instanceOf(LicencePositionTransferEquityForm.class)),
            model().attribute("licenseeOrgUnitUrl", licenseeOrgUnitUrl()),
            model().attribute("preselectedTransferFrom", Map.of()),
            model().attribute("preselectedTransferTo", Map.of()),
            model().attribute("backLinkUrl", executedSummaryUrl()));
  }

  @Test
  void submitForExecutedPosition_whenTransferorHoldsNoEquity_redirectsToWithdraw() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));
    when(transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, POSITION_ID))
        .thenReturn(Map.of(1, BigDecimal.ZERO));

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedWithdrawUrl(0)));

    verify(transferEquityCorrectionService).addTransferEquityForExecutedPosition(correction, licencePosition, form);
  }

  @Test
  void submitForExecutedPosition_whenTransferorRetainsEquity_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of(transferOp(1, 2, 40, null)));
    when(transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, POSITION_ID))
        .thenReturn(Map.of(1, BigDecimal.valueOf(60)));

    var form = transferForm();
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService).addTransferEquityForExecutedPosition(correction, licencePosition, form);
  }

  @Test
  void submitWithdrawForExecutedPosition_whenValid_persistsRetentionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of(transferOp(1, 2, 100, null)));

    var form = withdrawForm("WITHDRAW");
    when(withdrawValidator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(false);

    mockMvc.perform(post(executedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService)
        .setTransferEquityRetentionForExecutedPosition(correction, licencePosition, 0, false);
  }

  @Test
  void renderSummaryForExecutedPosition_rendersSummaryWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operations = List.of(transferOp(1, 2, 100, true));
    var views = List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(100), true));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(operations);
    when(transferEquityCorrectionService.getTransferEquityViews(operations)).thenReturn(views);
    when(transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, POSITION_ID))
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
  void submitForExecutedPosition_whenInvalid_rendersFormWithPreselectedOrganisationsAndDoesNotPersist() throws Exception {
    givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);

    var form = new LicencePositionTransferEquityForm();
    form.setTransferFrom("1");
    form.setTransferTo("2");
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("1")).thenReturn(Map.of("1", "Org One"));
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("2")).thenReturn(Map.of("2", "Org Two"));

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/transferEquity"),
            model().attribute("pageTitle", "Add equity transfer"),
            model().attribute("pageCaption", LICENCE_REFERENCE),
            model().attribute("form", form),
            model().attribute("licenseeOrgUnitUrl", licenseeOrgUnitUrl()),
            model().attribute("preselectedTransferFrom", Map.of("1", "Org One")),
            model().attribute("preselectedTransferTo", Map.of("2", "Org Two")),
            model().attribute("backLinkUrl", executedPositionUrl()));

    verify(transferEquityCorrectionService, never())
        .addTransferEquityForExecutedPosition(any(), any(), any());
  }

  @Test
  void submitForExecutedPosition_whenInvalidAndTransferToIsBlankOrInvalidNumber_rendersFormWithEmptyPreselectedMap() throws Exception {
    givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);

    var form = new LicencePositionTransferEquityForm();
    form.setTransferTo("NOT_A_NUMBER");
    when(validator.hasErrors(eq(form), any(BindingResult.class))).thenReturn(true);

    mockMvc.perform(post(executedFormUrl()).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().isOk())
        .andExpect(model().attribute("preselectedTransferFrom", Map.of()))
        .andExpect(model().attribute("preselectedTransferTo", Map.of()));
  }

  @Test
  void renderWithdrawForExecutedPosition_rendersWithdrawPageWithAllAttributes() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operation = transferOp(1, 2, 100, true);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
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
  void renderWithdrawForExecutedPosition_whenIndexOutOfRange_redirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of());

    mockMvc.perform(get(executedWithdrawUrl(0)).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));
  }

  @Test
  void submitWithdrawForExecutedPosition_whenIndexOutOfRange_redirectsToSummaryAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of());

    var form = withdrawForm("RETAIN");

    mockMvc.perform(post(executedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService, never())
        .setTransferEquityRetentionForExecutedPosition(any(), any(), anyInt(), anyBoolean());
  }

  @Test
  void submitWithdrawForExecutedPosition_whenInvalid_rendersWithdrawPageAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operation = transferOp(1, 2, 100, null);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
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
        .setTransferEquityRetentionForExecutedPosition(any(), any(), anyInt(), anyBoolean());
  }

  @Test
  void removeForExecutedPosition_removesFromCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);

    mockMvc.perform(post(executedRemoveUrl(0)).with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(executedSummaryUrl()));

    verify(transferEquityCorrectionService).removeTransferEquityForExecutedPosition(correction, licencePosition, 0);
  }

  @Test
  void renderSummaryForExecutedPosition_whenTransferorRetainsEquity_marksWithdrawalNotApplicable() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var operations = List.of(transferOp(1, 2, 40, null));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(operations);
    when(transferEquityCorrectionService.getTransferEquityViews(operations))
        .thenReturn(List.of(new TransferEquityHoldingView("From Org", "To Org", BigDecimal.valueOf(40), null)));
    when(transferEquityCorrectionService.getEquityHoldingsForCorrection(correction, POSITION_ID))
        .thenReturn(Map.of(1, BigDecimal.valueOf(60)));

    mockMvc.perform(get(executedSummaryUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(model().attribute("withdrawApplicable", List.of(false)));
  }

  @Test
  void submitWithdrawForAddedPosition_whenIndexOutOfRange_redirectsToSummaryAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(transferEquityCorrectionService.getCommittedTransferEquityOperations(positionCorrection))
        .thenReturn(List.of());

    var form = withdrawForm("RETAIN");

    mockMvc.perform(post(addedWithdrawUrl(0)).with(user(regulatorUser)).with(csrf()).flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(addedSummaryUrl()));

    verify(transferEquityCorrectionService, never()).setTransferEquityRetention(any(), anyInt(), anyBoolean());
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

  private static String licenseeOrgUnitUrl() {
    return SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null));
  }

  private static String addedFormUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));
  }

  private static String addedSummaryUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));
  }

  private static String addedWithdrawUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderWithdrawForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, index, null));
  }

  private static String addedRemoveUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .removeForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, index, null));
  }

  private static String addedChangeChooserUrl() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));
  }

  private static String addedPositionUrl() {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));
  }

  private static String executedFormUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null));
  }

  private static String executedSummaryUrl() {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderSummaryForExecutedPosition(CORRECTION_ID, POSITION_ID, null));
  }

  private static String executedWithdrawUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .renderWithdrawForExecutedPosition(CORRECTION_ID, POSITION_ID, index, null));
  }

  private static String executedRemoveUrl(int index) {
    return ReverseRouter.route(on(LicencePositionTransferEquityController.class)
        .removeForExecutedPosition(CORRECTION_ID, POSITION_ID, index, null));
  }

  private static String executedPositionUrl() {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));
  }
}
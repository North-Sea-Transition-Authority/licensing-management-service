package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionSetEquityController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionSetEquityControllerTest extends AbstractControllerTest {

  @MockitoBean
  private LicencePositionSetEquityFormValidator validator;

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @MockitoBean
  private SetEquityCorrectionService setEquityCorrectionService;

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .withLicenceReference("CS/1")
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

  private LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .build();
  }

  @Test
  void renderForAddedPosition_whenAllocated_rendersForm() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl", "preselectedTransferTo"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }

  @Test
  void submitForAddedPosition_whenValid_persistsToCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(positionCorrection)).thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("40");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));

    verify(setEquityCorrectionService).commitSetEquity(positionCorrection,
        List.of(new SetEquityOperation(123, form.getEquity().getAsBigDecimal().orElseThrow())));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(positionCorrection))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));

    verify(setEquityCorrectionService, never()).commitSetEquity(any(), anyList());
  }

  @Test
  void submitForAddedPosition_whenOrganisationAlreadyAdded_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var committedOperations = List.of(new SetEquityOperation(123, BigDecimal.valueOf(40)));
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(positionCorrection))
        .thenReturn(committedOperations);

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(committedOperations))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()));

    verify(setEquityCorrectionService, never()).commitSetEquity(any(), anyList());
  }

  @Test
  void renderSummaryForAddedPosition_rendersViewsFromCorrection() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var committedOperations = List.of(new SetEquityOperation(1, BigDecimal.valueOf(40)));
    var setEquityViews = List.of(new SetEquityRow("Org One", BigDecimal.valueOf(40)));
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(positionCorrection))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations))
        .thenReturn(setEquityViews);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquitySummary"),
            model().attribute("pageTitle", "Add licence equity"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("setEquityViews", setEquityViews),
            model().attribute("totalEquity", BigDecimal.valueOf(40)),
            model().attributeExists("removeUrls", "addOrganisationUrl", "saveAndContinueUrl", "backLinkUrl"));
  }

  @Test
  void removeForAddedPosition_removesFromCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(setEquityCorrectionService.getCommittedSetEquityOperations(positionCorrection))
        .thenReturn(List.of(
            new SetEquityOperation(1, BigDecimal.valueOf(40)),
            new SetEquityOperation(2, BigDecimal.valueOf(60))));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .removeForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, 1, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));

    verify(setEquityCorrectionService).commitSetEquity(positionCorrection, List.of(new SetEquityOperation(2, BigDecimal.valueOf(60))));
  }

  @Test
  void submitSummaryForAddedPosition_redirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitSummaryForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }

  @Test
  void renderSummaryForAddedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(
        Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForAddedPosition_whenLicenceIsNotCarbonStorage_forbidden() throws Exception {
    var nonCarbonStorageLicence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.GAS_STORAGE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(nonCarbonStorageLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForExecutedPosition_whenAllocated_rendersFormWithAddChangeBackLink() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void submitForExecutedPosition_whenValid_persistsToCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction,
        licencePosition))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("40");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));

    verify(setEquityCorrectionService).commitSetEquityForExecutedPosition(correction, licencePosition,
        List.of(new SetEquityOperation(123, form.getEquity().getAsBigDecimal().orElseThrow())));
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, licencePosition))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));

    verify(setEquityCorrectionService, never()).commitSetEquityForExecutedPosition(any(), any(), anyList());
  }

  @Test
  void renderSummaryForExecutedPosition_rendersViewsFromCorrection() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var committedOperations = List.of(new SetEquityOperation(1, BigDecimal.valueOf(40)));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction,
        licencePosition))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations))
        .thenReturn(List.of(new SetEquityRow("Org One", BigDecimal.valueOf(40))));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquitySummary"),
            model().attribute("totalEquity", BigDecimal.valueOf(40)),
            model().attributeExists("setEquityViews", "removeUrls", "addOrganisationUrl", "saveAndContinueUrl"));
  }

  @Test
  void removeForExecutedPosition_removesFromCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction,
        licencePosition))
        .thenReturn(List.of(
            new SetEquityOperation(1, BigDecimal.valueOf(40)),
            new SetEquityOperation(2, BigDecimal.valueOf(60))));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .removeForExecutedPosition(CORRECTION_ID, POSITION_ID, 1, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));

    verify(setEquityCorrectionService).commitSetEquityForExecutedPosition(correction, licencePosition,
        List.of(new SetEquityOperation(2, BigDecimal.valueOf(60))));
  }

  @Test
  void submitSummaryForExecutedPosition_redirectsToLicencePosition() throws Exception {
    givenCorrectionAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitSummaryForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION_ID, POSITION_ID, null))));

    verifyNoInteractions(licencePositionCorrectionService, setEquityCorrectionService, licencePositionService);
  }

}
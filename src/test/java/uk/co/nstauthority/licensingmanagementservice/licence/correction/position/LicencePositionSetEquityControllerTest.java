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
@ActiveProfiles("test")
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

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(CORRECTION));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(CORRECTION));
    return CORRECTION;
  }

  @Test
  void renderForAddedPosition_whenAllocated_rendersForm() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl", "preselectedTransferTo"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))));
  }

  @Test
  void submitForAddedPosition_whenValid_persistsToCorrectionAndRedirectsToSummary() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
    when(setEquityCorrectionService.getCommittedSetEquityOperations(POSITION_CORRECTION)).thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("40");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION))));

    verify(setEquityCorrectionService).commitSetEquity(POSITION_CORRECTION,
        List.of(new SetEquityOperation(123, form.getEquity().getAsBigDecimal().orElseThrow())));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
    when(setEquityCorrectionService.getCommittedSetEquityOperations(POSITION_CORRECTION))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForAddedPosition(CORRECTION, POSITION_CORRECTION))));

    verify(setEquityCorrectionService, never()).commitSetEquity(any(), anyList());
  }

  @Test
  void submitForAddedPosition_whenOrganisationAlreadyAdded_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
    var committedOperations = List.of(new SetEquityOperation(123, BigDecimal.valueOf(40)));
    when(setEquityCorrectionService.getCommittedSetEquityOperations(POSITION_CORRECTION))
        .thenReturn(committedOperations);

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("60");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(committedOperations))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForAddedPosition(CORRECTION, POSITION_CORRECTION, null, null, null)))
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
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
    var committedOperations = List.of(new SetEquityOperation(1, BigDecimal.valueOf(40)));
    var setEquityViews = List.of(new SetEquityRow("Org One", BigDecimal.valueOf(40)));
    when(setEquityCorrectionService.getCommittedSetEquityOperations(POSITION_CORRECTION))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations))
        .thenReturn(setEquityViews);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION)))
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
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
    when(setEquityCorrectionService.getCommittedSetEquityOperations(POSITION_CORRECTION))
        .thenReturn(List.of(
            new SetEquityOperation(1, BigDecimal.valueOf(40)),
            new SetEquityOperation(2, BigDecimal.valueOf(60))));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .removeForAddedPosition(CORRECTION, POSITION_CORRECTION, 1)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION))));

    verify(setEquityCorrectionService).commitSetEquity(POSITION_CORRECTION, List.of(new SetEquityOperation(2, BigDecimal.valueOf(60))));
  }

  @Test
  void submitSummaryForAddedPosition_redirectsToAddedPosition() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION, POSITION_CORRECTION))));
  }

  @Test
  void renderSummaryForAddedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(
        Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForAddedPosition(CORRECTION, POSITION_CORRECTION)))
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
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForAddedPosition(correction, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(validator);
  }

  @Test
  void renderForExecutedPosition_whenAllocated_rendersFormWithAddChangeBackLink() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForExecutedPosition(CORRECTION, POSITION))));
  }

  @Test
  void submitForExecutedPosition_whenValid_persistsToCorrectionAndRedirectsToSummary() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction,
        POSITION))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");
    form.getEquity().setInputValue("40");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION, POSITION))));

    verify(setEquityCorrectionService).commitSetEquityForExecutedPosition(correction, POSITION,
        List.of(new SetEquityOperation(123, form.getEquity().getAsBigDecimal().orElseThrow())));
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersFormAndDoesNotPersist() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction, POSITION))
        .thenReturn(List.of());

    var form = new LicencePositionSetEquityForm();
    form.setTransferTo("123");

    when(validator.hasErrors(eq(form), any(BindingResult.class), eq(List.of()))).thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitForExecutedPosition(CORRECTION, POSITION, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/setEquity"),
            model().attributeExists("form", "licenseeOrgUnitUrl"),
            model().attribute("pageTitle", "Add equity"),
            model().attribute("pageCaption", correction.getLicence().getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionAddChangeController.class)
                .renderForExecutedPosition(CORRECTION, POSITION))));

    verify(setEquityCorrectionService, never()).commitSetEquityForExecutedPosition(any(), any(), anyList());
  }

  @Test
  void renderSummaryForExecutedPosition_rendersViewsFromCorrection() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    var committedOperations = List.of(new SetEquityOperation(1, BigDecimal.valueOf(40)));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction,
        POSITION))
        .thenReturn(committedOperations);
    when(setEquityCorrectionService.getSetEquityViews(committedOperations))
        .thenReturn(List.of(new SetEquityRow("Org One", BigDecimal.valueOf(40))));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION, POSITION)))
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(setEquityCorrectionService.getCommittedSetEquityOperationsForExecutedPosition(correction,
        POSITION))
        .thenReturn(List.of(
            new SetEquityOperation(1, BigDecimal.valueOf(40)),
            new SetEquityOperation(2, BigDecimal.valueOf(60))));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .removeForExecutedPosition(CORRECTION, POSITION, 1)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderSummaryForExecutedPosition(CORRECTION, POSITION))));

    verify(setEquityCorrectionService).commitSetEquityForExecutedPosition(correction, POSITION,
        List.of(new SetEquityOperation(2, BigDecimal.valueOf(60))));
  }

  @Test
  void submitSummaryForExecutedPosition_redirectsToLicencePosition() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .submitSummaryForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION, POSITION))));

    verifyNoInteractions(setEquityCorrectionService);
  }

}
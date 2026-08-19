package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionPartialSurrenderController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionPartialSurrenderControllerTest extends AbstractControllerTest {

  private static final Integer LICENCE_ID = 116;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.AUGUST, 1);
  private static final int POSITION_DATE_ORDER = 2;
  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderDetails";

  private static final Feature BLOCK_30_1A = FeatureTestUtil.builder()
      .withFeatureName("30/1a")
      .build();
  private static final Feature BLOCK_30_2 = FeatureTestUtil.builder()
      .withFeatureName("30/2")
      .build();
  private static final List<Feature> BLOCK_FEATURES = List.of(BLOCK_30_1A, BLOCK_30_2);
  private static final Map<String, String> BLOCK_OPTIONS = Map.of(
      BLOCK_30_1A.getId().toString(), "Block 30/1a",
      BLOCK_30_2.getId().toString(), "Block 30/2");

  @MockitoBean
  private PartialSurrenderDetailsFormValidator validator;

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @Test
  void renderForExecutedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenLicenceIsNotProduction_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(carbonStorageLicence)
            .build()));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_rendersFormWithBlockOptionsAndDerivedDate() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", "Surrender details"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("surrenderDate", "1 August 2026"),
            model().attribute("blockOptions", BLOCK_OPTIONS),
            model().attribute("backLinkUrl", addChangeUrlForExecutedPosition()));
  }

  @Test
  void renderForExecutedPosition_whenSurrenderAlreadyCommitted_prefillsSelectedBlocksAndLinksBackToTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(licencePosition, BLOCK_FEATURES);
    givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId()))
            .build());
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", taskListUrl()))
        .andReturn();

    var form = (PartialSurrenderDetailsForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getFeatureIds()).containsExactly(BLOCK_30_1A.getId(), BLOCK_30_2.getId());
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockOptions", "surrenderDate"),
            model().attribute("backLinkUrl", addChangeUrlForExecutedPosition()));

    verify(partialSurrenderCorrectionService, never())
        .commitPartialSurrenderForExecutedPosition(any(), any(), any());
    verify(licencePositionService, times(1)).getBlockFeatures(licencePosition);
  }

  @Test
  void submitForExecutedPosition_whenInvalidAndSurrenderAlreadyCommitted_linksBackToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(licencePosition, BLOCK_FEATURES);
    givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(BLOCK_30_1A.getId()))
            .build());
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void submitForExecutedPosition_whenValid_commitsAndRedirectsToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        eq(correction), eq(licencePosition), any(PartialSurrenderOperation.class)))
        .thenReturn(updatePositionCorrection(licencePosition));
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(CORRECTION_ID, POSITION_CORRECTION_ID, null, null))));

    var captor = ArgumentCaptor.forClass(PartialSurrenderOperation.class);
    verify(partialSurrenderCorrectionService)
        .commitPartialSurrenderForExecutedPosition(eq(correction), eq(licencePosition), captor.capture());
    assertThat(captor.getValue()).isEqualTo(new PartialSurrenderOperation(null, List.of(BLOCK_30_1A.getId()), Map.of()));
  }

  @Test
  void renderForExecutedPosition_whenPositionHasNoBlocks_hasEmptyBlockOptions() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(licencePosition, List.of());
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("blockOptions", Map.of()));
  }

  @Test
  void renderForAddedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForAddedPosition_rendersFormWithBlocksHeldGoingIntoThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("surrenderDate", "1 August 2026"),
            model().attribute("blockOptions", BLOCK_OPTIONS),
            model().attribute("backLinkUrl", addChangeUrlForAddedPosition()));
  }

  @Test
  void renderForAddedPosition_whenSurrenderAlreadyCommitted_linksBackToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(BLOCK_30_1A.getId()))
            .build()));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockOptions", "surrenderDate"),
            model().attribute("backLinkUrl", addChangeUrlForAddedPosition()));

    verify(partialSurrenderCorrectionService, never()).commitPartialSurrender(any(), any());
  }

  @Test
  void submitForAddedPosition_whenInvalidAndSurrenderAlreadyCommitted_linksBackToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(LicenceOperation.newPartialSurrenderOperation()
            .withFeatureIds(List.of(BLOCK_30_1A.getId()))
            .build()));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void submitForAddedPosition_whenValid_commitsAndRedirectsToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(CORRECTION_ID, POSITION_CORRECTION_ID, null, null))));

    var captor = ArgumentCaptor.forClass(PartialSurrenderOperation.class);
    verify(partialSurrenderCorrectionService).commitPartialSurrender(eq(positionCorrection), captor.capture());
    assertThat(captor.getValue())
        .isEqualTo(new PartialSurrenderOperation(null, List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId()), Map.of()));
  }

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
        .withPositionDate(POSITION_DATE)
        .build();
  }

  private void givenBlockFeaturesForExecutedPosition(LicencePosition licencePosition, List<Feature> blockFeatures) {
    when(licencePositionService.getBlockFeatures(licencePosition)).thenReturn(blockFeatures);
  }

  private LicencePositionCorrection addedPositionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withEffectiveDate(POSITION_DATE)
            .withEffectiveDateOrder(POSITION_DATE_ORDER)
            .build())
        .build();
  }

  private LicencePositionCorrection updatePositionCorrection(LicencePosition licencePosition) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(licencePosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().build())
        .build();
  }

  private void givenBlockFeaturesForAddedPosition(
      LicencePositionCorrection positionCorrection,
      List<Feature> blockFeatures
  ) {
    when(partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection))
        .thenReturn(blockFeatures);
  }

  private void givenNoUpdatePositionCorrection(LicenceCorrection correction, LicencePosition licencePosition) {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(Optional.empty());
  }

  private void givenStagedSurrenderOnUpdatePositionCorrection(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      PartialSurrenderOperation staged
  ) {
    var positionCorrection = updatePositionCorrection(licencePosition);
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(Optional.of(positionCorrection));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
  }

  private static String addChangeUrlForExecutedPosition() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null));
  }

  private static String addChangeUrlForAddedPosition() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));
  }

  private static String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION_ID, POSITION_CORRECTION_ID, null, null));
  }
}

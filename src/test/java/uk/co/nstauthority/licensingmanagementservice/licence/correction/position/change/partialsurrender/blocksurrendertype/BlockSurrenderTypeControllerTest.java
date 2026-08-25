package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = BlockSurrenderTypeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class BlockSurrenderTypeControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final Feature BLOCK = FeatureTestUtil.builder().withFeatureName("30/1a").build();
  private static final UUID FEATURE_ID = BLOCK.getId();
  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderType";

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;

  @Test
  void renderSurrenderTypeForm_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSurrenderTypeForm_whenLicenceIsNotProduction_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(carbonStorageLicence)
            .build()));

    mockMvc.perform(get(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSurrenderTypeForm_rendersFormWithBlockNameAndSurrenderTypeOptions() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("blockName", "Block 30/1a"),
            model().attribute("surrenderTypeOptions", BlockSurrenderType.getOptions()),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void submitSurrenderTypeForm_whenInvalid_rendersFormAndSavesNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .submitSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockName", "surrenderTypeOptions"),
            model().attribute("backLinkUrl", taskListUrl()));

    verify(partialSurrenderCorrectionService, never()).setBlockSurrenderType(any(), any(), any());
  }

  @Test
  void submitSurrenderTypeForm_whenValid_savesTypeAndRedirectsToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(false);

    var form = new BlockSurrenderTypeForm();
    form.setSurrenderType(BlockSurrenderType.FULL_SURRENDER.name());

    mockMvc.perform(post(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .submitSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(taskListUrl()));

    verify(partialSurrenderCorrectionService)
        .setBlockSurrenderType(positionCorrection, FEATURE_ID, BlockSurrenderType.FULL_SURRENDER);
  }

  @Test
  void submitSurrenderTypeForm_whenPartialSurrender_savesTypeAndRedirectsToDefineArea() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(false);

    var form = new BlockSurrenderTypeForm();
    form.setSurrenderType(BlockSurrenderType.PARTIAL_SURRENDER.name());

    mockMvc.perform(post(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .submitSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(defineAreaUrl()));

    verify(partialSurrenderCorrectionService)
        .setBlockSurrenderType(positionCorrection, FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER);
  }

  @Test
  void renderSurrenderTypeFormForCorrectingChange_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(correctSurrenderTypeUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSurrenderTypeFormForCorrectingChange_whenLicenceIsNotProduction_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(carbonStorageLicence)
            .build()));

    mockMvc.perform(get(correctSurrenderTypeUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSurrenderTypeFormForCorrectingChange_whenPositionIsNotOnTheCorrectionLicence_notFound() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID))
        .thenThrow(new LmsEntityNotFoundException("licencePosition", POSITION_ID));

    mockMvc.perform(get(correctSurrenderTypeUrl()).with(user(regulatorUser)))
        .andExpect(status().isNotFound());

    verify(partialSurrenderCorrectionService, never())
        .getSurrenderUnderCorrectionOrThrow(correction, POSITION, LIVE_CHANGE_ID);
  }

  @Test
  void renderSurrenderTypeFormForCorrectingChange_prefillsFromTheSurrenderUnderCorrection() throws Exception {
    givenSurrenderUnderCorrection(LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .withSurrenderDetails(Map.of(FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, UUID.randomUUID(), List.of())))
        .build());

    var result = mockMvc.perform(get(correctSurrenderTypeUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("blockName", "Block 30/1a"),
            model().attribute("surrenderTypeOptions", BlockSurrenderType.getOptions()),
            model().attribute("backLinkUrl", correctingChangeTaskListUrl()))
        .andReturn();

    var form = (BlockSurrenderTypeForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getSurrenderType()).isEqualTo(BlockSurrenderType.PARTIAL_SURRENDER.name());
  }

  @Test
  void submitSurrenderTypeFormForCorrectingChange_whenInvalid_rendersFormAndStagesNothing() throws Exception {
    givenSurrenderUnderCorrection(LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .build());
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(post(submitCorrectSurrenderTypeUrl()).with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockName", "surrenderTypeOptions"),
            model().attribute("backLinkUrl", correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never())
        .correctExistingPartialSurrender(any(), any(), any(), any());
  }

  @Test
  void submitSurrenderTypeFormForCorrectingChange_whenPartialSurrender_stagesAndRedirectsToDefineArea()
      throws Exception {
    var surrenderUnderCorrection = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .build();
    var correction = givenSurrenderUnderCorrection(surrenderUnderCorrection);
    var corrected = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .withSurrenderDetails(Map.of(FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, UUID.randomUUID(), List.of())))
        .build();
    when(partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        surrenderUnderCorrection, FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER)).thenReturn(corrected);
    when(partialSurrenderCorrectionService.correctExistingPartialSurrender(
        correction, POSITION, LIVE_CHANGE_ID, corrected)).thenReturn(positionCorrection());
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(false);

    var form = new BlockSurrenderTypeForm();
    form.setSurrenderType(BlockSurrenderType.PARTIAL_SURRENDER.name());

    mockMvc.perform(post(submitCorrectSurrenderTypeUrl()).with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(defineAreaUrl()));

    verify(partialSurrenderCorrectionService)
        .correctExistingPartialSurrender(correction, POSITION, LIVE_CHANGE_ID, corrected);
  }

  @Test
  void submitSurrenderTypeFormForCorrectingChange_whenTheTypeDiffersFromTheLiveChange_thenStagesACorrection()
      throws Exception {
    var surrenderUnderCorrection = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .build();
    var correction = givenSurrenderUnderCorrection(surrenderUnderCorrection);
    // a full surrender still carries a command journey, so the corrected operation differs from the untyped live one
    var corrected = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .withSurrenderDetails(Map.of(FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, UUID.randomUUID(), List.of(FEATURE_ID))))
        .build();
    when(partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        surrenderUnderCorrection, FEATURE_ID, BlockSurrenderType.FULL_SURRENDER)).thenReturn(corrected);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(LocalDate.of(2026, Month.AUGUST, 1))
        .withFeatureIds(List.of(FEATURE_ID))
        .build());
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(false);

    var form = new BlockSurrenderTypeForm();
    form.setSurrenderType(BlockSurrenderType.FULL_SURRENDER.name());

    mockMvc.perform(post(submitCorrectSurrenderTypeUrl()).with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()))
        .andExpect(notificationBanner(NotificationBanner.newSuccessBanner()
            .withHeadingContent("Partial surrender type saved")
            .build()));

    verify(partialSurrenderCorrectionService)
        .correctExistingPartialSurrender(correction, POSITION, LIVE_CHANGE_ID, corrected);
  }

  @Test
  void submitSurrenderTypeFormForCorrectingChange_whenTheTypeMatchesTheLiveChange_thenRevertsTheStagedCorrection()
      throws Exception {
    var surrenderUnderCorrection = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .build();
    var correction = givenSurrenderUnderCorrection(surrenderUnderCorrection);
    // the corrected surrender matches the live one (only the reused command journey id would differ)
    var corrected = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID))
        .withSurrenderDetails(Map.of(FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, UUID.randomUUID(), List.of(FEATURE_ID))))
        .build();
    when(partialSurrenderCorrectionService.getOrCreatePartialSurrenderDetails(
        surrenderUnderCorrection, FEATURE_ID, BlockSurrenderType.FULL_SURRENDER)).thenReturn(corrected);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(LocalDate.of(2026, Month.AUGUST, 1))
        .withFeatureIds(List.of(FEATURE_ID))
        .withSurrenderDetails(Map.of(FEATURE_ID, new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, UUID.randomUUID(), List.of(FEATURE_ID))))
        .build());
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(false);

    var form = new BlockSurrenderTypeForm();
    form.setSurrenderType(BlockSurrenderType.FULL_SURRENDER.name());

    mockMvc.perform(post(submitCorrectSurrenderTypeUrl()).with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService).revertPartialSurrenderCorrection(correction, POSITION);
    verify(partialSurrenderCorrectionService, never())
        .correctExistingPartialSurrender(any(), any(), any(), any());
  }

  private void givenLiveSurrender(PartialSurrenderOperation liveSurrender) {
    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(LIVE_CHANGE_ID)).thenReturn(liveSurrender);
  }

  private LicenceCorrection givenSurrenderUnderCorrection(PartialSurrenderOperation surrenderUnderCorrection) {
    var correction = givenCorrectionAllocatedToUser();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, POSITION, LIVE_CHANGE_ID))
        .thenReturn(surrenderUnderCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(surrenderUnderCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    return correction;
  }

  private static String correctSurrenderTypeUrl() {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class)
        .renderSurrenderTypeFormForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, FEATURE_ID, null));
  }

  private static String submitCorrectSurrenderTypeUrl() {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class)
        .submitSurrenderTypeFormForCorrectingChange(
            CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, FEATURE_ID, null, null, null, null));
  }

  private static String correctingChangeTaskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null));
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .build();
  }

  private static String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION_ID, POSITION_CORRECTION_ID, null, null));
  }

  private static String defineAreaUrl() {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
        .renderDefineArea(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null));
  }
}

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

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ContextConfiguration(classes = LicencePositionPartialSurrenderController.class)
@ActiveProfiles("test")
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
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();

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
  private static final Set<UUID> ALREADY_OPERATED_ON = Set.of(UUID.randomUUID());

  @MockitoBean
  private PartialSurrenderDetailsFormValidator validator;

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private LicencePositionSpatialService licencePositionSpatialService;

  @Test
  void renderForExecutedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION" })
  void renderForExecutedPosition_whenLicenceTypeIsNotAllowed_forbidden(LicenceType licenceType) throws Exception {
    var notAllowedLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(notAllowedLicence)
            .build()));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.INCLUDE, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION" })
  void renderForExecutedPosition_whenLicenceTypeIsAllowed_thenOk(LicenceType licenceType) throws Exception {
    var allowedLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(allowedLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(allowedLicence)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionService.getPositionForLicence(allowedLicence, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk());
  }

  @Test
  void renderForExecutedPosition_rendersFormWithBlockOptionsAndDerivedDate() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
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
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId()))
        .build();
    var positionCorrection = givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition, staged);
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, stagedChangeId);
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
  void renderForExecutedPosition_whenSurrenderAlreadyStaged_anchorsCandidateBlocksOnTheStagedChange()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    var positionCorrection = givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition, staged);
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, null))
        .thenReturn(BLOCK_FEATURES);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, stagedChangeId))
        .thenReturn(List.of(BLOCK_30_2));
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("blockOptions", Map.of(BLOCK_30_2.getId().toString(), "Block 30/2")));
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, null))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
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
    verify(licencePositionSpatialService, times(1))
        .getBlockFeaturesGoingIntoChange(correction, licencePosition, null);
  }

  @Test
  void submitForExecutedPosition_whenInvalidAndSurrenderAlreadyCommitted_linksBackToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    var positionCorrection = givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition, staged);
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, stagedChangeId);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, positionCorrection, stagedChangeId))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
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
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        eq(correction), eq(licencePosition), any(PartialSurrenderOperation.class)))
        .thenReturn(updatePositionCorrection(licencePosition));
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, null))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

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
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, List.of(), null);
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
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES, null);
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
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES, stagedChangeId);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void renderForAddedPosition_whenSurrenderAlreadyStaged_anchorsCandidateBlocksOnTheStagedChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, null))
        .thenReturn(BLOCK_FEATURES);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(List.of(BLOCK_30_2));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("blockOptions", Map.of(BLOCK_30_2.getId().toString(), "Block 30/2")));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES, null);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(positionCorrection, null))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
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
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionCorrection, staged);
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES, stagedChangeId);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        positionCorrection, stagedChangeId))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
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
    givenBlockFeaturesForAddedPosition(positionCorrection, BLOCK_FEATURES, null);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(positionCorrection, null))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

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

  @Test
  void renderForCorrectingChange_prefillsFromTheLiveChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_2.getId()))
        .build());
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("blockOptions", BLOCK_OPTIONS),
            model().attribute("backLinkUrl", correctingChangeTaskListUrl()))
        .andReturn();

    var form = (PartialSurrenderDetailsForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getFeatureIds()).containsExactly(BLOCK_30_2.getId());
  }

  @Test
  void renderForCorrectingChange_whenAlreadyCorrected_prefillsFromTheStagedCorrection() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
            .build());
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", taskListUrl()))
        .andReturn();

    var form = (PartialSurrenderDetailsForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getFeatureIds()).containsExactly(BLOCK_30_1A.getId());
    verify(partialSurrenderCorrectionService, never()).getLiveSurrenderOrThrow(any());
  }

  @Test
  void submitForCorrectingChange_whenInvalid_rendersFormAndCorrectsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockOptions", "surrenderDate"),
            model().attribute("backLinkUrl", correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never()).correctExistingPartialSurrender(any(), any(), any(), any());
  }

  @Test
  void submitForCorrectingChange_whenValid_correctsTheLiveChangeAndRedirectsToTheLiveChangeTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var live = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(POSITION_DATE)
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    givenLiveSurrender(live);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    var captor = ArgumentCaptor.forClass(PartialSurrenderOperation.class);
    verify(partialSurrenderCorrectionService).correctExistingPartialSurrender(
        eq(correction), eq(licencePosition), eq(LIVE_CHANGE_ID), captor.capture());
    assertThat(captor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(new PartialSurrenderOperation(null, List.of(BLOCK_30_2.getId()), Map.of()));
  }

  @Test
  void submitForCorrectingChange_whenTheLiveSurrenderHasItsOwnDate_correctsWithoutCarryingThatDate() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var live = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(POSITION_DATE.minusYears(1))
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    givenLiveSurrender(live);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection());

    var captor = ArgumentCaptor.forClass(PartialSurrenderOperation.class);
    verify(partialSurrenderCorrectionService).correctExistingPartialSurrender(
        eq(correction), eq(licencePosition), eq(LIVE_CHANGE_ID), captor.capture());
    assertThat(captor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(new PartialSurrenderOperation(null, List.of(BLOCK_30_2.getId()), Map.of()));
  }

  @Test
  void renderForCorrectingChange_whenTheLiveSurrenderHasItsOwnDate_showsThePositionDate() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(POSITION_DATE.minusYears(1))
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("surrenderDate", DateUtil.formatLongDate(POSITION_DATE)));
  }

  @Test
  void submitForCorrectingChange_whenBlocksUnchanged_correctsNothingAndRedirectsToTheLiveChangeTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never())
        .correctExistingPartialSurrender(any(), any(), any(), any());
    verify(partialSurrenderCorrectionService).revertPartialSurrenderCorrection(correction, licencePosition);
  }

  @Test
  void submitForCorrectingChange_whenBlocksUnchangedButASurrenderTypeIsStaged_correctsWithoutReverting()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .withSurrenderDetails(Map.of(BLOCK_30_1A.getId(),
            new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.FULL_SURRENDER, UUID.randomUUID(), List.of(BLOCK_30_1A.getId()))))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    var positionCorrection = givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition, staged);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never()).revertPartialSurrenderCorrection(any(), any());
    verify(partialSurrenderCorrectionService)
        .correctExistingPartialSurrender(correction, licencePosition, LIVE_CHANGE_ID, staged);
  }

  @Test
  void submitForCorrectingChange_whenBlocksRevertedToTheLiveBlocks_revertsTheStagedCorrection() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_2.getId()))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    var positionCorrection = givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition, staged);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService).revertPartialSurrenderCorrection(correction, licencePosition);
    verify(partialSurrenderCorrectionService, never())
        .correctExistingPartialSurrender(any(), any(), any(), any());
  }

  @Test
  void submitForCorrectingChange_whenBlocksDifferFromTheLiveBlocksButMatchTheStagedBlocks_corrects()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_2.getId()))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    var positionCorrection = givenStagedSurrenderOnUpdatePositionCorrection(correction, licencePosition, staged);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, positionCorrection, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never()).revertPartialSurrenderCorrection(any(), any());
  }

  @Test
  void submitForCorrectingChange_whenResubmittingItsOwnBlocks_excludesTheEditedChangeSoNoAlreadyOperatedError()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var live = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionCorrection(correction, licencePosition);
    givenLiveSurrender(live);
    when(licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(licencePositionCorrectionService)
        .blockFeatureIdsAlreadyOperatedOnForExecutedPosition(licencePosition, null, LIVE_CHANGE_ID);
    verify(partialSurrenderCorrectionService).correctExistingPartialSurrender(
        eq(correction), eq(licencePosition), eq(LIVE_CHANGE_ID), any(PartialSurrenderOperation.class));
  }

  private void givenLiveSurrender(PartialSurrenderOperation liveSurrender) {
    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(LIVE_CHANGE_ID)).thenReturn(liveSurrender);
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

  private void givenBlockFeaturesForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      List<Feature> blockFeatures,
      @Nullable String stagedChangeId
  ) {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, stagedChangeId))
        .thenReturn(blockFeatures);
  }

  private String givenStagedSurrenderChangeId(
      LicencePositionCorrection positionCorrection,
      PartialSurrenderOperation staged
  ) {
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(stagedChange.changeId()));
    return stagedChange.changeId();
  }

  private void givenBlockFeaturesForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      List<Feature> blockFeatures
  ) {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, LIVE_CHANGE_ID))
        .thenReturn(blockFeatures);
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
      List<Feature> blockFeatures,
      @Nullable String stagedChangeId
  ) {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(positionCorrection, stagedChangeId))
        .thenReturn(blockFeatures);
  }

  private void givenNoUpdatePositionCorrection(LicenceCorrection correction, LicencePosition licencePosition) {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(Optional.empty());
  }

  private LicencePositionCorrection givenStagedSurrenderOnUpdatePositionCorrection(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      PartialSurrenderOperation staged
  ) {
    var positionCorrection = updatePositionCorrection(licencePosition);
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(Optional.of(positionCorrection));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.of(staged));
    return positionCorrection;
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

  private static String correctingChangeTaskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION_ID, POSITION_ID, LIVE_CHANGE_ID, null, null));
  }
}

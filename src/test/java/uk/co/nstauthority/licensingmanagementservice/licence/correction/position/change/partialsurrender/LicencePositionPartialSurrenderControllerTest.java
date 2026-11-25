package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
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
  private static final UUID STAGED_POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.AUGUST, 1);
  private static final int POSITION_DATE_ORDER = 2;
  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderDetails";
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(LIVE_CHANGE_ID))
      .build();
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
      .build();

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
  void renderForPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION" })
  void renderForPosition_whenLicenceTypeIsNotAllowed_forbidden(LicenceType licenceType) throws Exception {
    var notAllowedLicence = LicenceTestUtil.builder().withLicenceType(licenceType).build();
    var notAllowedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(notAllowedLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(notAllowedCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(notAllowedCorrection));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
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
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, POSITION))))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.INCLUDE, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION" })
  void renderForPosition_whenLicenceTypeIsAllowed_thenOk(LicenceType licenceType) throws Exception {
    var allowedLicence = LicenceTestUtil.builder().withId(LICENCE_ID).withLicenceType(licenceType).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(allowedLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(allowedLicence)
        .withPositionDate(POSITION_DATE)
        .build();
    when(licencePositionService.getPositionForLicence(allowedLicence, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, licencePosition))))
            .with(user(regulatorUser)))
        .andExpect(status().isOk());
  }

  @Test
  void renderForPosition_rendersFormWithBlockOptionsAndDerivedDate() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, licencePosition))))
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
  void renderForPosition_whenSurrenderAlreadyCommitted_prefillsSelectedBlocksAndLinksBackToTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId()))
        .build();
    var positionStagedPayload = givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition, staged);
    var stagedChangeId = givenStagedSurrenderChangeId(positionStagedPayload, staged);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, stagedChangeId);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, licencePosition))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", taskListUrl()))
        .andReturn();

    var form = (PartialSurrenderDetailsForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getFeatureIds()).containsExactly(BLOCK_30_1A.getId(), BLOCK_30_2.getId());
  }

  @Test
  void renderForPosition_whenSurrenderAlreadyStaged_anchorsCandidateBlocksOnTheStagedChange()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    var positionStagedPayload = givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition, staged);
    var stagedChangeId = givenStagedSurrenderChangeId(positionStagedPayload, staged);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition.getId(), null))
        .thenReturn(BLOCK_FEATURES);
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition.getId(), stagedChangeId))
        .thenReturn(List.of(BLOCK_30_2));
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, licencePosition))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("blockOptions", Map.of(BLOCK_30_2.getId().toString(), "Block 30/2")));
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
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, POSITION), null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void submitForPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOn(new CorrectionPosition.Executed(correction, licencePosition), null)).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, licencePosition), null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockOptions", "surrenderDate"),
            model().attribute("backLinkUrl", addChangeUrlForExecutedPosition()));

    verify(partialSurrenderCorrectionService, never())
        .commitPartialSurrender(any(CorrectionPosition.class), any());
    verify(licencePositionSpatialService, times(1))
        .getBlockFeaturesGoingIntoChange(correction, licencePosition.getId(), null);
  }

  @Test
  void submitForPosition_whenInvalidAndSurrenderAlreadyCommitted_linksBackToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    var positionStagedPayload = givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition, staged);
    var stagedChangeId = givenStagedSurrenderChangeId(positionStagedPayload, staged);
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, stagedChangeId);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOn(new CorrectionPosition.Executed(correction, licencePosition), stagedChangeId)).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, licencePosition), null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void submitForPosition_whenValid_commitsAndRedirectsToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, BLOCK_FEATURES, null);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    when(partialSurrenderCorrectionService.commitPartialSurrender(eq(new CorrectionPosition.Executed(correction, licencePosition)), any(PartialSurrenderOperation.class)))
        .thenReturn(updatePositionStagedPayload(correction, licencePosition));
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOn(new CorrectionPosition.Executed(correction, licencePosition), null)).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForPosition(new CorrectionPosition.Executed(correction, licencePosition), null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(CORRECTION, POSITION_STAGED_PAYLOAD, null))));

    var captor = ArgumentCaptor.forClass(PartialSurrenderOperation.class);
    verify(partialSurrenderCorrectionService)
        .commitPartialSurrender(eq(new CorrectionPosition.Executed(correction, licencePosition)), captor.capture());
    assertThat(captor.getValue()).isEqualTo(new PartialSurrenderOperation(null, List.of(BLOCK_30_1A.getId()), Map.of()));
  }

  @Test
  void renderForPosition_whenPositionHasNoBlocks_hasEmptyBlockOptions() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    givenCorrectionPosition(new CorrectionPosition.Executed(correction, licencePosition));
    givenBlockFeaturesForExecutedPosition(correction, licencePosition, List.of(), null);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Executed(correction, licencePosition))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("blockOptions", Map.of()));
  }

  @Test
  void renderForPosition_whenStagedPosition_rendersFormWithBlocksHeldGoingIntoThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionStagedPayload = addedPositionStagedPayload(correction);
    givenCorrectionPosition(new CorrectionPosition.Staged(positionStagedPayload));
    when(licencePositionStagedPayloadService.findPositionStagedPayload(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(Optional.of(positionStagedPayload));
    givenBlockFeaturesForAddedPosition(positionStagedPayload, BLOCK_FEATURES, null);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload))
        .thenReturn(Optional.empty());
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Staged(positionStagedPayload))).thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Staged(positionStagedPayload))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("surrenderDate", "1 August 2026"),
            model().attribute("blockOptions", BLOCK_OPTIONS),
            model().attribute("backLinkUrl", addChangeUrlFor(positionStagedPayload)));
  }

  @Test
  void renderForPosition_whenStagedPositionAndSurrenderAlreadyCommitted_linksBackToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionStagedPayload = addedPositionStagedPayload(correction);
    givenCorrectionPosition(new CorrectionPosition.Staged(positionStagedPayload));
    when(licencePositionStagedPayloadService.findPositionStagedPayload(new CorrectionPosition.Staged(positionStagedPayload)))
        .thenReturn(Optional.of(positionStagedPayload));
    var staged = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload))
        .thenReturn(Optional.of(staged));
    var stagedChangeId = givenStagedSurrenderChangeId(positionStagedPayload, staged);
    givenBlockFeaturesForAddedPosition(positionStagedPayload, BLOCK_FEATURES, stagedChangeId);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(new CorrectionPosition.Staged(positionStagedPayload))).thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForPosition(new CorrectionPosition.Staged(positionStagedPayload))))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void renderForCorrectingChange_prefillsFromTheLiveChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_2.getId()))
        .build());
    when(licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(correction, licencePosition, LIVE_CHANGE)))
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition,
        LicenceOperation.newPartialSurrenderOperation()
            .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
            .build());
    when(licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(correction, licencePosition, LIVE_CHANGE)))
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
  void renderForCorrectingChange_whenSingleBlock_rendersTheStartCorrectionPageWithoutStagingAnything() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var surrenderedHalfId = UUID.randomUUID();
    var liveSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .withSurrenderDetails(Map.of(BLOCK_30_1A.getId(),
            new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(surrenderedHalfId))))
        .build();
    var blockRows = List.of(new PartialSurrenderChangeView.BlockRow("Block 30/1a", List.of("Partial surrender")));

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(liveSurrender);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        correction, licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(Optional.of(BLOCK_30_1A));
    when(partialSurrenderCorrectionService.getBlockRows(liveSurrender)).thenReturn(blockRows);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(correction, licencePosition, LIVE_CHANGE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/startSingleBlockPartialSurrenderCorrection"),
            model().attribute("pageTitle", "Are you sure you want to correct this partial surrender?"),
            model().attribute("pageCaption", "P/1"),
            model().attribute("blockRows", blockRows),
            model().attribute("surrenderedFeatureIds", List.of(surrenderedHalfId)),
            model().attribute("srsWkid", CoordinateSystemUtils.getWkid(BLOCK_30_1A.getCoordinateSystem())),
            model().attribute("startCorrectionUrl", startCorrectionUrl()),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, licencePosition.getId())));

    verify(partialSurrenderCorrectionService, never()).correctExistingPartialSurrender(any(), any(), any(), any());
    verify(partialSurrenderCorrectionService, never()).getOrCreatePartialSurrenderDetails(any(), any(), any());
  }

  @Test
  void renderForCorrectingChange_whenSingleBlockLiveSurrenderHasNoSurrenderedAreas_thenMapsTheBlock() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var liveSurrender = surrenderOf(BLOCK_30_1A);

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(liveSurrender);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        correction, licencePosition, null, LIVE_CHANGE_ID))
        .thenReturn(Optional.of(BLOCK_30_1A));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(correction, licencePosition, LIVE_CHANGE)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("surrenderedFeatureIds", List.of(BLOCK_30_1A.getId())));
  }

  @Test
  void startCorrectingSingleBlockChange_whenSingleBlock_stagesTheCorrectionAndRedirectsToDefineArea() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var committedPositionStagedPayload = updatePositionStagedPayload(correction, licencePosition);

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    when(partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        correction, licencePosition, LIVE_CHANGE_ID))
        .thenReturn(Optional.of(new SingleBlockSurrender(committedPositionStagedPayload, BLOCK_30_1A)));

    mockMvc.perform(post(startCorrectionUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(defineAreaUrl()));
  }

  @Test
  void startCorrectingSingleBlockChange_whenNoLongerSingleBlock_throws() {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    when(partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        correction, licencePosition, LIVE_CHANGE_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> mockMvc.perform(post(startCorrectionUrl())
        .with(user(regulatorUser))
        .with(csrf())))
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .rootCause()
        .hasMessage("Change %s no longer has a single block to correct".formatted(LIVE_CHANGE_ID));
  }

  @Test
  void submitForCorrectingChange_whenInvalid_rendersFormAndCorrectsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    when(licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        null,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(live);
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        null,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(live);
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        null,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(POSITION_DATE.minusYears(1))
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(correction, licencePosition, LIVE_CHANGE)))
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        null,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never())
        .correctExistingPartialSurrender(any(), any(), any(), any());
    verify(partialSurrenderCorrectionService)
        .revertPartialSurrenderCorrection(correction, licencePosition, surrenderOf(BLOCK_30_1A));
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    var positionStagedPayload = givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition, staged);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionStagedPayload,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never()).revertPartialSurrenderCorrection(any(), any(), any());
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    var positionStagedPayload = givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition, staged);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionStagedPayload,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService)
        .revertPartialSurrenderCorrection(correction, licencePosition, surrenderOf(BLOCK_30_1A));
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    var positionStagedPayload = givenStagedSurrenderOnUpdatePositionStagedPayload(correction, licencePosition, staged);
    givenLiveSurrender(LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK_30_1A.getId()))
        .build());
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionStagedPayload,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(partialSurrenderCorrectionService, never()).revertPartialSurrenderCorrection(any(), any(), any());
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
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    givenBlockFeaturesForCorrectingChange(correction, licencePosition, BLOCK_FEATURES);
    givenNoUpdatePositionStagedPayload(correction, licencePosition);
    givenLiveSurrender(live);
    when(licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        null,
        LIVE_CHANGE_ID
    )).thenReturn(ALREADY_OPERATED_ON);
    when(validator.hasErrors(any(PartialSurrenderDetailsForm.class), any(BindingResult.class),
        eq(BLOCK_FEATURES), eq(ALREADY_OPERATED_ON))).thenReturn(false);

    var form = new PartialSurrenderDetailsForm();
    form.setFeatureIds(new LinkedHashSet<>(List.of(BLOCK_30_1A.getId(), BLOCK_30_2.getId())));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .submitForCorrectingChange(correction, licencePosition, LIVE_CHANGE, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verify(licencePositionStagedPayloadService)
        .blockFeatureIdsAlreadyOperatedOnForExecutedPosition(correction, licencePosition, null, LIVE_CHANGE_ID);
    verify(partialSurrenderCorrectionService).correctExistingPartialSurrender(
        eq(correction), eq(licencePosition), eq(LIVE_CHANGE_ID), any(PartialSurrenderOperation.class));
  }

  private PartialSurrenderOperation givenLiveSurrender(PartialSurrenderOperation liveSurrender) {
    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(LIVE_CHANGE_ID)).thenReturn(liveSurrender);
    return liveSurrender;
  }

  private static PartialSurrenderOperation surrenderOf(Feature block) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(block.getId()))
        .build();
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
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
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition.getId(), stagedChangeId))
        .thenReturn(blockFeatures);
  }

  private String givenStagedSurrenderChangeId(
      LicencePositionStagedPayload positionStagedPayload,
      PartialSurrenderOperation staged
  ) {
    var stagedChange = AddChange.buildOperationsChange(List.of(staged), 1);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionStagedPayload))
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

  private LicencePositionStagedPayload addedPositionStagedPayload(LicenceCorrection correction) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withLicencePositionId(STAGED_POSITION_ID.toString())
            .withEffectiveDate(POSITION_DATE)
            .withEffectiveDateOrder(POSITION_DATE_ORDER)
            .build())
        .build();
  }

  private LicencePositionStagedPayload updatePositionStagedPayload(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(licencePosition)
        .withPayload(LicencePositionPayload.newUpdateLicencePositionPayload().build())
        .build();
  }

  private void givenBlockFeaturesForAddedPosition(
      LicencePositionStagedPayload positionStagedPayload,
      List<Feature> blockFeatures,
      @Nullable String stagedChangeId
  ) {
    when(licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        positionStagedPayload.getLicenceCorrection(),
        positionStagedPayload.getPositionId(),
        stagedChangeId
    )).thenReturn(blockFeatures);
  }

  private void givenNoUpdatePositionStagedPayload(LicenceCorrection correction, LicencePosition licencePosition) {
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(correction, licencePosition))
        .thenReturn(Optional.empty());
    when(licencePositionStagedPayloadService.findPositionStagedPayload(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(Optional.empty());
  }

  private LicencePositionStagedPayload givenStagedSurrenderOnUpdatePositionStagedPayload(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      PartialSurrenderOperation staged
  ) {
    var positionStagedPayload = updatePositionStagedPayload(correction, licencePosition);
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(correction, licencePosition))
        .thenReturn(Optional.of(positionStagedPayload));
    when(licencePositionStagedPayloadService.findPositionStagedPayload(new CorrectionPosition.Executed(correction, licencePosition)))
        .thenReturn(Optional.of(positionStagedPayload));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload))
        .thenReturn(Optional.of(staged));
    return positionStagedPayload;
  }

  private static String addChangeUrlForExecutedPosition() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForPosition(new CorrectionPosition.Executed(CORRECTION, POSITION)));
  }

  private static String addChangeUrlFor(LicencePositionStagedPayload positionStagedPayload) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForPosition(new CorrectionPosition.Staged(positionStagedPayload)));
  }

  private static String defineAreaUrl() {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
        .renderDefineArea(CORRECTION, POSITION_STAGED_PAYLOAD, BLOCK_30_1A.getId()));
  }

  private static String startCorrectionUrl() {
    return ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
        .startCorrectingSingleBlockChange(CORRECTION, POSITION, LIVE_CHANGE));
  }

  private static String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_STAGED_PAYLOAD, null));
  }

  private static String correctingChangeTaskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, null));
  }
}

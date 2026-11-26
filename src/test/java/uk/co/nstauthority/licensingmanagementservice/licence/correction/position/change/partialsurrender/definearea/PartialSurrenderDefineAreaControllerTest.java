package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
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

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.relationship.BoundingBox;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderReviewAndSubmitController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = {
    PartialSurrenderDefineAreaController.class
})
@ActiveProfiles("test")
class PartialSurrenderDefineAreaControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final UUID STAGED_POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder().build();
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
  private static final Feature FEATURE = FeatureTestUtil.builder()
      .withCoordinateSystem(CoordinateSystem.ED50)
      .build();
  private static final UUID FEATURE_ID = FEATURE.getId();
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final int ED50_WKID = 4230;
  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderDefineArea";
  private static final String SELECT_AREAS_VIEW_NAME =
      "lms/licence/correction/change/partialSurrender/partialSurrenderSelectAreas";
  private static final Feature FIRST_AREA = FeatureTestUtil.builder()
      .withFeatureName("30/1a_1")
      .withCoordinateSystem(CoordinateSystem.ED50)
      .build();
  private static final Feature SECOND_AREA = FeatureTestUtil.builder()
      .withFeatureName("30/1a_2")
      .withCoordinateSystem(CoordinateSystem.ED50)
      .build();
  private static final Feature OTHER_ACTIVE_BLOCK = FeatureTestUtil.builder().withFeatureName("30/2").build();
  private static final List<Feature> OTHER_ACTIVE_BLOCKS = List.of(OTHER_ACTIVE_BLOCK);
  private static final Map<Line, BoundingBox> OTHER_ACTIVE_BLOCK_LINES =
      Map.of(new Line(), new BoundingBox(0, 0, 1, 1));

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private CommandJourneyService commandJourneyService;

  @MockitoBean
  private PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;

  @MockitoBean
  private PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator;

  @MockitoBean
  private PartialSurrenderValidator partialSurrenderValidator;

  @MockitoBean
  private FeatureRelationshipService featureRelationshipService;

  @Test
  void renderDefineArea_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderDefineArea_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderDefineArea_whenLicenceTypeIsNotAllowed_forbidden() throws Exception {
    var notAllowedLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.GAS_STORAGE).build();
    var notAllowedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(notAllowedLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(notAllowedCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(notAllowedCorrection));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class,
      names = {"CARBON_STORAGE", "SEAWARD_PRODUCTION", "LANDWARD_PRODUCTION"})
  void renderDefineArea_rendersMapWithCommandJourneyAndSrsWkid(LicenceType licenceType) throws Exception {
    var licence = LicenceTestUtil.builder().withLicenceType(licenceType).withLicenceReference("P/1").build();
    givenBlockSurrenderWithActiveFeatures(licence);

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("commandJourneyId", COMMAND_JOURNEY_ID),
            model().attribute("srsWkid", ED50_WKID),
            model().attribute("pageCaption", licence.getLicenceReference()),
            model().attribute("pageTitle", "Define area to surrender"),
            model().attribute("backLinkUrl", surrenderTypeUrl()));
  }

  @Test
  void renderDefineArea_whenCorrectingAnExecutedChange_backLinkGoesToTheCorrectingChangeRoute() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = correctingPositionStagedPayload(correction);
    givenBlockSurrender(positionStagedPayload, List.of(), List.of(FEATURE));
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", correctingChangeSurrenderTypeUrl()));
  }

  @Test
  void renderDefineArea_whenSingleBlockOnAddedPosition_backLinkGoesToAddChange() throws Exception {
    var positionStagedPayload = givenBlockSurrender(List.of(), List.of(FEATURE));

    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .thenReturn(Optional.of(FEATURE));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, STAGED_POSITION_ID))
        );
  }

  @Test
  void renderDefineArea_whenSingleBlockOnExecutedPosition_backLinkGoesToAddChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = correctingPositionStagedPayload(correction);

    givenBlockSurrender(positionStagedPayload, List.of(), List.of(FEATURE));
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .thenReturn(Optional.of(FEATURE));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId()))
        );
  }

  @Test
  void renderDefineArea_whenSingleBlockCorrectingAnExecutedChange_backLinkGoesToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = correctingPositionStagedPayload(correction);

    givenBlockSurrender(positionStagedPayload, List.of(), List.of(FEATURE));
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .thenReturn(Optional.of(FEATURE));
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId()))
        );
  }

  @Test
  void defineArea_whenBlockNotSplit_rendersFormWithError() throws Exception {
    givenBlockSurrenderWithActiveFeatures();

    when(partialSurrenderDefineAreaValidator.hasErrors(List.of(FEATURE))).thenReturn(true);

    mockMvc.perform(post(defineAreaUrl())
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("mapErrorMessage", "You must split the block before continuing"),
            model().attributeExists("errorSummaryItems")
        );
  }

  @Test
  void defineArea_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(post(defineAreaUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void defineArea_whenBlockSplit_removesStaleIdsAndRedirectsToSelectAreas() throws Exception {
    var positionStagedPayload = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));

    mockMvc.perform(post(defineAreaUrl())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(selectAreasUrl()));

    verify(partialSurrenderCorrectionService).clearSurrenderedIds(
        positionStagedPayload, FEATURE_ID, List.of(FIRST_AREA.getId(), SECOND_AREA.getId()));
  }

  @Test
  void renderSelectAreas_rendersMapCheckboxesAndPreTicksExistingSelection() throws Exception {
    givenBlockSurrender(List.of(FIRST_AREA.getId()), List.of(FIRST_AREA, SECOND_AREA));

    mockMvc.perform(get(selectAreasUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(SELECT_AREAS_VIEW_NAME),
            model().attribute("pageTitle", "Select the areas to surrender"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("srsWkid", ED50_WKID),
            model().attribute("areaCheckboxOptions",
                LicenceBlockFeatureUtil.toBlockCheckboxOptions(List.of(FIRST_AREA, SECOND_AREA))),
            model().attribute("activeFeatureIds", List.of(FIRST_AREA.getId(), SECOND_AREA.getId())),
            model().attribute("form", hasProperty("surrenderedFeatureIds", contains(FIRST_AREA.getId()))),
            model().attribute("backLinkUrl", defineAreaUrl())
        );
  }

  @Test
  void selectAreas_whenValidSelection_savesAndRedirectsToTaskList() throws Exception {
    var positionStagedPayload = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));
    givenOtherActiveBlocks(positionStagedPayload);

    givenSelectionErrors(false);
    givenOverlapErrors(false);

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(taskListUrl()));

    verify(partialSurrenderCorrectionService)
        .setSurrenderedFeatureIds(positionStagedPayload, FEATURE_ID, Set.of(FIRST_AREA.getId()));
  }

  @Test
  void selectAreas_whenSingleBlock_savesAndRedirectsToReviewAndSubmit() throws Exception {
    var positionStagedPayload = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));
    givenOtherActiveBlocks(positionStagedPayload);

    givenSelectionErrors(false);
    givenOverlapErrors(false);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .thenReturn(Optional.of(FEATURE));

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderReviewAndSubmitController.class)
            .renderReviewAndSubmit(CORRECTION, POSITION_STAGED_PAYLOAD, null))));

    verify(partialSurrenderCorrectionService)
        .setSurrenderedFeatureIds(positionStagedPayload, FEATURE_ID, Set.of(FIRST_AREA.getId()));
  }

  @Test
  void selectAreas_whenSingleBlockCorrectingAnExecutedChange_redirectsToCorrectingReviewAndSubmit() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = correctingPositionStagedPayload(correction);
    givenBlockSurrender(positionStagedPayload, List.of(), List.of(FIRST_AREA, SECOND_AREA));
    givenOtherActiveBlocks(positionStagedPayload);

    givenSelectionErrors(false);
    givenOverlapErrors(false);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .thenReturn(Optional.of(FEATURE));
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderReviewAndSubmitController.class)
            .renderReviewAndSubmitForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, null))));
  }

  @Test
  void selectAreas_whenSelectionErrors_rendersFormWithoutCheckingOverlap() throws Exception {
    givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));
    givenSelectionErrors(true);

    mockMvc.perform(post(selectAreasUrl())
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(SELECT_AREAS_VIEW_NAME)
        );

    verifyNoInteractions(partialSurrenderValidator, featureRelationshipService);
  }

  @Test
  void selectAreas_whenAreasKeptOverlap_renderForm() throws Exception {
    var positionStagedPayload = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));
    givenOtherActiveBlocks(positionStagedPayload);
    givenSelectionErrors(false);
    givenOverlapErrors(true);

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(SELECT_AREAS_VIEW_NAME)
        );

    verify(partialSurrenderCorrectionService, never()).setSurrenderedFeatureIds(any(), any(), any());
  }

  private void givenSelectionErrors(boolean hasErrors) {
    when(partialSurrenderSelectAreasFormValidator.hasSelectionErrors(
        any(PartialSurrenderSelectAreasForm.class),
        any(BindingResult.class),
        eq(List.of(FIRST_AREA, SECOND_AREA)),
        eq(FEATURE)
    )).thenReturn(hasErrors);
  }

  private void givenOverlapErrors(boolean hasErrors) {
    when(partialSurrenderSelectAreasFormValidator.hasOverlapErrors(
        any(PartialSurrenderSelectAreasForm.class),
        any(BindingResult.class),
        eq(List.of(FIRST_AREA, SECOND_AREA)),
        eq(OTHER_ACTIVE_BLOCK_LINES)
    )).thenReturn(hasErrors);
  }

  private void givenOtherActiveBlocks(LicencePositionStagedPayload positionStagedPayload) {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FEATURE_ID))
        .build();
    var positionDate = LocalDate.of(2026, Month.JUNE, 1);
    var stagedChangeId = UUID.randomUUID().toString();
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(surrender);
    when(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).thenReturn(positionDate);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionStagedPayload))
        .thenReturn(Optional.of(stagedChangeId));
    when(partialSurrenderValidator.getOtherActiveBlocks(surrender, positionDate, stagedChangeId))
        .thenReturn(OTHER_ACTIVE_BLOCKS);
    when(featureRelationshipService.getLineBoundingBoxes(OTHER_ACTIVE_BLOCKS)).thenReturn(OTHER_ACTIVE_BLOCK_LINES);
  }

  private void givenBlockSurrenderWithActiveFeatures() {
    givenBlockSurrenderWithActiveFeatures(LICENCE);
  }

  private void givenBlockSurrenderWithActiveFeatures(Licence licence) {
    var activeFeatures = new ArrayList<Feature>();
    for (var i = 0; i < 1; i++) {
      activeFeatures.add(FEATURE);
    }
    givenBlockSurrender(licence, List.of(), activeFeatures);
  }

  private LicencePositionStagedPayload givenBlockSurrender(
      List<UUID> surrenderedFeatureIds, List<Feature> activeFeatures
  ) {
    return givenBlockSurrender(LICENCE, surrenderedFeatureIds, activeFeatures);
  }

  private LicencePositionStagedPayload givenBlockSurrender(
      Licence licence, List<UUID> surrenderedFeatureIds, List<Feature> activeFeatures
  ) {
    var correction = givenCorrectionAllocatedToUser(licence);
    return givenBlockSurrender(positionStagedPayload(correction), surrenderedFeatureIds, activeFeatures);
  }

  private LicencePositionStagedPayload givenBlockSurrender(
      LicencePositionStagedPayload positionStagedPayload,
      List<UUID> surrenderedFeatureIds,
      List<Feature> activeFeatures
  ) {
    when(licencePositionStagedPayloadService.findById(POSITION_STAGED_PAYLOAD_ID))
        .thenReturn(Optional.of(positionStagedPayload));

    when(partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionStagedPayload, FEATURE_ID))
        .thenReturn(new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, surrenderedFeatureIds));

    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID)).thenReturn(activeFeatures);

    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionStagedPayload, FEATURE_ID))
        .thenReturn(FEATURE);

    return positionStagedPayload;
  }

  private LicenceCorrection givenCorrectionAllocatedToUser(Licence licence) {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(licence).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionStagedPayload positionStagedPayload(LicenceCorrection correction) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(STAGED_POSITION_ID.toString())
            .build())
        .build();
  }

  private LicencePositionStagedPayload correctingPositionStagedPayload(LicenceCorrection correction) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder()
            .withId(POSITION_ID)
            .withLicence(LICENCE)
            .build())
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
  }

  private static String defineAreaUrl() {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
        .renderDefineArea(CORRECTION, POSITION_STAGED_PAYLOAD, FEATURE_ID));
  }

  private static String selectAreasUrl() {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
        .renderSelectAreas(CORRECTION, POSITION_STAGED_PAYLOAD, FEATURE_ID));
  }

  private static String surrenderTypeUrl() {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class)
        .renderSurrenderTypeForm(CORRECTION, POSITION_STAGED_PAYLOAD, FEATURE_ID));
  }

  private static String correctingChangeSurrenderTypeUrl() {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class)
        .renderSurrenderTypeFormForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, FEATURE_ID));
  }

  private static String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_STAGED_PAYLOAD, null));
  }
}

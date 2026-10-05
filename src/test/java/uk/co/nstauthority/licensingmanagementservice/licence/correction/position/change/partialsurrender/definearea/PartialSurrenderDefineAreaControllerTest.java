package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.ArrayList;
import java.util.List;
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
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
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
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
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
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
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

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private CommandJourneyService commandJourneyService;

  @MockitoBean
  private PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;

  @MockitoBean
  private PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator;

  @Test
  void renderDefineArea_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderDefineArea_whenLicenceTypeIsNotAllowed_forbidden() throws Exception {
    var notAllowedLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.GAS_STORAGE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(notAllowedLicence)
            .build()));

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
    var positionCorrection = correctingPositionCorrection(correction);
    givenBlockSurrender(positionCorrection, List.of(), List.of(FEATURE));
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
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
    var positionCorrection = givenBlockSurrender(List.of(), List.of(FEATURE));

    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(FEATURE));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(CORRECTION, POSITION_CORRECTION)))
        );
  }

  @Test
  void renderDefineArea_whenSingleBlockOnExecutedPosition_backLinkGoesToAddChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = correctingPositionCorrection(correction);

    givenBlockSurrender(positionCorrection, List.of(), List.of(FEATURE));
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(FEATURE));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION)))
        );
  }

  @Test
  void renderDefineArea_whenSingleBlockCorrectingAnExecutedChange_backLinkGoesToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = correctingPositionCorrection(correction);

    givenBlockSurrender(positionCorrection, List.of(), List.of(FEATURE));
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(FEATURE));
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(get(defineAreaUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION)))
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
  void defineArea_whenBlockSplit_removesStaleIdsAndRedirectsToSelectAreas() throws Exception {
    var positionCorrection = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));

    mockMvc.perform(post(defineAreaUrl())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(selectAreasUrl()));

    verify(partialSurrenderCorrectionService).clearSurrenderedIds(
        positionCorrection, FEATURE_ID, List.of(FIRST_AREA.getId(), SECOND_AREA.getId()));
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
    var positionCorrection = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));

    when(partialSurrenderSelectAreasFormValidator.hasErrors(
        any(PartialSurrenderSelectAreasForm.class),
        any(BindingResult.class),
        eq(List.of(FIRST_AREA, SECOND_AREA))
    )).thenReturn(false);

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(taskListUrl()));

    verify(partialSurrenderCorrectionService)
        .setSurrenderedFeatureIds(positionCorrection, FEATURE_ID, Set.of(FIRST_AREA.getId()));
  }

  @Test
  void selectAreas_whenSingleBlock_savesAndRedirectsToReviewAndSubmit() throws Exception {
    var positionCorrection = givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));

    when(partialSurrenderSelectAreasFormValidator.hasErrors(
        any(PartialSurrenderSelectAreasForm.class),
        any(BindingResult.class),
        eq(List.of(FIRST_AREA, SECOND_AREA))
    )).thenReturn(false);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(FEATURE));

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderReviewAndSubmit(CORRECTION, POSITION_CORRECTION, null))));

    verify(partialSurrenderCorrectionService)
        .setSurrenderedFeatureIds(positionCorrection, FEATURE_ID, Set.of(FIRST_AREA.getId()));
  }

  @Test
  void selectAreas_whenSingleBlockCorrectingAnExecutedChange_redirectsToCorrectingReviewAndSubmit() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = correctingPositionCorrection(correction);
    givenBlockSurrender(positionCorrection, List.of(), List.of(FIRST_AREA, SECOND_AREA));

    when(partialSurrenderSelectAreasFormValidator.hasErrors(
        any(PartialSurrenderSelectAreasForm.class),
        any(BindingResult.class),
        eq(List.of(FIRST_AREA, SECOND_AREA))
    )).thenReturn(false);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(FEATURE));
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(post(selectAreasUrl())
            .param("surrenderedFeatureIds", FIRST_AREA.getId().toString())
            .with(user(regulatorUser)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderReviewAndSubmitForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, null))));
  }

  @Test
  void selectAreas_whenInvalid_renderForm() throws Exception {
    givenBlockSurrender(List.of(), List.of(FIRST_AREA, SECOND_AREA));

    when(partialSurrenderSelectAreasFormValidator.hasErrors(
        any(PartialSurrenderSelectAreasForm.class),
        any(BindingResult.class),
        eq(List.of(FIRST_AREA, SECOND_AREA))
    )).thenReturn(true);

    mockMvc.perform(post(selectAreasUrl())
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(SELECT_AREAS_VIEW_NAME)
        );
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

  private LicencePositionCorrection givenBlockSurrender(
      List<UUID> surrenderedFeatureIds, List<Feature> activeFeatures
  ) {
    return givenBlockSurrender(LICENCE, surrenderedFeatureIds, activeFeatures);
  }

  private LicencePositionCorrection givenBlockSurrender(
      Licence licence, List<UUID> surrenderedFeatureIds, List<Feature> activeFeatures
  ) {
    var correction = givenCorrectionAllocatedToUser(licence);
    return givenBlockSurrender(positionCorrection(correction), surrenderedFeatureIds, activeFeatures);
  }

  private LicencePositionCorrection givenBlockSurrender(
      LicencePositionCorrection positionCorrection,
      List<UUID> surrenderedFeatureIds,
      List<Feature> activeFeatures
  ) {
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(positionCorrection));

    when(partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, surrenderedFeatureIds));

    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID)).thenReturn(activeFeatures);

    return positionCorrection;
  }

  private LicenceCorrection givenCorrectionAllocatedToUser(Licence licence) {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(licence).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionCorrection positionCorrection(LicenceCorrection correction) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .build();
  }

  private LicencePositionCorrection correctingPositionCorrection(LicenceCorrection correction) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LicencePositionTestUtil.newBuilder()
            .withId(POSITION_ID)
            .withLicence(LICENCE)
            .build())
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
  }

  private static String defineAreaUrl() {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
        .renderDefineArea(CORRECTION, POSITION_CORRECTION, FEATURE_ID));
  }

  private static String selectAreasUrl() {
    return ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
        .renderSelectAreas(CORRECTION, POSITION_CORRECTION, FEATURE_ID));
  }

  private static String surrenderTypeUrl() {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class)
        .renderSurrenderTypeForm(CORRECTION, POSITION_CORRECTION, FEATURE_ID));
  }

  private static String correctingChangeSurrenderTypeUrl() {
    return ReverseRouter.route(on(BlockSurrenderTypeController.class)
        .renderSurrenderTypeFormForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, FEATURE_ID));
  }

  private static String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_CORRECTION, null));
  }
}

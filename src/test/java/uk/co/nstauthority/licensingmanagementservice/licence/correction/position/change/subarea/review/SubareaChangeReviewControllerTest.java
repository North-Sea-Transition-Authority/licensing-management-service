package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static org.assertj.core.api.Assertions.assertThat;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;

@ContextConfiguration(classes = SubareaChangeReviewController.class)
class SubareaChangeReviewControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Feature BLOCK_FEATURE = FeatureTestUtil.builder()
      .withFeatureName("30/1")
      .build();
  private static final UUID BLOCK_FEATURE_ID = BLOCK_FEATURE.getId();
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final SubareaDetails ORIGINAL_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final List<SubareaTransfer> OPERATION_TRANSFERS = List.of(new SubareaTransfer(
      List.of(ORIGINAL_SUBAREA),
      List.of(new SubareaDetails(UUID.randomUUID(), "Subarea A2", "A2"))
  ));
  private static final List<SubareaTransfer> JOURNEY_TRANSFERS = List.of(new SubareaTransfer(
      List.of(ORIGINAL_SUBAREA),
      List.of(new SubareaDetails(UUID.randomUUID(), "Subarea A1", "A1"))
  ));
  private static final String VIEW_NAME = "lms/licence/correction/change/subarea/subareaChangeReview";
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
  private static final SummaryCard SUBAREA_CARD = SummaryCard.simpleSummaryCardWithHeading(
      "Subarea A",
      SummaryDataView.newStringKeyValue("Area", "10.00 km²")
  );
  private static final SummarySection OVERVIEW = new SummarySection(
      10,
      List.of(SummaryItem.withRows(null, List.of(new SummaryRow(List.of(
          new SummaryColumn("Before", new SummaryMapView(List.of(UUID.randomUUID()), 4230), List.of()),
          new SummaryColumn("After", new SummaryMapView(List.of(UUID.randomUUID()), 4230), List.of())
      ))))),
      SummarySectionDisplay.INLINE
  );
  private static final SummarySection CHANGES = new SummarySection(
      20,
      List.of(SummaryItem.withRows("Subarea A", List.of(new SummaryRow(List.of(
          new SummaryColumn("Before", null, List.of(SUBAREA_CARD)),
          new SummaryColumn("After", null, List.of(SUBAREA_CARD.withAction(SummaryCardAction.post("Change", "/change"))))
      )))).withTag(new Tag("Split", TagColour.TURQUOISE)))
  );
  private static final List<SummarySection> REVIEW = List.of(OVERVIEW, CHANGES);

  @MockitoBean
  private SubareaChangeSummarySectionService subareaChangeSummarySectionService;

  @MockitoBean
  private LicencePositionSpatialService licencePositionSpatialService;

  @MockitoBean
  private SubareaChangeService subareaChangeService;

  @MockitoBean
  private SubareaChangeValidator subareaChangeValidator;

  @MockitoBean
  private FeatureService featureService;

  @Test
  void renderStagedReview_whenNotAllocated_thenForbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(stagedReviewUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderStagedReview_whenLicenceIsNotProduction_thenForbidden() throws Exception {
    givenCorrectionAllocatedToUser(LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build())
        .build());

    mockMvc.perform(get(stagedReviewUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderStagedReview_whenCorrectionIsNotInProgress_thenForbidden() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(ReverseRouter.route(on(SubareaChangeReviewController.class)
            .renderStagedReview(correction, POSITION_CORRECTION, BLOCK_FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderStagedReview_whenCommandJourneyStarted_thenCanSubmit() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    givenStagedSubareaChange(COMMAND_JOURNEY_ID);
    when(subareaChangeService.getTransfers(COMMAND_JOURNEY_ID, List.of(ORIGINAL_SUBAREA))).thenReturn(JOURNEY_TRANSFERS);
    givenStagedReview(JOURNEY_TRANSFERS);

    mockMvc.perform(get(stagedReviewUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", "Review and submit subarea changes for Block 30/1"),
            model().attribute("pageCaption", "P/1"),
            model().attribute("summarySections", REVIEW),
            model().attribute("submitUrl", stagedReviewUrl()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(SubareaChangeController.class)
                .renderSubareaChange(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID))));
  }

  @Test
  void renderStagedReview_whenNoCommandJourney_thenCannotSubmit() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    givenStagedSubareaChange(null);
    givenStagedReview(OPERATION_TRANSFERS);

    var result = mockMvc.perform(get(stagedReviewUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("summarySections", REVIEW))
        .andReturn();

    assertThat(result.getModelAndView().getModel()).doesNotContainKey("submitUrl");
    assertThat(result.getModelAndView().getModel()).doesNotContainKey("backLinkUrl");
  }

  @Test
  void submit_whenChangeMade_thenCompletesChangeAndRedirectsToPosition() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    givenStagedSubareaChange(COMMAND_JOURNEY_ID);
    when(subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID)).thenReturn(false);

    mockMvc.perform(post(stagedReviewUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(LicencePositionCorrectionRouteUtil.getPositionPageUrl(POSITION_CORRECTION)),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change saved")
                .build()));

    verify(subareaChangeService).completeSubareaChange(POSITION_CORRECTION, BLOCK_FEATURE_ID);
  }

  @Test
  void submit_whenNoChangeMade_thenRendersReviewWithError() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    givenStagedSubareaChange(COMMAND_JOURNEY_ID);
    when(subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID)).thenReturn(true);
    when(subareaChangeService.getTransfers(COMMAND_JOURNEY_ID, List.of(ORIGINAL_SUBAREA))).thenReturn(JOURNEY_TRANSFERS);
    givenStagedReview(JOURNEY_TRANSFERS);

    var result = mockMvc.perform(post(stagedReviewUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("summarySections", REVIEW))
        .andReturn();

    assertThat(result.getModelAndView().getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new ErrorSummaryItem(
            1,
            "summaryaccordion-subarea-changes",
            "You must make a change to the subareas before submitting"
        )));
    verify(subareaChangeService, never()).completeSubareaChange(POSITION_CORRECTION, BLOCK_FEATURE_ID);
  }

  @Test
  void submit_whenCorrectionIsNotInProgress_thenForbidden() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(post(ReverseRouter.route(on(SubareaChangeReviewController.class)
            .submit(correction, POSITION_CORRECTION, BLOCK_FEATURE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void changeStagedSubareas() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    givenPositionCorrection();

    mockMvc.perform(post(stagedChangeSubareasUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(SubareaChangeController.class)
                .renderSubareaChange(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID))));

    verify(subareaChangeService).getOrCreateCommandJourney(POSITION_CORRECTION, BLOCK_FEATURE_ID);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void submit_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUser(correctionWithStatus(status));
    givenPositionCorrection();

    mockMvc.perform(post(stagedReviewUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verify(subareaChangeService, never()).completeSubareaChange(POSITION_CORRECTION, BLOCK_FEATURE_ID);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void changeStagedSubareas_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUser(correctionWithStatus(status));
    givenPositionCorrection();

    mockMvc.perform(post(stagedChangeSubareasUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verify(subareaChangeService, never()).getOrCreateCommandJourney(POSITION_CORRECTION, BLOCK_FEATURE_ID);
  }

  @Test
  void renderLiveReview() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    var change = givenLiveSubareaChange();
    givenLiveReview(change);

    var result = mockMvc.perform(get(liveReviewUrl(change))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", "Review and submit subarea changes for Block 30/1"),
            model().attribute("pageCaption", "P/1"),
            model().attribute("summarySections", REVIEW))
        .andReturn();

    assertThat(result.getModelAndView().getModel()).doesNotContainKey("submitUrl");
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void renderLiveReview_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUser(correctionWithStatus(status));
    var change = givenLiveSubareaChange();

    mockMvc.perform(get(liveReviewUrl(change))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void changeLiveSubareas() throws Exception {
    givenCorrectionAllocatedToUser(CORRECTION);
    var change = givenLiveSubareaChange();
    when(subareaChangeService.correctExistingSubareaChange(CORRECTION, POSITION, change)).thenReturn(POSITION_CORRECTION);
    when(subareaChangeService.getLiveSubareaOperationOrThrow(change))
        .thenReturn(LicenceOperation.newSubAreaOperation().withBlockFeatureId(BLOCK_FEATURE_ID).build());

    mockMvc.perform(post(liveChangeSubareasUrl(change))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(SubareaChangeController.class)
                .renderSubareaChange(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID))));
  }

  @ParameterizedTest
  @EnumSource(value = LicenceCorrectionStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "IN_PROGRESS")
  void changeLiveSubareas_whenCorrectionIsNotInProgress_thenForbidden(LicenceCorrectionStatus status) throws Exception {
    givenCorrectionAllocatedToUser(correctionWithStatus(status));
    var change = givenLiveSubareaChange();

    mockMvc.perform(post(liveChangeSubareasUrl(change))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  private void givenCorrectionAllocatedToUser(LicenceCorrection correction) {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
  }

  private static LicenceCorrection correctionWithStatus(LicenceCorrectionStatus status) {
    return LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .withStatus(status)
        .build();
  }

  private void givenPositionCorrection() {
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(POSITION_CORRECTION));
  }

  private void givenStagedSubareaChange(UUID commandJourneyId) {
    givenPositionCorrection();
    when(subareaChangeService.getSubareaOperationOrThrow(POSITION_CORRECTION, BLOCK_FEATURE_ID))
        .thenReturn(new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, OPERATION_TRANSFERS, commandJourneyId));
  }

  private void givenStagedReview(List<SubareaTransfer> transfers) {
    var stateWithoutChange = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(ORIGINAL_SUBAREA))
        .withSubareaEquity(BLOCK_FEATURE_ID, ORIGINAL_SUBAREA, 1, BigDecimal.valueOf(100));
    when(subareaChangeService.getSubareaChangeOrThrow(POSITION_CORRECTION, BLOCK_FEATURE_ID))
        .thenReturn(new AddChange(CHANGE_ID, 1, List.of()));
    when(licencePositionSpatialService.getStateGoingIntoChange(POSITION_CORRECTION, CHANGE_ID))
        .thenReturn(LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(ORIGINAL_SUBAREA)));
    when(licencePositionSpatialService.getStateAtEndOfPositionWithoutChange(POSITION_CORRECTION, CHANGE_ID))
        .thenReturn(stateWithoutChange);
    givenSummarySections(new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(ORIGINAL_SUBAREA),
        transfers,
        stateWithoutChange,
        stagedChangeSubareasUrl()
    ));
  }

  private void givenLiveReview(LicencePositionChange change) {
    var stateBefore = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(ORIGINAL_SUBAREA));
    var stateAtEnd = LicencePositionState.EMPTY.withBlock(
        BLOCK_FEATURE_ID,
        OPERATION_TRANSFERS.getFirst().outputSubareas()
    );
    when(subareaChangeService.getLiveSubareaOperationOrThrow(change))
        .thenReturn(new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, OPERATION_TRANSFERS, null));
    when(licencePositionSpatialService.getStateGoingIntoChange(CORRECTION, POSITION_ID, change.getId().toString()))
        .thenReturn(stateBefore);
    when(licencePositionSpatialService.getStateGoingIntoChange(CORRECTION, POSITION_ID, null))
        .thenReturn(stateAtEnd);
    givenSummarySections(new SubareaChangeSummaryContext.LiveChange(
        BLOCK_FEATURE_ID,
        List.of(ORIGINAL_SUBAREA),
        OPERATION_TRANSFERS,
        stateBefore,
        stateAtEnd,
        liveChangeSubareasUrl(change)
    ));
  }

  private void givenSummarySections(SubareaChangeSummaryContext context) {
    when(subareaChangeSummarySectionService.getSummarySections(context, regulatorUser)).thenReturn(REVIEW);
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK_FEATURE);
  }

  private LicencePositionChange givenLiveSubareaChange() {
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withLicencePosition(POSITION)
        .withOperations(List.of(LicenceOperation.newSubAreaOperation().withBlockFeatureId(BLOCK_FEATURE_ID).build()))
        .build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(POSITION);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionChangeService.findById(change.getId())).thenReturn(Optional.of(change));
    return change;
  }

  private static String stagedReviewUrl() {
    return ReverseRouter.route(on(SubareaChangeReviewController.class)
        .renderStagedReview(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID, null));
  }

  private static String stagedChangeSubareasUrl() {
    return ReverseRouter.route(on(SubareaChangeReviewController.class)
        .changeStagedSubareas(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID));
  }

  private static String liveReviewUrl(LicencePositionChange change) {
    return ReverseRouter.route(on(SubareaChangeReviewController.class)
        .renderLiveReview(CORRECTION, POSITION, change, null));
  }

  private static String liveChangeSubareasUrl(LicencePositionChange change) {
    return ReverseRouter.route(on(SubareaChangeReviewController.class)
        .changeLiveSubareas(CORRECTION, POSITION, change));
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.relationship.BoundingBox;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ContextConfiguration(classes = PartialSurrenderTaskListController.class)
@ActiveProfiles("test")
class PartialSurrenderTaskListControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(LIVE_CHANGE_ID))
      .build();

  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final String POSITION_REGULATOR_REFERENCE = "TRANSACTION-REF";
  private static final String ADDED_POSITION_CORRECTION_REFERENCE = "CORRECTION-REF";
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .withPositionDate(POSITION_DATE)
      .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder()
          .withRegulatorReference(POSITION_REGULATOR_REFERENCE)
          .build())
      .build();
  private static final PartialSurrenderOperation SURRENDER = LicenceOperation.newPartialSurrenderOperation()
      .withSurrenderedFeatureIds(List.of(FeatureTestUtil.builder().build().getId()))
      .build();
  private static final Feature BLOCK = FeatureTestUtil.builder().build();
  private static final Map<Line, BoundingBox> BLOCK_LINES = Map.of(new Line(), new BoundingBox(0, 0, 1, 1));
  private static final List<TaskListSection> SECTIONS = List.of(new TaskListSection("Surrender details", 10,
      List.of(new TaskListItem("Surrender details", TaskListLabel.COMPLETE, "/surrender-details"))));

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .build();

  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderTaskList";

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private PartialSurrenderTaskListService partialSurrenderTaskListService;

  @MockitoBean
  private PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;

  @MockitoBean
  private PartialSurrenderValidator partialSurrenderValidator;

  @MockitoBean
  private LicencePositionSpatialService licencePositionSpatialService;

  @Test
  void renderTaskList_whenExecutedPosition_thenBackLinkGoesToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(
        correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PartialSurrenderTaskListController.TASK_LIST_PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("positionReference", POSITION_REGULATOR_REFERENCE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("taskListSections", SECTIONS),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION, null))));
  }

  @Test
  void renderTaskList_whenTheStagedSurrenderCorrectsALiveChange_thenRedirectsToTheLiveChangeTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(
        correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verifyNoInteractions(partialSurrenderTaskListService);
  }

  @Test
  void renderTaskList_whenSingleBlock_thenRedirectsToDefineArea() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(
        correction,
        LicencePositionCorrectionChangeType.UPDATE_POSITION
    );

    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(BLOCK));

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderDefineArea(CORRECTION, POSITION_CORRECTION, BLOCK.getId()))));

    verifyNoInteractions(partialSurrenderTaskListService);
  }

  @Test
  void renderTaskList_whenAddedPosition_thenBackLinkGoesToTheAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.ADD_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("positionReference", ADDED_POSITION_CORRECTION_REFERENCE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null))));
  }

  @Test
  void renderTaskList_whenPositionIsBeingRemoved_thenThrows() {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(
        correction, LicencePositionCorrectionChangeType.REMOVE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

    assertThatThrownBy(() -> mockMvc.perform(get(taskListUrl()).with(user(regulatorUser))))
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Licence position correction %s removes a position so cannot have changes made against it"
            .formatted(POSITION_CORRECTION_ID));
  }

  @Test
  void renderTaskList_whenNoPartialSurrenderStaged_thenNotFound() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(
        correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenThrow(new LmsEntityNotFoundException("no partial surrender"));

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void renderTaskList_whenNotAllocated_thenForbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION" })
  void renderTaskList_whenLicenceTypeIsNotAllowed_thenForbidden(LicenceType licenceType) throws Exception {
    givenCorrectionAllocatedToUser(LicenceTestUtil.builder().withLicenceType(licenceType).build());

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, mode = EnumSource.Mode.INCLUDE, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION" })
  void renderTaskList_whenLicenceTypeIsAllowed_thenOk(LicenceType licenceType) throws Exception {
    var allowedLicence = LicenceTestUtil.builder()
        .withLicenceType(licenceType)
        .withLicenceReference("P/1")
        .build();
    var correction = givenCorrectionAllocatedToUser(allowedLicence);
    var positionCorrection = givenPositionCorrection(
        correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk());
  }

  @Test
  void renderForCorrectingChange_thenRendersFromTheLiveChangeWithTheBackLinkToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.LiveChange(correction, POSITION, LIVE_CHANGE), regulatorUser))
        .thenReturn(SECTIONS);

    mockMvc.perform(get(correctingChangeTaskListUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PartialSurrenderTaskListController.TASK_LIST_PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("positionReference", POSITION_REGULATOR_REFERENCE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("taskListSections", SECTIONS),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION, null))));
  }

  @Test
  void renderForCorrectingChange_whenSingleBlock_thenRedirectsToCorrectSurrenderDetails() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var positionCorrection = givenUpdatePositionCorrectionFound(correction);

    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        correction,
        POSITION,
        positionCorrection,
        LIVE_CHANGE_ID
    )).thenReturn(Optional.of(BLOCK));

    mockMvc.perform(get(correctingChangeTaskListUrl()).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE))));

    verifyNoInteractions(partialSurrenderTaskListService);
  }

  @Test
  void renderForCorrectingChange_whenNotAllocated_thenForbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(correctingChangeTaskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForCorrectingChange_whenLicenceTypeIsNotAllowed_thenForbidden() throws Exception {
    givenCorrectionAllocatedToUser(LicenceTestUtil.builder().withLicenceType(LicenceType.GAS_STORAGE).build());

    mockMvc.perform(get(correctingChangeTaskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForCorrectingChange_whenPositionIsNotOnTheCorrectionLicence_thenNotFound() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionNotOnCorrectionLicence(correction);

    mockMvc.perform(get(correctingChangeTaskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void renderReviewAndSubmitForCorrectingChange() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var summarySections = List.of(new SummarySection(10, List.of(SummaryItem.withCards("Surrender details", List.of()))));
    var surrender = givenSurrenderUnderCorrection(correction);
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(licencePositionSpatialService.getStateGoingIntoChange(correction, POSITION_ID, LIVE_CHANGE_ID))
        .thenReturn(state);
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.LiveChange(correction, POSITION, LIVE_CHANGE_ID, surrender, state),
        regulatorUser
    )).thenReturn(summarySections);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, POSITION)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit"),
            model().attribute("pageTitle", PartialSurrenderTaskListController.REVIEW_AND_SUBMIT_PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("summarySections", summarySections),
            model().attribute("accordionId", LIVE_CHANGE_ID),
            model().attribute("errorSummaryItems", List.of()),
            model().attribute("backLinkUrl", correctingChangeTaskListUrl()),
            model().attribute("backLinkText", "Back to task list")
        );
  }

  @Test
  void renderReviewAndSubmitForCorrectingChange_whenSingleBlock_thenBackLinkGoesToSelectAreas() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var positionCorrection = givenUpdatePositionCorrectionFound(correction);
    givenSurrenderUnderCorrection(correction);
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
        .thenReturn(Optional.of(LIVE_CHANGE_ID));
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(BLOCK));
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, POSITION)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
                .renderSelectAreas(CORRECTION, POSITION_CORRECTION, BLOCK.getId()))),
            model().attribute("backLinkText", "Back")
        );
  }

  @Test
  void renderReviewAndSubmitForCorrectingChange_whenStagedCorrectionIsForAnotherChange_thenBackLinkGoesToTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var positionCorrection = givenUpdatePositionCorrectionFound(correction);
    givenSurrenderUnderCorrection(correction);
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection))
        .thenReturn(Optional.of(UUID.randomUUID().toString()));
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, POSITION)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", correctingChangeTaskListUrl()),
            model().attribute("backLinkText", "Back to task list")
        );

    verify(partialSurrenderCorrectionService, never()).findSingleBlockNotOperatedOn(positionCorrection);
  }

  @Test
  void renderReviewAndSubmitForCorrectingChange_whenFormErrors_thenErrorSummaryItemsAddedAndBlockLinesNotLoaded()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var surrender = givenSurrenderUnderCorrection(correction);
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(licencePositionSpatialService.getStateGoingIntoChange(correction, POSITION_ID, LIVE_CHANGE_ID))
        .thenReturn(state);
    when(partialSurrenderValidator.validateForms(surrender, state)).thenReturn(List.of("First error", "Second error"));

    var modelAndView = mockMvc.perform(get(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            new ErrorSummaryItem(0, "summaryaccordion-" + LIVE_CHANGE_ID, "First error"),
            new ErrorSummaryItem(1, "summaryaccordion-" + LIVE_CHANGE_ID, "Second error")
        ));
    verify(partialSurrenderValidator, never()).getBlockLineBoundingBoxes(any());
  }

  @Test
  void renderReviewAndSubmitForCorrectingChange_whenFormsValid_thenSpatialErrorSummaryItemsAdded() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var surrender = givenSurrenderUnderCorrection(correction);
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(licencePositionSpatialService.getStateGoingIntoChange(correction, POSITION_ID, LIVE_CHANGE_ID))
        .thenReturn(state);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, POSITION)).thenReturn(POSITION_DATE);
    when(partialSurrenderValidator.validateForms(surrender, state)).thenReturn(List.of());
    when(partialSurrenderValidator.getBlockLineBoundingBoxes(Set.of(POSITION_DATE))).thenReturn(BLOCK_LINES);
    when(partialSurrenderValidator.validateSpatially(surrender, state, POSITION_DATE, LIVE_CHANGE_ID, BLOCK_LINES))
        .thenReturn(List.of("First error", "Second error"));

    var modelAndView = mockMvc.perform(get(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            new ErrorSummaryItem(0, "summaryaccordion-" + LIVE_CHANGE_ID, "First error"),
            new ErrorSummaryItem(1, "summaryaccordion-" + LIVE_CHANGE_ID, "Second error")
        ));
  }

  @Test
  void renderReviewAndSubmitForCorrectingChange_whenPositionIsNotOnTheCorrectionLicence_thenNotFound()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionNotOnCorrectionLicence(correction);

    mockMvc.perform(get(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void renderReviewAndSubmit() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var summarySections = List.of(new SummarySection(10, List.of(SummaryItem.withCard(
        "Surrender details",
        SummaryCard.simpleSummaryCard(SummaryDataView.newStringKeyValue("Key", "Value"))
        )))
    );
    var state = givenStagedSurrender(positionCorrection);
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.Staged(positionCorrection, SURRENDER, state),
        regulatorUser
    )).thenReturn(summarySections);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit"),
            model().attribute("pageTitle", PartialSurrenderTaskListController.REVIEW_AND_SUBMIT_PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("summarySections", summarySections),
            model().attribute("accordionId", POSITION_CORRECTION_ID),
            model().attribute("errorSummaryItems", List.of()),
            model().attribute("backLinkUrl", taskListUrl()),
            model().attribute("backLinkText", "Back to task list")
        );
  }

  @Test
  void renderReviewAndSubmit_whenSingleBlock_thenBackLinkGoesToSelectAreas() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection))
        .thenReturn(Optional.of(BLOCK));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("backLinkUrl", ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
                .renderSelectAreas(CORRECTION, POSITION_CORRECTION, BLOCK.getId()))),
            model().attribute("backLinkText", "Back")
        );
  }

  @Test
  void renderReviewAndSubmit_whenABlockIsPartiallySurrendered_thenBeforeAndAfterMapSectionsInModel() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var summarySections = List.of(new SummarySection(20, List.of(SummaryItem.withCard(
        "Block 30/1",
        SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
            .addStringValue("Type of surrender", "Partial surrender")
            .addMapValue("Before", new SummaryMapView(List.of(UUID.randomUUID()), 4230))
            .addMapValue("After", new SummaryMapView(List.of(UUID.randomUUID()), 4230))
            .build())))));
    var state = givenStagedSurrender(positionCorrection);
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.Staged(positionCorrection, SURRENDER, state),
        regulatorUser
    )).thenReturn(summarySections);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("summarySections", summarySections),
            model().attribute("accordionId", POSITION_CORRECTION_ID)
        );
  }

  @Test
  void renderReviewAndSubmit_whenFormErrors_thenErrorSummaryItemsAddedAndBlockLinesNotLoaded() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var stagedChangeId = UUID.randomUUID().toString();
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(stagedChangeId));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection)).thenReturn(SURRENDER);
    when(licencePositionSpatialService.getStateGoingIntoChange(positionCorrection, stagedChangeId)).thenReturn(state);
    when(partialSurrenderValidator.validateForms(SURRENDER, state)).thenReturn(List.of("First error", "Second error"));

    var modelAndView = mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            new ErrorSummaryItem(0, "summaryaccordion-" + POSITION_CORRECTION_ID, "First error"),
            new ErrorSummaryItem(1, "summaryaccordion-" + POSITION_CORRECTION_ID, "Second error")
        ));
    verify(partialSurrenderValidator, never()).getBlockLineBoundingBoxes(any());
  }

  @Test
  void renderReviewAndSubmit_whenFormsValid_thenSpatialErrorSummaryItemsAdded() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var stagedChangeId = UUID.randomUUID().toString();
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(stagedChangeId));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection)).thenReturn(SURRENDER);
    when(licencePositionSpatialService.getStateGoingIntoChange(positionCorrection, stagedChangeId)).thenReturn(state);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderValidator.validateForms(SURRENDER, state)).thenReturn(List.of());
    when(partialSurrenderValidator.getBlockLineBoundingBoxes(Set.of(POSITION_DATE))).thenReturn(BLOCK_LINES);
    when(partialSurrenderValidator.validateSpatially(SURRENDER, state, POSITION_DATE, stagedChangeId, BLOCK_LINES))
        .thenReturn(List.of("First error", "Second error"));

    var modelAndView = mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            new ErrorSummaryItem(0, "summaryaccordion-" + POSITION_CORRECTION_ID, "First error"),
            new ErrorSummaryItem(1, "summaryaccordion-" + POSITION_CORRECTION_ID, "Second error")
        ));
  }

  @Test
  void renderReviewAndSubmit_whenInlineAndAccordionSections_thenInlineSectionsShownAboveTaggedAccordion()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var summarySections = List.of(
        new SummarySection(10, List.of(SummaryItem.withCard(
            "Surrender details",
            SummaryCard.simpleSummaryCard(SummaryDataView.newStringKeyValue("Licence", "P/1"))
        )), SummarySectionDisplay.INLINE),
        new SummarySection(20, List.of(SummaryItem.withCard(
            "Block 30/1",
            SummaryCard.simpleSummaryCard(SummaryDataView.newStringKeyValue("Area", "200.00 km²"))
        ).withTag(new Tag("Partial surrender", TagColour.BLUE))))
    );
    givenSummarySections(positionCorrection, summarySections);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            content().string(stringContainsInOrder(
                "<h2 class=\"govuk-heading-l\">Surrender details</h2>",
                "govuk-accordion",
                ">Block 30/1</span>",
                "<strong class=\"govuk-tag govuk-tag--blue\">Partial surrender</strong>"
            )),
            content().string(containsString("Submit")),
            content().string(not(containsString("govuk-button--disabled")))
        );
  }

  @Test
  void renderReviewAndSubmit_whenItemHasRows_thenEachColumnsHeadingMapAndCardsShownTogether() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var blockRow = new SummaryRow(List.of(
        new SummaryColumn("Before", null, List.of(headedCard("Block 30/1"))),
        new SummaryColumn("After", null, List.of(headedCard("Block 30/1 kept")))
    ));
    var subareaRow = new SummaryRow("Subarea A", new Tag("Unchanged", TagColour.GREY), List.of(
        new SummaryColumn("Before", null, List.of(headedCard("Subarea A"))),
        new SummaryColumn("After", null, List.of(), "This subarea is kept unchanged.")
    ));
    var croppedSubareaRow = new SummaryRow("Subarea B", new Tag("Cropped", TagColour.BLUE), List.of(
        new SummaryColumn("Before", new SummaryMapView(List.of(UUID.randomUUID()), 4230), List.of(headedCard("Subarea B"))),
        new SummaryColumn("After", new SummaryMapView(List.of(UUID.randomUUID()), 4230), List.of(headedCard("Subarea B1")))
    ));
    givenSummarySections(positionCorrection, List.of(new SummarySection(20, List.of(
        SummaryItem.withRows("Block 30/1", List.of(blockRow, subareaRow, croppedSubareaRow))
    ))));
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            content().string(stringContainsInOrder(
                "<h3 class=\"govuk-heading-m\">Before</h3>",
                "<h4 class=\"govuk-summary-card__title\">Block 30/1</h4>",
                "<h3 class=\"govuk-heading-m\">After</h3>",
                "<h4 class=\"govuk-summary-card__title\">Block 30/1 kept</h4>",
                "Subarea A",
                "<strong class=\"govuk-tag govuk-tag--grey\">Unchanged</strong>",
                "<h4 class=\"govuk-heading-s\">Before</h4>",
                "<h5 class=\"govuk-summary-card__title\">Subarea A</h5>",
                "<h4 class=\"govuk-heading-s\">After</h4>",
                "govuk-inset-text",
                "This subarea is kept unchanged.",
                "Subarea B",
                "<strong class=\"govuk-tag govuk-tag--blue\">Cropped</strong>",
                "<h4 class=\"govuk-heading-s\">Before</h4>",
                "data-gis-component",
                "<h5 class=\"govuk-summary-card__title\">Subarea B</h5>",
                "<h4 class=\"govuk-heading-s\">After</h4>",
                "data-gis-component",
                "<h5 class=\"govuk-summary-card__title\">Subarea B1</h5>"
            ))
        );
  }

  @Test
  void renderReviewAndSubmit_whenNotAllocated_thenForbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void submitReviewAndSubmit_whenNoErrors_thenRedirectsToPositionWithBanner() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    givenStagedSurrender(positionCorrection);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);

    mockMvc.perform(post(reviewAndSubmitUrl()).with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION, null))),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent(PartialSurrenderTaskListController.SUBMITTED_BANNER)
                .build())
        );
  }

  @Test
  void submitReviewAndSubmit_whenErrors_thenReviewAndSubmitShownWithErrors() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    var state = givenStagedSurrender(positionCorrection);
    when(partialSurrenderValidator.validateForms(SURRENDER, state)).thenReturn(List.of("First error"));

    var modelAndView = mockMvc.perform(post(reviewAndSubmitUrl()).with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit")
        )
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new ErrorSummaryItem(0, "summaryaccordion-" + POSITION_CORRECTION_ID, "First error")));
  }

  @Test
  void submitReviewAndSubmitForCorrectingChange_whenNoErrors_thenRedirectsToPositionWithBanner() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    givenSurrenderUnderCorrection(correction);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, POSITION)).thenReturn(POSITION_DATE);

    mockMvc.perform(post(reviewAndSubmitForCorrectingChangeUrl()).with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION, POSITION, null))),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent(PartialSurrenderTaskListController.SUBMITTED_BANNER)
                .build())
        );
  }

  @Test
  void submitReviewAndSubmitForCorrectingChange_whenErrors_thenReviewAndSubmitShownWithErrors() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var surrender = givenSurrenderUnderCorrection(correction);
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(licencePositionSpatialService.getStateGoingIntoChange(correction, POSITION_ID, LIVE_CHANGE_ID))
        .thenReturn(state);
    when(partialSurrenderValidator.validateForms(surrender, state)).thenReturn(List.of("First error"));

    var modelAndView = mockMvc.perform(post(reviewAndSubmitForCorrectingChangeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit")
        )
        .andReturn().getModelAndView();

    assertThat(modelAndView.getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new ErrorSummaryItem(0, "summaryaccordion-" + LIVE_CHANGE_ID, "First error")));
  }

  private void givenSummarySections(
      LicencePositionCorrection positionCorrection,
      List<SummarySection> summarySections
  ) {
    var state = givenStagedSurrender(positionCorrection);
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.Staged(positionCorrection, SURRENDER, state),
        regulatorUser
    )).thenReturn(summarySections);
  }

  private SummaryCard headedCard(String heading) {
    return SummaryCard.simpleSummaryCardWithHeading(heading, SummaryDataView.newStringKeyValue("Area", "1.00 km²"));
  }

  private LicencePositionState givenStagedSurrender(LicencePositionCorrection positionCorrection) {
    var stagedChangeId = UUID.randomUUID().toString();
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection))
        .thenReturn(Optional.of(stagedChangeId));
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection)).thenReturn(SURRENDER);
    when(licencePositionSpatialService.getStateGoingIntoChange(positionCorrection, stagedChangeId)).thenReturn(state);
    return state;
  }

  private String reviewAndSubmitUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderReviewAndSubmit(CORRECTION, POSITION_CORRECTION, null));
  }

  private String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_CORRECTION, null));
  }

  private String correctingChangeTaskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, null));
  }

  private PartialSurrenderOperation givenSurrenderUnderCorrection(LicenceCorrection correction) {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
    when(partialSurrenderCorrectionService.getSurrenderUnderCorrectionOrThrow(correction, POSITION, LIVE_CHANGE_ID))
        .thenReturn(surrender);

    return surrender;
  }

  private String reviewAndSubmitForCorrectingChangeUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderReviewAndSubmitForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, null));
  }

  private LicencePositionCorrection givenUpdatePositionCorrectionFound(LicenceCorrection correction) {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, POSITION))
        .thenReturn(Optional.of(positionCorrection));

    return positionCorrection;
  }

  private void givenPositionOnCorrectionLicence(LicenceCorrection correction) {
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(licencePositionChangeService.findById(LIVE_CHANGE.getId())).thenReturn(Optional.of(LIVE_CHANGE));
    when(licencePositionService.getPositionForLicence(correction.getLicence(), POSITION_ID)).thenReturn(POSITION);
  }

  private void givenPositionNotOnCorrectionLicence(LicenceCorrection correction) {
    var positionOnAnotherLicence = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LicenceTestUtil.builder().withId(correction.getLicence().getId() + 1).build())
        .build();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(positionOnAnotherLicence));
    when(licencePositionService.getPositionForLicence(correction.getLicence(), POSITION_ID))
        .thenThrow(new LmsEntityNotFoundException("licencePosition", POSITION_ID));
  }

  private LicenceCorrection givenCorrectionAllocatedToUser(Licence licence) {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(licence).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionCorrection givenPositionCorrection(
      LicenceCorrection correction,
      LicencePositionCorrectionChangeType changeType
  ) {
    var payload = changeType == LicencePositionCorrectionChangeType.ADD_POSITION
        ? CreateLicencePositionPayloadTestUtil.newBuilder()
            .withCorrectionReference(ADDED_POSITION_CORRECTION_REFERENCE)
            .build()
        : UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withChangeType(changeType)
        .withTargetLicencePosition(POSITION)
        .withPayload(payload)
        .build();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(positionCorrection));
    return positionCorrection;
  }
}

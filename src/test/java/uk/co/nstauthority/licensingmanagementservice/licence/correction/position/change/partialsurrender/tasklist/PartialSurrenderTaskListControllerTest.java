package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ContextConfiguration(classes = PartialSurrenderTaskListController.class)
@ActiveProfiles({"test", "enable-lms2"})
class PartialSurrenderTaskListControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();

  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final String POSITION_REGULATOR_REFERENCE = "TRANSACTION-REF";
  private static final String ADDED_POSITION_CORRECTION_REFERENCE = "CORRECTION-REF";
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder()
          .withRegulatorReference(POSITION_REGULATOR_REFERENCE)
          .build())
      .build();
  private static final PartialSurrenderOperation SURRENDER = LicenceOperation.newPartialSurrenderOperation()
      .withFeatureIds(List.of(FeatureTestUtil.builder().build().getId()))
      .build();
  private static final List<TaskListSection> SECTIONS = List.of(new TaskListSection("Surrender details", 10,
      List.of(new TaskListItem("Surrender details", TaskListLabel.COMPLETE, "/surrender-details"))));

  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderTaskList";

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private PartialSurrenderTaskListService partialSurrenderTaskListService;

  @MockitoBean
  private PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;

  @Test
  void renderTaskList_whenExecutedPosition_thenBackLinkGoesToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(
        correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

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
                .renderLicencePosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void renderTaskList_whenAddedPosition_thenBackLinkGoesToTheAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.ADD_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionCorrection))
        .thenReturn(SURRENDER);
    when(licencePositionCorrectionService.resolveEffectiveDate(positionCorrection)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("positionReference", ADDED_POSITION_CORRECTION_REFERENCE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
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
        new PartialSurrenderTaskListContext(positionCorrection), regulatorUser)).thenReturn(SECTIONS);

    assertThatThrownBy(() -> mockMvc.perform(get(taskListUrl()).with(user(regulatorUser))))
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Licence position correction %s removes a position so cannot carry a partial surrender"
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

  @Test
  void renderTaskList_whenLicenceIsNotProduction_thenForbidden() throws Exception {
    givenCorrectionAllocatedToUser(LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build());

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
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
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext(positionCorrection), regulatorUser)).thenReturn(summarySections);

    when(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).thenReturn(false);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit"),
            model().attribute("pageTitle", PartialSurrenderTaskListController.REVIEW_AND_SUBMIT_PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("summarySections", summarySections),
            model().attribute("allSurrenderedBlocksAreFull", false),
            model().attribute("backLinkUrl", taskListUrl())
        );
  }

  @Test
  void renderReviewAndSubmit_whenAllBlocksFullSurrender_thenValidationErrorFlagSet() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionCorrection = givenPositionCorrection(correction, LicencePositionCorrectionChangeType.UPDATE_POSITION);
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext(positionCorrection), regulatorUser)).thenReturn(List.of());
    when(partialSurrenderCorrectionService.allSurrenderedBlocksAreFull(positionCorrection)).thenReturn(true);

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("allSurrenderedBlocksAreFull", true)
        );
  }

  @Test
  void renderReviewAndSubmit_whenNotAllocated_thenForbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(reviewAndSubmitUrl()).with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  private String reviewAndSubmitUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderReviewAndSubmit(CORRECTION_ID, POSITION_CORRECTION_ID, null, null));
  }

  private String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION_ID, POSITION_CORRECTION_ID, null, null));
  }

  private LicenceCorrection givenCorrectionAllocatedToUser(Licence licence) {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(licence).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
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
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    return positionCorrection;
  }
}

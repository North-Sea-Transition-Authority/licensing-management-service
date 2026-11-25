package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
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
  private static final UUID POSITION_STAGED_PAYLOAD_ID = UUID.randomUUID();
  private static final UUID STAGED_POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String LIVE_CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(LIVE_CHANGE_ID))
      .build();

  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final String POSITION_REGULATOR_REFERENCE = "TRANSACTION-REF";
  private static final String ADDED_POSITION_STAGED_PAYLOAD_REFERENCE = "CORRECTION-REF";
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
  private static final List<TaskListSection> SECTIONS = List.of(new TaskListSection("Surrender details", 10,
      List.of(new TaskListItem("Surrender details", TaskListLabel.COMPLETE, "/surrender-details"))));

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionStagedPayload POSITION_STAGED_PAYLOAD = LicencePositionStagedPayloadTestUtil.newBuilder()
      .withId(POSITION_STAGED_PAYLOAD_ID)
      .withLicenceCorrection(CORRECTION)
      .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
          .withLicencePositionId(STAGED_POSITION_ID.toString())
          .build())
      .build();

  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderTaskList";

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private PartialSurrenderTaskListService partialSurrenderTaskListService;

  @Test
  void renderTaskList_whenExecutedPosition_thenBackLinkGoesToThePosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = givenPositionStagedPayload(
        correction, LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(SURRENDER);
    when(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionStagedPayload), regulatorUser)).thenReturn(SECTIONS);

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PartialSurrenderTaskListController.TASK_LIST_PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("positionReference", POSITION_REGULATOR_REFERENCE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("taskListSections", SECTIONS),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId())));
  }

  @Test
  void renderTaskList_whenTheStagedSurrenderCorrectsALiveChange_thenRedirectsToTheLiveChangeTaskList()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = givenPositionStagedPayload(
        correction, LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(SURRENDER);
    when(partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload))
        .thenReturn(Optional.of(LIVE_CHANGE.getId().toString()));

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(correctingChangeTaskListUrl()));

    verifyNoInteractions(partialSurrenderTaskListService);
  }

  @Test
  void renderTaskList_whenSingleBlock_thenRedirectsToDefineArea() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = givenPositionStagedPayload(
        correction,
        LicencePositionStagedPayloadChangeType.UPDATE_POSITION
    );

    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(SURRENDER);
    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload))
        .thenReturn(Optional.of(BLOCK));

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderDefineArea(CORRECTION, POSITION_STAGED_PAYLOAD, BLOCK.getId()))));

    verifyNoInteractions(partialSurrenderTaskListService);
  }

  @Test
  void renderTaskList_whenAddedPosition_thenBackLinkGoesToTheAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = givenPositionStagedPayload(correction, LicencePositionStagedPayloadChangeType.ADD_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(SURRENDER);
    when(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionStagedPayload), regulatorUser)).thenReturn(SECTIONS);

    mockMvc.perform(get(taskListUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("positionReference", ADDED_POSITION_STAGED_PAYLOAD_REFERENCE),
            model().attribute("positionDate", DateUtil.formatLongDate(POSITION_DATE)),
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, STAGED_POSITION_ID)));
  }

  @Test
  void renderTaskList_whenPositionIsBeingRemoved_thenThrows() {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = givenPositionStagedPayload(
        correction, LicencePositionStagedPayloadChangeType.REMOVE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(SURRENDER);
    when(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionStagedPayload), regulatorUser)).thenReturn(SECTIONS);

    assertThatThrownBy(() -> mockMvc.perform(get(taskListUrl()).with(user(regulatorUser))))
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Licence position staged payload %s removes a position so cannot have changes made against it"
            .formatted(POSITION_STAGED_PAYLOAD_ID));
  }

  @Test
  void renderTaskList_whenNoPartialSurrenderStaged_thenNotFound() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    var positionStagedPayload = givenPositionStagedPayload(
        correction, LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
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
  void renderTaskList_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(correction, POSITION_STAGED_PAYLOAD, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
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
    var positionStagedPayload = givenPositionStagedPayload(
        correction, LicencePositionStagedPayloadChangeType.UPDATE_POSITION);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(positionStagedPayload))
        .thenReturn(SURRENDER);
    when(licencePositionStagedPayloadService.resolveEffectiveDate(positionStagedPayload)).thenReturn(POSITION_DATE);
    when(partialSurrenderTaskListService.getTaskListSections(
        new PartialSurrenderTaskListContext.Staged(positionStagedPayload), regulatorUser)).thenReturn(SECTIONS);

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
            model().attribute("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(CORRECTION, POSITION.getId())));
  }

  @Test
  void renderForCorrectingChange_whenSingleBlock_thenRedirectsToCorrectSurrenderDetails() throws Exception {
    var correction = givenCorrectionAllocatedToUser(LICENCE);
    givenPositionOnCorrectionLicence(correction);
    var positionStagedPayload = givenUpdatePositionStagedPayloadFound(correction);

    when(partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        correction,
        POSITION,
        positionStagedPayload,
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

  private String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION, POSITION_STAGED_PAYLOAD, null));
  }

  private String correctingChangeTaskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(CORRECTION, POSITION, LIVE_CHANGE, null));
  }

  private LicencePositionStagedPayload givenUpdatePositionStagedPayloadFound(LicenceCorrection correction) {
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(POSITION)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder().build())
        .build();
    when(licencePositionStagedPayloadService.findUpdatePositionStagedPayload(correction, POSITION))
        .thenReturn(Optional.of(positionStagedPayload));

    return positionStagedPayload;
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

  private LicencePositionStagedPayload givenPositionStagedPayload(
      LicenceCorrection correction,
      LicencePositionStagedPayloadChangeType changeType
  ) {
    var payload = changeType == LicencePositionStagedPayloadChangeType.ADD_POSITION
        ? CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(STAGED_POSITION_ID.toString())
            .withCorrectionReference(ADDED_POSITION_STAGED_PAYLOAD_REFERENCE)
            .build()
        : UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var positionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(POSITION_STAGED_PAYLOAD_ID)
        .withLicenceCorrection(correction)
        .withChangeType(changeType)
        .withTargetLicencePosition(POSITION)
        .withPayload(payload)
        .build();
    when(licencePositionStagedPayloadService.findById(POSITION_STAGED_PAYLOAD_ID))
        .thenReturn(Optional.of(positionStagedPayload));
    return positionStagedPayload;
  }
}

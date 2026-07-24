package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class LicencePositionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionRepository licencePositionRepository;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionChangeViewService licencePositionChangeViewService;

  @Mock
  private LicencePositionStateViewService licencePositionStateViewService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @InjectMocks
  private LicencePositionService licencePositionService;

  @Captor
  private ArgumentCaptor<LicencePosition> licencePositionArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<ChronologicalPosition>> chronologicalPositionsCaptor;

  @ParameterizedTest
  @MethodSource("provideMaxPositionOrderCombinations")
  void createLicencePosition_noExistingPositionOnDate(Integer maxPositionOrder, int positionOrder) {
    var transaction = LicenceTransactionTestUtil.newBuilder().build();
    var date  = LocalDate.of(2026, Month.JANUARY, 1);

    when(licencePositionRepository.findMaxPositionDateOrder(LICENCE, date)).thenReturn(maxPositionOrder);

    var expectedLicencePosition = LicencePositionTestUtil.newBuilder()
        .withId(null)
        .withLicence(LICENCE)
        .withLicenceTransaction(transaction)
        .withPositionDate(date)
        .withPositionOrder(positionOrder)
        .build();

    licencePositionService.createLicencePosition(LICENCE, transaction, date);

    verify(licencePositionRepository).save(licencePositionArgumentCaptor.capture());

    assertThat(licencePositionArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedLicencePosition);
  }

  private static Stream<Arguments> provideMaxPositionOrderCombinations() {
    return Stream.of(
        Arguments.of(null, 1),
        Arguments.of(1, 2),
        Arguments.of(2, 3)
    );
  }

  @Test
  void getPositionForLicence() {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build();
    when(licencePositionRepository.findByIdAndLicence(POSITION_ID, LICENCE))
        .thenReturn(Optional.of(position));

    assertThat(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).isEqualTo(position);
  }

  @Test
  void getPositionForLicence_whenNotFound_throws() {
    when(licencePositionRepository.findByIdAndLicence(POSITION_ID, LICENCE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> licencePositionService.getPositionForLicence(LICENCE, POSITION_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getCurrentAdministratorId_ForCorrection_resolvesFromCorrectedChronologicalPositions() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var position = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true).build();
    var chronological = List.of(position);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionStateViewService.resolveCurrentAdministratorId(POSITION_ID, List.of(ChronologicalPosition.fromLicencePosition(position, List.of()))))
        .thenReturn(123);

    assertThat(licencePositionService.getCurrentAdministratorIdForCorrection(correction, position.getId())).isEqualTo(123);

    // the administrator is resolved from the corrected chronological positions (correction changes folded in)
    verify(licencePositionStateViewService)
        .resolveCurrentAdministratorId(eq(POSITION_ID), chronologicalPositionsCaptor.capture());
    assertThat(chronologicalPositionsCaptor.getValue())
        .extracting(ChronologicalPosition::id)
        .containsExactly(POSITION_ID);
  }

  @Test
  void getPositionPageView() {
    var older = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var newer = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-2").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(false).build();
    var chronological = List.of(older, newer);

    var changes = List.of(
        LicencePositionChangeTestUtil.newBuilder().withLicencePosition(older).build()
    );

    var changeViews = Map.<String, LicencePositionChangeView>of();

    var stateView = new LicencePositionStateView(null);

    var liveChronologicalPositions = List.of(
        ChronologicalPosition.fromLicencePosition(older, PositionChange.fromLicencePositionChanges(changes)));

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(List.of(older))).thenReturn(changes);
    when(licencePositionChangeViewService.getChangeViews(newer.getId(), liveChronologicalPositions)).thenReturn(changeViews);
    when(licencePositionStateViewService.getStateView(newer.getId(), liveChronologicalPositions)).thenReturn(stateView);

    var result = licencePositionService.getPositionPageView(newer);

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("REF-1");
    assertThat(result.changeViewByType()).isEqualTo(changeViews);
    assertThat(result.stateView()).isEqualTo(stateView);
  }

  @Test
  void getCorrectionPositionPageView_buildsEditUrlsAndIncludesAddedPositions() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var chronological = List.of(executed);

    List<LicencePositionChange> changes = List.of();
    Map<String, LicencePositionChangeView> changeViews = Map.of();
    var stateView = new LicencePositionStateView(null);

    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
        .withCorrectionReference("ADD-REF")
        .build();
    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(addedPayload)
        .build();

    var allChronologicalPositions = List.of(
        ChronologicalPosition.fromLicencePosition(executed, List.of()),
        ChronologicalPosition.fromPayload(addedPayload));

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionChangeViewService.getChangeViews(executed.getId(), allChronologicalPositions)).thenReturn(changeViews);
    when(licencePositionStateViewService.getStateView(executed.getId(), allChronologicalPositions)).thenReturn(stateView);
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));

    var result = licencePositionService.getCorrectionPositionPageView(correction, executed);

    assertThat(result.actions().addAdministratorChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(correction.getId(), executed.getId(), null)));
    assertThat(result.canEdit()).isTrue();
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference, LicencePositionTimelineView::addedInThisCorrection)
        .containsExactly(
            tuple("ADD-REF", true),
            tuple("REF-1", false)
        );
  }

  @Test
  void getCorrectionAddedPositionPageView() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withCorrectionReference("ADD-REF")
        .build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(payload)
        .build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var chronological = List.of(executed);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction))
        .thenReturn(List.of(positionCorrection));

    var allChronologicalPositions = List.of(
        ChronologicalPosition.fromLicencePosition(executed, List.of()),
        ChronologicalPosition.fromPayload(payload));

    when(licencePositionChangeViewService.getChangeViews(UUID.fromString(payload.licencePositionId()), allChronologicalPositions))
        .thenReturn(Map.of());

    var result = licencePositionService.getCorrectionAddedPositionPageView(correction, positionCorrection);

    assertThat(result.actions().addAdministratorChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(correction.getId(), positionCorrection.getId(), null)));
    assertThat(result.changeViewByType()).isEmpty();
    assertThat(result.stateView()).isNull();
    assertThat(result.canEdit()).isTrue();
    assertThat(result.selectedPositionId()).isEqualTo(UUID.fromString(payload.licencePositionId()));
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-REF", "REF-1");
  }

  @Test
  void getCorrectionTimeline_ordersLiveAndAddedPositionsByDateThenOrderDescending() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var liveJan = executedPosition("LIVE-JAN", LocalDate.of(2026, Month.JANUARY, 1), 1);
    var liveJun1 = executedPosition("LIVE-JUN-1", LocalDate.of(2026, Month.JUNE, 1), 1);
    var liveJun2 = executedPosition("LIVE-JUN-2", LocalDate.of(2026, Month.JUNE, 1), 2);

    var addedMar = addedCorrection(correction, "ADD-MAR", LocalDate.of(2026, Month.MARCH, 1), 1);
    var addedJun3 = addedCorrection(correction, "ADD-JUN-3", LocalDate.of(2026, Month.JUNE, 1), 3);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(List.of(liveJan, liveJun1, liveJun2));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(liveJan, liveJun1, liveJun2))).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction))
        .thenReturn(List.of(addedMar, addedJun3));

    var result = licencePositionService.getCorrectionAddedPositionPageView(correction, addedMar);

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-JUN-3", "LIVE-JUN-2", "LIVE-JUN-1", "ADD-MAR", "LIVE-JAN");
  }

  private LicencePosition executedPosition(String regulatorReference, LocalDate positionDate, int positionOrder) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference(regulatorReference).build())
        .withPositionDate(positionDate)
        .withPositionOrder(positionOrder)
        .withIsExecuted(true)
        .build();
  }

  private LicencePositionCorrection addedCorrection(
      LicenceCorrection correction,
      String correctionReference,
      LocalDate effectiveDate,
      int effectiveDateOrder
  ) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(effectiveDate)
            .withEffectiveDateOrder(effectiveDateOrder)
            .withCorrectionReference(correctionReference)
            .build())
        .build();
  }

  @Test
  void getExecutedChronologicalLicencePositions_returnsOnlyExecutedInChronologicalOrder() {
    var licence = LicenceTestUtil.builder().build();

    var earlierExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1))
        .withIsExecuted(true)
        .build();
    var nonExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.MARCH, 1))
        .withIsExecuted(false)
        .build();

    when(licencePositionRepository.findByLicence(licence))
        .thenReturn(List.of(nonExecuted, earlierExecuted));

    var result = licencePositionService.getExecutedChronologicalLicencePositions(licence);

    assertThat(result).containsExactly(earlierExecuted);
  }

  @Test
  void getCorrectionPositionPageView_marksRemovedPositionsAndExcludesThemFromStateRecalculation() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(correctionId)
        .withLicence(LICENCE)
        .build();

    var current = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("CURRENT").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var removedOther = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REMOVED").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var chronological = List.of(current, removedOther);

    var stateView = new LicencePositionStateView(null);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removedOther, current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getRemovedLicencePositionIds(correction)).thenReturn(Set.of(removedOther.getId()));

    var allChronologicalPositions = List.of(ChronologicalPosition.fromLicencePosition(current, List.of()));

    when(licencePositionChangeViewService.getChangeViews(current.getId(), allChronologicalPositions)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(current.getId(), allChronologicalPositions)).thenReturn(stateView);

    var result = licencePositionService.getCorrectionPositionPageView(correction, current);

    // the position removed in this correction is dropped from the recalculation, leaving only the current position
    verify(licencePositionStateViewService).getStateView(current.getId(), allChronologicalPositions);

    assertThat(result.stateView()).isEqualTo(stateView);
    // the timeline still lists the removed position, marked as removed and without a remove link
    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::removedInThisCorrection,
            timelineView -> timelineView.removeUrl() != null)
        .containsExactly(
            tuple("CURRENT", false, true),
            tuple("REMOVED", true, false)
        );
  }

  @Test
  void getCorrectionPositionPageView_whenViewedPositionRemovedInThisCorrection_retainsItAndOmitsRemoveUrl() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var removed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REMOVED").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var chronological = List.of(removed);

    var stateView = new LicencePositionStateView(null);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getRemovedLicencePositionIds(correction)).thenReturn(Set.of(POSITION_ID));

    var allChronologicalPositions = List.of(ChronologicalPosition.fromLicencePosition(removed, List.of()));

    when(licencePositionChangeViewService.getChangeViews(removed.getId(), allChronologicalPositions)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(removed.getId(), allChronologicalPositions)).thenReturn(stateView);

    var result = licencePositionService.getCorrectionPositionPageView(correction, removed);

    // the viewed position is removed, but it is retained in the recalculation so its own page still renders
    verify(licencePositionStateViewService).getStateView(removed.getId(), allChronologicalPositions);

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::removedInThisCorrection,
            timelineView -> timelineView.removeUrl() != null)
        .containsExactly(
            tuple("REMOVED", true, false)
        );
  }

  @Test
  void getCorrectionPositionPageView_whenPositionRemovedInThisCorrection_omitsRemoveUrlAndSetsReinstateUrl() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(correctionId)
        .withLicence(LICENCE)
        .build();

    var removed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REMOVED").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var chronological = List.of(removed);

    var stateView = new LicencePositionStateView(null);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getRemovedLicencePositionIds(correction)).thenReturn(Set.of(POSITION_ID));

    var allChronologicalPositions = List.of(ChronologicalPosition.fromLicencePosition(removed, List.of()));

    when(licencePositionChangeViewService.getChangeViews(removed.getId(), allChronologicalPositions)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(removed.getId(), allChronologicalPositions)).thenReturn(stateView);

    var result = licencePositionService.getCorrectionPositionPageView(correction, removed);

    var expectedReinstateUrl = String.format("/licence-corrections/%s/positions/%s/reinstate",
        correctionId, POSITION_ID);
    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::removedInThisCorrection,
            LicencePositionTimelineView::removeUrl,
            LicencePositionTimelineView::reinstateUrl)
        .containsExactly(
            tuple("REMOVED", true, null, expectedReinstateUrl)
        );
  }

  @Test
  void getCorrectionPositionPageView_whenPositionDateCorrected_usesCorrectedDateAndSetsCorrectDateUrl() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(correctionId)
        .withLicence(LICENCE)
        .build();

    var current = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("CURRENT").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var chronological = List.of(current);

    var stateView = new LicencePositionStateView(null);

    var correctedPayload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.AUGUST, 15))
        .withEffectiveDateOrder(3)
        .withCorrectionReference("CORR-REF")
        .build();

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(List.of());
    when(licencePositionCorrectionService.getUpdatedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    var allChronologicalPositions = List.of(ChronologicalPosition.fromLicencePosition(current, List.of()));

    when(licencePositionChangeViewService.getChangeViews(current.getId(), allChronologicalPositions)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(current.getId(), allChronologicalPositions)).thenReturn(stateView);
    when(licencePositionCorrectionService.getUpdatedPositionPayloadsByTargetId(correction))
        .thenReturn(Map.of(POSITION_ID, correctedPayload));

    var result = licencePositionService.getCorrectionPositionPageView(correction, current);

    var expectedCorrectDateUrl = String.format(
        "/licence-corrections/%s/positions/%s/correct-position-date", correctionId, POSITION_ID);

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::formattedPositionDate,
            LicencePositionTimelineView::correctedInThisCorrection,
            LicencePositionTimelineView::correctDateUrl)
        .containsExactly(
            tuple("CORR-REF", "15 August 2026", true, expectedCorrectDateUrl));
  }
}
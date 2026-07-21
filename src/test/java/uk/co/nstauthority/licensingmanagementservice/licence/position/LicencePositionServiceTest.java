package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
  void getCurrentAdministratorId() {
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var chronological = List.of(position);
    List<LicencePositionChange> changes = List.of();

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionStateViewService.resolveCurrentAdministratorId(position, chronological, changes))
        .thenReturn(123);

    assertThat(licencePositionService.getCurrentAdministratorId(position)).isEqualTo(123);
  }

  @Test
  void getChronologicalLicencePositions() {
    var olderPosition1 = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();
    var olderPosition2 = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(2).build();
    var newestPosition = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).build();


    when(licencePositionRepository.findByLicence(LICENCE))
        .thenReturn(List.of(newestPosition, olderPosition2, olderPosition1));

    assertThat(licencePositionService.getChronologicalLicencePositions(LICENCE))
        .containsExactly(olderPosition1, olderPosition2, newestPosition);
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
        LicencePositionChangeTestUtil.newBuilder().withLicencePosition(newer).build()
    );

    var changeViews = Map.<String, LicencePositionChangeView>of();

    var stateView = new LicencePositionStateView(null);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionChangeViewService.getChangeViews(newer, chronological, changes)).thenReturn(changeViews);
    when(licencePositionStateViewService.getStateView(newer, chronological, changes)).thenReturn(stateView);

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

    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
            .withCorrectionReference("ADD-REF")
            .build())
        .build();

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionChangeViewService.getChangeViews(executed, chronological, changes)).thenReturn(changeViews);
    when(licencePositionStateViewService.getStateView(executed, chronological, changes)).thenReturn(stateView);
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
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction))
        .thenReturn(List.of(positionCorrection));

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
    var chronological = List.of(current);

    List<LicencePositionChange> changes = List.of();
    var stateView = new LicencePositionStateView(null);
    var stateCalculationPositions = List.of(current);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionChangeViewService.getChangeViews(current, stateCalculationPositions, changes)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(current, stateCalculationPositions, changes)).thenReturn(stateView);
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionService.getCorrectionPositionPageView(correction, current);

    var expectedRemoveUrl = String.format("/licence-corrections/%s/positions/%s/remove",
        correctionId, POSITION_ID);
    assertThat(result.stateView()).isEqualTo(stateView);
    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::removedInThisCorrection,
            LicencePositionTimelineView::removeUrl)
        .containsExactly(
            tuple("CURRENT", false, expectedRemoveUrl)
        );
  }

  @Test
  void getCorrectionPositionPageView_whenPositionRemovedInThisCorrection_omitsRemoveUrl() {
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

    List<LicencePositionChange> changes = List.of();
    var stateView = new LicencePositionStateView(null);

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionChangeViewService.getChangeViews(removed, chronological, changes)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(removed, chronological, changes)).thenReturn(stateView);
    when(licencePositionCorrectionService.getRemovedLicencePositionIds(correction)).thenReturn(Set.of(POSITION_ID));
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

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

    List<LicencePositionChange> changes = List.of();
    var stateView = new LicencePositionStateView(null);

    var correctedPayload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.AUGUST, 15))
        .withEffectiveDateOrder(3)
        .withCorrectionReference("CORR-REF")
        .build();

    when(licencePositionRepository.findByLicence(LICENCE)).thenReturn(chronological);
    when(licencePositionChangeService.findByLicencePositionIn(chronological)).thenReturn(changes);
    when(licencePositionChangeViewService.getChangeViews(current, chronological, changes)).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(current, chronological, changes)).thenReturn(stateView);
    when(licencePositionCorrectionService.getUpdatedPositionPayloadsByTargetId(correction))
        .thenReturn(Map.of(POSITION_ID, correctedPayload));
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

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
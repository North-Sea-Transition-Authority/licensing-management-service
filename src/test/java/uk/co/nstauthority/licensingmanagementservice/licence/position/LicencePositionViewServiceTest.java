package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
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
class LicencePositionViewServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionChangeViewService licencePositionChangeViewService;

  @Mock
  private LicencePositionStateViewService licencePositionStateViewService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private LicencePositionViewService licencePositionViewService;

  @Test
  void getAdministratorChangeContext() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var position = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionViewService.getAdministratorChangeContext(correction, POSITION_ID);

    assertThat(result.currentAdministratorId()).isNull();
    assertThat(result.previousAdministratorId()).isNull();
    assertThat(result.currentAdministratorName()).isEmpty();
    assertThat(result.previousAdministratorName()).isEmpty();
    verify(licencePositionService).getExecutedChronologicalLicencePositions(LICENCE);
  }

  @Test
  void getAdministratorChangeContext_resolvesAdministratorNames() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var position = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(position).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1)))
        .thenReturn(Map.of(1, "Current Admin Org"));

    var result = licencePositionViewService.getAdministratorChangeContext(correction, POSITION_ID);

    assertThat(result.currentAdministratorId()).isEqualTo(1);
    assertThat(result.previousAdministratorId()).isNull();
    assertThat(result.currentAdministratorName()).isEqualTo("Current Admin Org");
    assertThat(result.previousAdministratorName()).isEmpty();
  }

  @Test
  void getPositionPageView_batchesAdministratorNameLookupIntoSingleRequest() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(position).build();
    var chronologicalPositions = List.of(ChronologicalPosition.fromLicencePosition(
        position,
        PositionChange.fromLicencePositionChanges(List.of(change))
    ));
    var administratorNames = Map.<Integer, String>of();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1))).thenReturn(administratorNames);
    when(licencePositionChangeViewService.getChangeViews(
        position.getId(),
        chronologicalPositions,
        administratorNames
    )).thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(position.getId(), chronologicalPositions, administratorNames))
        .thenReturn(new LicencePositionStateView(null));

    licencePositionViewService.getPositionPageView(position);

    verify(organisationUnitQueryService).getOrganisationUnitNamesByIds(List.of(1));
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

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(older).build();
    var chronologicalPositions = List.of(ChronologicalPosition.fromLicencePosition(
        older,
        PositionChange.fromLicencePositionChanges(List.of(change))
    ));
    var administratorNames = Map.<Integer, String>of();

    var changeViews = Map.<String, LicencePositionChangeView>of();
    var stateView = new LicencePositionStateView(null);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(older));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(older))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1))).thenReturn(administratorNames);
    when(licencePositionChangeViewService.getChangeViews(newer.getId(), chronologicalPositions, administratorNames))
        .thenReturn(changeViews);
    when(licencePositionStateViewService.getStateView(newer.getId(), chronologicalPositions, administratorNames))
        .thenReturn(stateView);

    var result = licencePositionViewService.getPositionPageView(newer);

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

    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
        .withCorrectionReference("ADD-REF")
        .build();
    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(addedPayload)
        .build();

    var chronologicalPositions = List.of(
        noChangeChronologicalPosition(executed),
        ChronologicalPosition.fromPayload(addedPayload)
    );
    var administratorNames = Map.<Integer, String>of();

    var changeViews = Map.<String, LicencePositionChangeView>of();
    var stateView = new LicencePositionStateView(null);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));
    when(licencePositionChangeViewService.getChangeViews(executed.getId(), chronologicalPositions, administratorNames))
        .thenReturn(changeViews);
    when(licencePositionStateViewService.getStateView(executed.getId(), chronologicalPositions, administratorNames))
        .thenReturn(stateView);

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    assertThat(result.actions().administratorChangeUrl())
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
    var addedPositionId = UUID.fromString(payload.licencePositionId());

    var executed = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var chronologicalPositions = List.of(
        noChangeChronologicalPosition(executed),
        ChronologicalPosition.fromPayload(payload)
    );
    var administratorNames = Map.<Integer, String>of();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(positionCorrection));
    when(licencePositionChangeViewService.getChangeViews(addedPositionId, chronologicalPositions, administratorNames))
        .thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(addedPositionId, chronologicalPositions, administratorNames))
        .thenReturn(null);

    var result = licencePositionViewService.getCorrectionAddedPositionPageView(correction, positionCorrection);

    assertThat(result.actions().administratorChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(correction.getId(), positionCorrection.getId(), null)));
    assertThat(result.changeViewByType()).isEmpty();
    assertThat(result.stateView()).isNull();
    assertThat(result.canEdit()).isTrue();
    assertThat(result.selectedPositionId()).isEqualTo(addedPositionId);
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-REF", "REF-1");
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
    var removed = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REMOVED").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var recalculatedPositions = List.of(noChangeChronologicalPosition(current));
    var administratorNames = Map.<Integer, String>of();

    var stateView = new LicencePositionStateView(null);

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(removed, current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed, current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(licencePositionChangeViewService.getChangeViews(current.getId(), recalculatedPositions, administratorNames))
        .thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(current.getId(), recalculatedPositions, administratorNames))
        .thenReturn(stateView);

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, current);

    assertThat(result.stateView()).isEqualTo(stateView);
    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::removedInThisCorrection,
            timelineView -> timelineView.removeUrl() != null
        )
        .containsExactly(
            tuple("CURRENT", false, true),
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

    var recalculatedPositions = List.of(noChangeChronologicalPosition(removed));
    var administratorNames = Map.<Integer, String>of();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(licencePositionChangeViewService.getChangeViews(removed.getId(), recalculatedPositions, administratorNames))
        .thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(removed.getId(), recalculatedPositions, administratorNames))
        .thenReturn(new LicencePositionStateView(null));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, removed);

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

    var correctedPayload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.AUGUST, 15))
        .withEffectiveDateOrder(3)
        .withCorrectionReference("CORR-REF")
        .build();
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(current)
        .withPayload(correctedPayload)
        .build();

    var recalculatedPositions = List.of(noChangeChronologicalPosition(current));
    var administratorNames = Map.<Integer, String>of();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));
    when(licencePositionChangeViewService.getChangeViews(current.getId(), recalculatedPositions, administratorNames))
        .thenReturn(Map.of());
    when(licencePositionStateViewService.getStateView(current.getId(), recalculatedPositions, administratorNames))
        .thenReturn(new LicencePositionStateView(null));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, current);

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

  @Test
  void getCorrectionTimeline_ordersLiveAndAddedPositionsByDateThenOrderDescending() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var liveJan = executedPosition("LIVE-JAN", LocalDate.of(2026, Month.JANUARY, 1), 1);
    var liveJun1 = executedPosition("LIVE-JUN-1", LocalDate.of(2026, Month.JUNE, 1), 1);
    var liveJun2 = executedPosition("LIVE-JUN-2", LocalDate.of(2026, Month.JUNE, 1), 2);

    var addedMar = addedCorrection(correction, "ADD-MAR", LocalDate.of(2026, Month.MARCH, 1), 1);
    var addedJun3 = addedCorrection(correction, "ADD-JUN-3", LocalDate.of(2026, Month.JUNE, 1), 3);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(liveJan, liveJun1, liveJun2));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(liveJan, liveJun1, liveJun2))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedMar, addedJun3));

    var result = licencePositionViewService.getCorrectionAddedPositionPageView(correction, addedMar);

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-JUN-3", "LIVE-JUN-2", "LIVE-JUN-1", "ADD-MAR", "LIVE-JAN");
  }

  private static ChronologicalPosition noChangeChronologicalPosition(LicencePosition position) {
    return ChronologicalPosition.fromLicencePosition(position, List.of());
  }

  private static LicencePosition executedPosition(String regulatorReference, LocalDate positionDate, int positionOrder) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference(regulatorReference).build())
        .withPositionDate(positionDate)
        .withPositionOrder(positionOrder)
        .withIsExecuted(true)
        .build();
  }

  private static LicencePositionCorrection addedCorrection(
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
}

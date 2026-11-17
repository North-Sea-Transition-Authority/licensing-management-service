package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.RemovePartialSurrenderChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTimelineView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BlockStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.SubareaStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

@ExtendWith(MockitoExtension.class)
class LicencePositionCorrectionViewServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Feature SURRENDERED_BLOCK = FeatureTestUtil.builder().withFeatureName("30/1a").build();
  private static final AdministratorOperation ADMINISTRATOR_OPERATION = LicenceOperation.newAdministratorChange()
      .withOperator(1)
      .build();

  @Mock
  private LicenceStatusService licenceStatusService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FeatureService featureService;

  @Mock
  private LicencePositionValidationService licencePositionValidationService;

  private LicencePositionCorrectionViewService licencePositionCorrectionViewService;

  @Captor
  private ArgumentCaptor<List<ChronologicalPosition>> validationPositionsCaptor;

  @Captor
  private ArgumentCaptor<ResolvedStates> validationStatesCaptor;

  @BeforeEach
  void setUp() {
    var chronologicalPositionService = new ChronologicalPositionService(
        licencePositionService,
        licencePositionChangeService,
        licencePositionCorrectionService
    );
    var licencePositionNameService = new LicencePositionNameService(organisationUnitQueryService, featureService);

    licencePositionCorrectionViewService = new LicencePositionCorrectionViewService(
        licencePositionService,
        licencePositionCorrectionService,
        new LicencePositionCorrectionFilterService(
            licencePositionCorrectionService,
            chronologicalPositionService,
            licencePositionNameService
        ),
        chronologicalPositionService,
        licencePositionNameService,
        licencePositionValidationService,
        licenceStatusService
    );
  }

  @Test
  void getCorrectionPositionPageView_whenPartialSurrenderIsUntouched_linksCorrectToTheTaskListForThatChange() {
    var correctionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();
    var executed = executedPosition();

    var liveSurrenderChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(executed)
        .withOperations(List.of(partialSurrenderOperation()))
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(liveSurrenderChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(featureService.getFeaturesByIds(any())).thenReturn(List.of(SURRENDERED_BLOCK));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    var surrenderChange =
        (PartialSurrenderChangeView) changeViewOfType(result, LicenceOperation.PARTIAL_SURRENDER);
    assertThat(surrenderChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderForCorrectingChange(correction, executed, liveSurrenderChange, null)));
  }

  @Test
  void getCorrectionPositionPageView_whenPartialSurrenderStagedOnThisCorrection_linksCorrectToTheStagedTaskList() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();
    var executed = executedPosition();

    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(partialSurrenderOperation()), 1)))
            .build())
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));
    when(featureService.getFeaturesByIds(any())).thenReturn(List.of(SURRENDERED_BLOCK));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    var surrenderChange =
        (PartialSurrenderChangeView) changeViewOfType(result, LicenceOperation.PARTIAL_SURRENDER);
    assertThat(surrenderChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(correction, updateCorrection, null)));
  }

  @Test
  void getCorrectionPositionPageView_whenPositionDateIsCorrected_splitsNameHistoryOnTheCorrectedDate() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var dateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LocalDate.of(1990, Month.JANUARY, 1))
            .build())
        .build();

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(executed).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(change));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(dateCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd",
            namePeriod("Admin Ltd", LocalDate.of(2000, Month.JANUARY, 1), null))));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    assertThat(result.stateView().organisationNameHistories())
        .isEqualTo(List.of(new OrganisationNameHistoryView(
            "Admin Ltd",
            List.of(),
            List.of(new NameHistoryEntryView("Admin Ltd", "from 1 January 2000"))
        )));
  }

  @Test
  void getCorrectionPositionPageView_licenseeNamesAreEmpty() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(executed).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd",
            namePeriod("Admin Ltd", LocalDate.of(2000, Month.JANUARY, 1), null))));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    assertThat(result.stateView().licenseeStateView().licensees()).isEqualTo(List.of());
  }

  @Test
  void getCorrectionPositionPageView_stateHasLicenseeNames() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();
    var operation = LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(1, 2)).build();

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withOperations(List.of(operation))
        .withLicencePosition(executed).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd",
            namePeriod("Admin Ltd", LocalDate.of(2000, Month.JANUARY, 1), null)),
            2, namePeriods("Org Ltd",
                namePeriod("Org Ltd", LocalDate.of(2000, Month.JANUARY, 2), null))));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    assertThat(result.stateView().licenseeStateView().licensees()).isEqualTo(List.of(
        new LicenseeView("Admin Ltd", OrganisationMarker.NO_BENEFICIAL_INTEREST),
        new LicenseeView("Org Ltd", OrganisationMarker.NO_BENEFICIAL_INTEREST)));
  }

  @Test
  void getCorrectionPositionPageView_whenPositionDateIsCorrected_namesOrganisationsAsTheyWereOnTheCorrectedDate() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var dateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LocalDate.of(1990, Month.JANUARY, 1))
            .build())
        .build();

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(executed).build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(change));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(dateCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Shell plc",
            namePeriod("Shell UK Exploration", LocalDate.of(1987, Month.JANUARY, 12), LocalDate.of(1999, Month.MARCH, 3)),
            namePeriod("Shell plc", LocalDate.of(1999, Month.MARCH, 3), null))
        ));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Shell UK Exploration"),
            new LicenseeStateView(List.of()),
            "",
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Shell UK Exploration",
                List.of(),
                List.of(new NameHistoryEntryView("Shell plc", "from 3 March 1999"))
            )),
            List.of()
        ));
  }

  private static LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();
  }

  @Test
  void getCorrectionPositionPageView_whenPartialSurrenderRemoved_rendersTheLiveSurrenderWithAnUndoUrl() {
    var correctionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();
    var executed = executedPosition();

    var liveSurrenderChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(executed)
        .withOperations(List.of(partialSurrenderOperation()))
        .build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(executed.getPositionDate())
            .withEffectiveDateOrder(executed.getPositionDateOrder())
            .withChanges(List.of(LicencePositionChangeType.removeChange().withChangeId(changeId.toString()).build()))
            .build())
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(liveSurrenderChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(featureService.getFeaturesByIds(Set.of(SURRENDERED_BLOCK.getId()))).thenReturn(List.of(SURRENDERED_BLOCK));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    var expected = new PartialSurrenderChangeView(
        "1 January 2026",
        List.of(new PartialSurrenderChangeView.BlockRow("Block 30/1a (0.00 km²)", List.of("Full surrender"))),
        null,
        LicencePositionChangeType.REMOVE_CHANGE,
        new ChangeViewUrls(
            null,
            null,
            null,
            ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
                .renderUndoPartialSurrender(correction, changeId.toString())),
            null,
            null,
            null));
    assertThat(changeViewOfType(result, LicenceOperation.PARTIAL_SURRENDER)).isEqualTo(expected);
  }

  private static PartialSurrenderOperation partialSurrenderOperation() {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(SURRENDERED_BLOCK.getId()))
        .withSurrenderDetails(Map.of(SURRENDERED_BLOCK.getId(),
            new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.FULL_SURRENDER, UUID.randomUUID(), List.of(SURRENDERED_BLOCK.getId()))))
        .build();
  }

  private static LicencePosition executedPosition(String reference, LocalDate positionDate) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference(reference).build())
        .withPositionDate(positionDate)
        .withPositionOrder(1)
        .withStatus(LicencePositionStatus.EXECUTED)
        .build();
  }

  private static LicencePositionChange changeOn(LicencePosition position, int changeOrder, LicenceOperation operation) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withChangeOrder(changeOrder)
        .withOperations(List.of(operation))
        .build();
  }

  private static LicenceOperation setEquityOperation(int transferTo) {
    return LicenceOperation.newSetEquityOperation().withTransferTo(transferTo).withEquity(new BigDecimal("100")).build();
  }

  private static LicenceTimelineFilter changeTypeFilter(String changeType) {
    return new LicenceTimelineFilter(Set.of(changeType), Set.of());
  }

  private static LicenceTimelineFilter organisationFilter(Integer organisationId) {
    return new LicenceTimelineFilter(Set.of(), Set.of(organisationId));
  }

  @Test
  void getCorrectionPositionPageView_buildsEditUrlsAndIncludesAddedPositions() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
        .withCorrectionReference("ADD-REF")
        .build();
    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(addedPayload)
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    assertThat(result.actions().addChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(correction, executed)));
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
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(positionCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionAddedPositionPageView(correction, positionCorrection, LicenceTimelineFilter.empty());

    assertThat(result.actions().addChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(correction, positionCorrection)));
    assertThat(result.orderedChangeViews()).isEmpty();
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(
        new AdministratorStateView(""),
        new LicenseeStateView(List.of()),
        "",
        List.of(),
        List.of(),
        List.of()));
    assertThat(result.canEdit()).isTrue();
    assertThat(result.selectedPositionId()).isEqualTo(addedPositionId);
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-REF", "REF-1");
  }

  @Test
  void getCorrectionPositionPageView_whenPendingAddChangeOnExecutedPosition_setsExecutedPositionCorrectUrl() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1)))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of());

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(correction, executed)));
    // The page-level "Add change" action stays available even when an administrator change is present.
    assertThat(result.actions().addChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(correction, executed)));
  }

  @Test
  void getCorrectionPositionPageView_whenCorrectingCommittedChange_setsCorrectExistingUrl() {
    var correctionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    // A committed change not yet corrected in this correction (change type is null) must still route to the
    // correct-existing endpoint so the search selector pre-populates with the current administrator.
    var committedChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId).withLicencePosition(executed).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(committedChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of());

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForCorrectingChange(correction, executed, committedChange)));
  }

  @Test
  void getCorrectionPositionPageView_whenAdminChangeRemoved_rendersExecutedAdminWithRemoveTypeAndRevertsState() {
    var correctionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId).withLicencePosition(executed)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(LicencePositionChangeType.removeChange().withChangeId(changeId.toString()).build()))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of(5, namePeriods(
        "Executed Admin Org", namePeriod("Executed Admin Org", LocalDate.of(1990, Month.JANUARY, 1), null))));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, LicenceTimelineFilter.empty());

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.joiningOrganisationName()).isEqualTo("Executed Admin Org");
    assertThat(adminChange.changeType()).isEqualTo(LicencePositionChangeType.REMOVE_CHANGE);
    assertThat(adminChange.urls().remove()).isNull();
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(
        new AdministratorStateView(""),
        new LicenseeStateView(List.of()),
        "",
        List.of(),
        List.of(),
        List.of()));
  }

  @Test
  void getCorrectionPositionPageView_whenSubareaStagedOnAnEarlierPosition_thenLaterPositionShowsItOnTheBlock() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();
    var earlierPosition = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();
    var laterPosition = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.FEBRUARY, 1)).withPositionOrder(2).build();

    var block = FeatureTestUtil.blockFeature(UUID.randomUUID(), "21", 1);
    var liveSubareaFeature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "A");
    var stagedSubareaFeature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "B");
    var liveSubarea = new SubareaDetails(liveSubareaFeature.getId(), "Alpha", "A");
    var stagedSubarea = new SubareaDetails(stagedSubareaFeature.getId(), "Bravo", "B");

    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(earlierPosition)
        .withOperations(List.of(
            new BlockCreateOperation(List.of(block.getId()), Map.of(block.getId(), List.of(liveSubarea)))))
        .build();
    var stagedCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(earlierPosition)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(earlierPosition.getPositionDate())
            .withEffectiveDateOrder(earlierPosition.getPositionDateOrder())
            .withChanges(List.of(AddChange.buildOperationsChange(
                List.of(new SubareaCreateOperation(block.getId(), List.of(stagedSubarea))), 2)))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(earlierPosition, laterPosition));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(earlierPosition, laterPosition)))
        .thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(stagedCorrection));
    when(featureService.getFeaturesByIds(Set.of(block.getId(), liveSubareaFeature.getId(), stagedSubareaFeature.getId())))
        .thenReturn(List.of(block, liveSubareaFeature, stagedSubareaFeature));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(
        correction, laterPosition, LicenceTimelineFilter.empty());

    var subareaFeatureIds = List.of(liveSubareaFeature.getId(), stagedSubareaFeature.getId());
    assertThat(result.stateView().blockStateViews())
        .containsExactly(new BlockStateView(
            "Block %s".formatted(block.getFeatureName()),
            "0.00 km²",
            new SummaryMapView(List.of(block.getId()), 4230, block.getId()),
            List.of(
                new SubareaStateView(
                    "Subarea Alpha",
                    "0.00 km²",
                    new SummaryMapView(subareaFeatureIds, 4230, liveSubareaFeature.getId()),
                    List.of()),
                new SubareaStateView(
                    "Subarea Bravo",
                    "0.00 km²",
                    new SummaryMapView(subareaFeatureIds, 4230, stagedSubareaFeature.getId()),
                    List.of()))
        ));
  }

  @Test
  void getCorrectionAddedPositionPageView_whenAdminChangePresent_setsAddedPositionCorrectUrl() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var payload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 5))
        .withCorrectionReference("ADD-REF")
        .withChanges(List.of(AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1)))
        .build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(payload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(positionCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of());

    var result = licencePositionCorrectionViewService.getCorrectionAddedPositionPageView(correction, positionCorrection, LicenceTimelineFilter.empty());

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(correction, positionCorrection)));
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
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();
    var removed = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REMOVED").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(removed, current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed, current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, current, LicenceTimelineFilter.empty());

    // the removed position is excluded from recalculation, and the current position carries no administrator
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(
        new AdministratorStateView(""),
        new LicenseeStateView(List.of()),
        "",
        List.of(),
        List.of(),
        List.of()));
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
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, removed, LicenceTimelineFilter.empty());

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
  void getCorrectionPositionPageView_whenPositionRemovedInThisCorrection_omitsChangeActionUrlsAndAddChangeAction() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();

    var removed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withLicencePosition(removed)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed))).thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of(5, namePeriods(
        "Executed Admin Org", namePeriod("Executed Admin Org", LocalDate.of(1990, Month.JANUARY, 1), null))));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, removed, LicenceTimelineFilter.empty());

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls()).isEqualTo(new ChangeViewUrls(null, null, null, null, null, null, null));
    assertThat(result.actions()).isEqualTo(LicencePositionPageView.Actions.none());
  }

  @Test
  void getCorrectionPositionPageView_whenViewingRemovedPosition_excludedFromValidationAndValidationStates() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var removed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();
    var following = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JULY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var removedAdminChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(removed)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(7).build()))
        .build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE))
        .thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(removed, following));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed, following)))
        .thenReturn(List.of(removedAdminChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(removeCorrection));

    licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, removed, LicenceTimelineFilter.empty());

    verify(licencePositionValidationService).validate(
        validationPositionsCaptor.capture(),
        validationStatesCaptor.capture(),
        eq(List.of(removeCorrection)),
        eq(LICENCE.getType()),
        eq(LicenceStatusType.EXTANT)
    );

    assertThat(validationPositionsCaptor.getValue())
        .extracting(ChronologicalPosition::id)
        .containsExactly(following.getId());

    assertThat(validationStatesCaptor.getValue().currentState(following.getId()).administratorId()).isNull();
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
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, current, LicenceTimelineFilter.empty());

    var expectedCorrectDateUrl = String.format(
        "/licence-corrections/%s/positions/%s/correct-position-date", correctionId, POSITION_ID);

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::formattedPositionDate,
            LicencePositionTimelineView::correctedInThisCorrection,
            LicencePositionTimelineView::correctDateUrl)
        .containsExactly(
            tuple("CORR-REF", "15 August 2026 (3)", true, expectedCorrectDateUrl));
  }

  @Test
  void getCorrectionPositionPageView_whenPayloadHasNoChangesOrDate_doesNotMarkPositionAsCorrected() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var current = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("CURRENT").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var emptyPayload = UpdateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(null)
        .withEffectiveDateOrder(null)
        .withChanges(List.of())
        .build();
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(current)
        .withPayload(emptyPayload)
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, current, LicenceTimelineFilter.empty());

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::formattedPositionDate,
            LicencePositionTimelineView::correctedInThisCorrection)
        .containsExactly(
            tuple("CURRENT", "1 June 2026", false));
  }

  @Test
  void getCorrectionPositionPageView_whenALiveChangeIsMovedAway_thenMarksTheSourcePositionAsCorrected() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();
    var movedChangeId = UUID.randomUUID();

    var source = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();
    var target = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withLicence(LICENCE).withStatus(LicencePositionStatus.EXECUTED)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-2").build())
        .withPositionDate(LocalDate.of(2026, Month.FEBRUARY, 1)).withPositionOrder(1).build();

    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(movedChangeId).withLicencePosition(source).withChangeOrder(1)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .build();

    var moveCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(target)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(null)
            .withEffectiveDateOrder(null)
            .withCorrectionReference("EXEC-2")
            .withChanges(List.of(
                UpdateChangeOperations.buildUpdateChange(movedChangeId.toString(), ADMINISTRATOR_OPERATION)
            ))
            .build())
        .build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(source, target));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(source, target))).thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(moveCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, source, LicenceTimelineFilter.empty());

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::correctedInThisCorrection
        )
        .containsExactlyInAnyOrder(tuple("EXEC-1", true), tuple("EXEC-2", true));
  }

  @Test
  void getCorrectionPositionPageView_whenSameDateOrderCorrected_recalculatesStateInCorrectedOrder() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var sameDate = LocalDate.of(2026, Month.JUNE, 1);
    var moved = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("MOVED").build())
        .withPositionDate(sameDate).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();
    var other = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("OTHER").build())
        .withPositionDate(sameDate).withPositionOrder(2).withStatus(LicencePositionStatus.EXECUTED).build();

    var movedUpdate = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(moved)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(sameDate).withEffectiveDateOrder(2).build())
        .build();
    var otherUpdate = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(other)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(sameDate).withEffectiveDateOrder(1).build())
        .build();

    var stateView = new LicencePositionStateView(
        new AdministratorStateView(""),
        new LicenseeStateView(List.of()),
        "",
        List.of(),
        List.of(),
        List.of());

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(moved, other));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(moved, other))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(movedUpdate, otherUpdate));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, moved, LicenceTimelineFilter.empty());

    assertThat(result.stateView()).isEqualTo(stateView);
  }

  @Test
  void getCorrectionTimeline_ordersLiveAndAddedPositionsByDateThenOrderDescending() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var liveJan = executedPosition("LIVE-JAN", LocalDate.of(2026, Month.JANUARY, 1), 1);
    var liveJun1 = executedPosition("LIVE-JUN-1", LocalDate.of(2026, Month.JUNE, 1), 1);
    var liveJun2 = executedPosition("LIVE-JUN-2", LocalDate.of(2026, Month.JUNE, 1), 2);

    var addedMar = addedCorrection(correction, "ADD-MAR", LocalDate.of(2026, Month.MARCH, 1), 1);
    var addedJun3 = addedCorrection(correction, "ADD-JUN-3", LocalDate.of(2026, Month.JUNE, 1), 3);

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(liveJan, liveJun1, liveJun2));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(liveJan, liveJun1, liveJun2))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedMar, addedJun3));

    var result = licencePositionCorrectionViewService.getCorrectionAddedPositionPageView(correction, addedMar, LicenceTimelineFilter.empty());

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-JUN-3", "LIVE-JUN-2", "LIVE-JUN-1", "ADD-MAR", "LIVE-JAN");
  }

  @Test
  void getCorrectionTimeline_whenMultiplePositionsOnSameDate_includesCorrectOrderUrl() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var pos1Id = UUID.randomUUID();
    var pos2Id = UUID.randomUUID();
    var sameDate = LocalDate.of(2026, Month.JANUARY, 1);

    var pos1 = LicencePositionTestUtil.newBuilder()
        .withId(pos1Id).withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(sameDate).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var pos2 = LicencePositionTestUtil.newBuilder()
        .withId(pos2Id).withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-2").build())
        .withPositionDate(sameDate).withPositionOrder(2).withStatus(LicencePositionStatus.EXECUTED).build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(pos1, pos2));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(pos1, pos2))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, pos1, LicenceTimelineFilter.empty());

    var expectedCorrectOrderUrl1 = ReverseRouter.route(
        on(uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(correction, pos1Id));
    var expectedCorrectOrderUrl2 = ReverseRouter.route(
        on(uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(correction, pos2Id));

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::correctOrderUrl)
        .containsExactlyInAnyOrder(
            tuple("REF-1", expectedCorrectOrderUrl1),
            tuple("REF-2", expectedCorrectOrderUrl2)
        );
  }

  @Test
  void getCorrectionTimeline_whenOnlyOnePositionOnDate_omitsCorrectOrderUrl() {
    var correctionId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var pos1 = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID()).withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(pos1));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(pos1))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, pos1, LicenceTimelineFilter.empty());

    assertThat(result.timelineViews())
        .extracting(uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTimelineView::correctOrderUrl)
        .containsOnlyNulls();
  }

  @Test
  void getCorrectionPositionPageView_whenChangeTypeAddedInTheCorrection_thenThatPositionIsShownWithTheSelectedPosition() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var administrated = executedPosition("ADMIN", LocalDate.of(2026, Month.JANUARY, 1));
    var corrected = executedPosition("CORRECTED", LocalDate.of(2026, Month.JUNE, 1));
    var updateCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(corrected)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(setEquityOperation(1)), 1)))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(administrated, corrected));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(administrated, corrected)))
        .thenReturn(List.of(changeOn(administrated, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd")));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(
        correction,
        administrated,
        changeTypeFilter(LicenceOperation.SET_EQUITY)
    );

    var expectedChangeTypeOptions = new LinkedHashMap<String, String>();
    expectedChangeTypeOptions.put(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change");
    expectedChangeTypeOptions.put(LicenceOperation.SET_EQUITY, "Set equity");
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::positionId)
        .containsExactly(corrected.getId(), administrated.getId());
    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(result.filterOptions())
        .isEqualTo(new LicenceTimelineFilterOptions(expectedChangeTypeOptions, Map.of("1", "Admin Ltd")));
    assertThat(result.filterApplied()).isTrue();
  }

  @Test
  void getCorrectionPositionPageView_whenOrganisationIsOnlyOnAnAddedPosition_thenTheAddedAndSelectedPositionsAreShown() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var executed = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
        .withCorrectionReference("ADD-REF")
        .withChanges(List.of(AddChange.buildOperationsChange(
            List.of(LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(2)).build()),
            1
        )))
        .build();
    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(addedPayload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(changeOn(executed, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, executed, organisationFilter(2));

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference, LicencePositionTimelineView::addedInThisCorrection)
        .containsExactly(tuple("ADD-REF", true), tuple("REF-1", false));
    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.LICENCE_ADMINISTRATOR);
  }

  @Test
  void getCorrectionPositionPageView_whenRemovedPositionHasAMatchingLiveChange_thenTheRemovedPositionIsShown() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var removed = executedPosition("REMOVED", LocalDate.of(2026, Month.JANUARY, 1));
    var current = executedPosition("CURRENT", LocalDate.of(2026, Month.JUNE, 1));
    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed, current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed, current)))
        .thenReturn(List.of(changeOn(removed, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd")));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(correction, current, organisationFilter(1));

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference, LicencePositionTimelineView::removedInThisCorrection)
        .containsExactly(tuple("CURRENT", false), tuple("REMOVED", true));
    assertThat(result.filterOptions().organisationOptions()).isEqualTo(Map.of("1", "Admin Ltd"));
  }

  @Test
  void getCorrectionPositionPageView_whenSameDatePositionIsFilteredOut_thenCorrectOrderUrlIsKept() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var matching = executedPosition("MATCHING", LocalDate.of(2026, Month.JANUARY, 1), 1);
    var other = executedPosition("OTHER", LocalDate.of(2026, Month.JANUARY, 1), 2);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(matching, other));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(matching, other)))
        .thenReturn(List.of(changeOn(matching, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(
        correction,
        matching,
        changeTypeFilter(LicenceOperation.LICENCE_ADMINISTRATOR)
    );

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference, LicencePositionTimelineView::correctOrderUrl)
        .containsExactly(tuple("MATCHING", ReverseRouter.route(on(LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(correction, matching.getId()))));
  }

  @Test
  void getCorrectionPositionPageView_whenFilteredOutPositionIsInvalid_thenItIsShown() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var invalid = executedPosition("INVALID", LocalDate.of(2026, Month.JANUARY, 1));
    var selected = executedPosition("SELECTED", LocalDate.of(2026, Month.JUNE, 1));
    var validationError = new PositionValidationError(invalid.getId(), "1 January 2026", null, null, "Error");

    when(licenceStatusService.getCurrentStatus(LICENCE)).thenReturn(LicenceStatusType.EXTANT);
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(invalid, selected));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(invalid, selected)))
        .thenReturn(List.of(changeOn(selected, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd")));
    when(licencePositionValidationService.validate(
        any(),
        any(),
        eq(List.of()),
        eq(LICENCE.getType()),
        eq(LicenceStatusType.EXTANT)
    )).thenReturn(List.of(validationError));

    var result = licencePositionCorrectionViewService.getCorrectionPositionPageView(
        correction,
        selected,
        changeTypeFilter(LicenceOperation.LICENCE_ADMINISTRATOR)
    );

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::positionId)
        .containsExactly(selected.getId(), invalid.getId());
    assertThat(result.errorSummaryItems())
        .usingRecursiveComparison()
        .isEqualTo(PositionValidationError.toErrorSummaryItems(List.of(validationError)));
  }

  @Test
  void getCorrectionAddedPositionPageView_whenChangeTypeFilter_thenTimelineIsFilteredButChangeViewsAreNot() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var executed = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var addedPayload = CreateLicencePositionPayloadTestUtil.newBuilder()
        .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
        .withCorrectionReference("ADD-REF")
        .withChanges(List.of(
            AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1),
            AddChange.buildOperationsChange(List.of(setEquityOperation(1)), 2)
        ))
        .build();
    var addedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(addedPayload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));

    var result = licencePositionCorrectionViewService.getCorrectionAddedPositionPageView(
        correction,
        addedPositionCorrection,
        changeTypeFilter(LicenceOperation.SET_EQUITY)
    );

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("ADD-REF");
    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.LICENCE_ADMINISTRATOR, LicenceOperation.SET_EQUITY);
    assertThat(result.filterApplied()).isTrue();
  }

  private static LicencePosition executedPosition(String regulatorReference, LocalDate positionDate, int positionOrder) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference(regulatorReference).build())
        .withPositionDate(positionDate)
        .withPositionOrder(positionOrder)
        .withStatus(LicencePositionStatus.EXECUTED)
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

  private static OrganisationNamePeriods namePeriods(String currentName, OrganisationNamePeriod... namePeriods) {
    return new OrganisationNamePeriods(currentName, List.of(namePeriods));
  }

  private static OrganisationNamePeriod namePeriod(String name, LocalDate startDate, LocalDate endDate) {
    return new OrganisationNamePeriod(name, startDate, endDate);
  }

  private static LicencePositionChangeView changeViewOfType(LicencePositionPageView pageView, String type) {
    return pageView.orderedChangeViews().stream()
        .filter(view -> view.type().equals(type))
        .findFirst()
        .orElseThrow();
  }
}

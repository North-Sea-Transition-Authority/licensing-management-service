package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.RemovePartialSurrenderChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class LicencePositionViewServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Feature SURRENDERED_BLOCK = FeatureTestUtil.builder().withFeatureName("30/1a").build();
  private static final AdministratorOperation ADMINISTRATOR_OPERATION = LicenceOperation.newAdministratorChange()
      .withOperator(1)
      .build();

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

  @Spy
  private LicencePositionValidationService licencePositionValidationService = new LicencePositionValidationService();

  @InjectMocks
  private LicencePositionViewService licencePositionViewService;

  @Captor
  private ArgumentCaptor<List<ChronologicalPosition>> validationPositionsCaptor;

  @Captor
  private ArgumentCaptor<ResolvedStates> validationStatesCaptor;

  @Captor
  private ArgumentCaptor<Boolean> isCarbonStorageCaptor;

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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(liveSurrenderChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(featureService.getFeaturesByIds(any())).thenReturn(List.of(SURRENDERED_BLOCK));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    var surrenderChange =
        (PartialSurrenderChangeView) changeViewOfType(result, LicenceOperation.PARTIAL_SURRENDER);
    assertThat(surrenderChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderForCorrectingChange(correctionId, POSITION_ID, changeId.toString(), null, null)));
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));
    when(featureService.getFeaturesByIds(any())).thenReturn(List.of(SURRENDERED_BLOCK));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    var surrenderChange =
        (PartialSurrenderChangeView) changeViewOfType(result, LicenceOperation.PARTIAL_SURRENDER);
    assertThat(surrenderChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(PartialSurrenderTaskListController.class)
            .renderTaskList(correctionId, updateCorrection.getId(), null, null)));
  }

  @Test
  void getPositionPageView_returnsNameHistoryForTheStateOrganisationsSplitOnThePositionDate() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder().withLicencePosition(position).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Future Admin Ltd",
            namePeriod("Former Admin Ltd", LocalDate.of(1990, Month.JANUARY, 1), LocalDate.of(2000, Month.MARCH, 3)),
            namePeriod("Admin Ltd", LocalDate.of(2000, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
            namePeriod("Future Admin Ltd", LocalDate.of(2022, Month.JANUARY, 22), null))
        ));

    var result = licencePositionViewService.getPositionPageView(position);

    assertThat(result.stateView().organisationNameHistories())
        .isEqualTo(List.of(new OrganisationNameHistoryView(
            "Admin Ltd",
            List.of(new NameHistoryEntryView("Former Admin Ltd", "to 3 March 2000")),
            List.of(new NameHistoryEntryView("Future Admin Ltd", "from 22 January 2022"))
        )));
  }

  @Test
  void getCorrectionPositionPageView_whenPositionDateIsCorrected_splitsNameHistoryOnTheCorrectedDate() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
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

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    assertThat(result.stateView().organisationNameHistories())
        .isEqualTo(List.of(new OrganisationNameHistoryView(
            "Admin Ltd",
            List.of(),
            List.of(new NameHistoryEntryView("Admin Ltd", "from 1 January 2000"))
        )));
  }

  @Test
  void getPositionPageView_namesOrganisationsAsTheyWereOnThePositionDate() {
    var changeId = UUID.randomUUID();

    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder().withId(changeId).withLicencePosition(position).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Shell plc",
            namePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
            namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null))
        ));

    var result = licencePositionViewService.getPositionPageView(position);

    assertThat(changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR))
        .isEqualTo(new AdministratorChangeView(
            null,
            "Shell Expro",
            changeId.toString(),
            null,
            new ChangeViewUrls(null, null, null, null)
        ));
    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Shell Expro"),
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Shell Expro",
                List.of(),
                List.of(new NameHistoryEntryView("Shell plc", "from 22 January 2022"))
            ))
        ));
  }

  @Test
  void getPositionPageView_whenBeneficialInterestHoldersHaveBeenRenamed_namesThemAsTheyWereOnThePositionDate() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(
            LicenceOperation.newAdministratorChange().withOperator(1).build(),
            LicenceOperation.newSetEquityOperation().withTransferTo(1).withEquity(new BigDecimal("60")).build(),
            LicenceOperation.newSetEquityOperation().withTransferTo(2).withEquity(new BigDecimal("40")).build()))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2))).thenReturn(Map.of(
        1, namePeriods("Shell plc",
            namePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
            namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null)),
        2, namePeriods("BP plc",
            namePeriod("Britoil", LocalDate.of(1980, Month.JANUARY, 1), LocalDate.of(2000, Month.JANUARY, 1)),
            namePeriod("BP Exploration", LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(2015, Month.JULY, 7)),
            namePeriod("BP plc", LocalDate.of(2015, Month.JULY, 7), null))
    ));

    var result = licencePositionViewService.getPositionPageView(position);

    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Shell Expro"),
            List.of(
                new BeneficialInterestView("BP Exploration", new BigDecimal("40")),
                new BeneficialInterestView("Shell Expro", new BigDecimal("60"))
            ),
            List.of(
                new OrganisationNameHistoryView(
                    "BP Exploration",
                    List.of(new NameHistoryEntryView("Britoil", "to 1 January 2000")),
                    List.of(new NameHistoryEntryView("BP plc", "from 7 July 2015"))),
                new OrganisationNameHistoryView(
                    "Shell Expro",
                    List.of(),
                    List.of(new NameHistoryEntryView("Shell plc", "from 22 January 2022")))
            )
        ));
  }

  @Test
  void getPositionPageView_whenNoNamePeriodCoversThePositionDate_namesTheOrganisationByItsCurrentName() {
    var changeId = UUID.randomUUID();

    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder().withId(changeId).withLicencePosition(position).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Current Name Ltd",
            namePeriod("Interim Ltd", LocalDate.of(2015, Month.APRIL, 4), LocalDate.of(2020, Month.APRIL, 4)))
        ));

    var result = licencePositionViewService.getPositionPageView(position);

    assertThat(changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR))
        .isEqualTo(new AdministratorChangeView(
            null,
            "Current Name Ltd",
            changeId.toString(),
            null,
            new ChangeViewUrls(null, null, null, null)
        ));
    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Current Name Ltd"),
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Current Name Ltd",
                List.of(),
                List.of(new NameHistoryEntryView("Interim Ltd", "from 4 April 2015"))
            ))
        ));
  }

  @Test
  void getCorrectionPositionPageView_whenPositionDateIsCorrected_namesOrganisationsAsTheyWereOnTheCorrectedDate() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
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
        .thenReturn(Map.of(1, namePeriods("Shell plc",
            namePeriod("Shell UK Exploration", LocalDate.of(1987, Month.JANUARY, 12), LocalDate.of(1999, Month.MARCH, 3)),
            namePeriod("Shell plc", LocalDate.of(1999, Month.MARCH, 3), null))
        ));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Shell UK Exploration"),
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Shell UK Exploration",
                List.of(),
                List.of(new NameHistoryEntryView("Shell plc", "from 3 March 1999"))
            ))
        ));
  }

  private static LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(liveSurrenderChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(featureService.getFeaturesByIds(List.of(SURRENDERED_BLOCK.getId()))).thenReturn(List.of(SURRENDERED_BLOCK));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    var expected = new PartialSurrenderChangeView(
        "1 January 2026",
        List.of(new PartialSurrenderChangeView.BlockRow(SURRENDERED_BLOCK.getFeatureName(), "Full surrender")),
        LicencePositionChangeType.REMOVE_CHANGE,
        new ChangeViewUrls(
            null,
            null,
            ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
                .renderUndoPartialSurrender(correctionId, changeId.toString(), null)),
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1))).thenReturn(Map.of());

    licencePositionViewService.getPositionPageView(position);

    verify(organisationUnitQueryService).getOrganisationNameHistoriesByIds(List.of(1));
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(older));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(older))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1))).thenReturn(Map.of());

    var result = licencePositionViewService.getPositionPageView(newer);

    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("REF-1");
    // the queried position has no change of its own, so its resolved change/state are empty
    assertThat(result.orderedChangeViews()).isEmpty();
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""), List.of(), List.of()));
    assertThat(result.licenceType()).isEqualTo(LICENCE.getType());
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(addedPositionCorrection));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    assertThat(result.actions().addChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAddChangeController.class)
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(positionCorrection));

    var result = licencePositionViewService.getCorrectionAddedPositionPageView(correction, positionCorrection);

    assertThat(result.actions().addChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(correction.getId(), positionCorrection.getId(), null)));
    assertThat(result.orderedChangeViews()).isEmpty();
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""), List.of(), List.of()));
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
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
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

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(correctionId, POSITION_ID, null)));
    // The page-level "Add change" action stays available even when an administrator change is present.
    assertThat(result.actions().addChangeUrl())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(correctionId, POSITION_ID, null)));
  }

  @Test
  void getCorrectionPositionPageView_whenCorrectingCommittedChange_setsCorrectExistingUrl() {
    var correctionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    // A committed change not yet corrected in this correction (change type is null) must still route to the
    // correct-existing endpoint so the search selector pre-populates with the current administrator.
    var committedChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId).withLicencePosition(executed).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of(committedChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of());

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForCorrectingChange(correctionId, POSITION_ID, changeId.toString(), null)));
  }

  @Test
  void getLiveChronologicalPositions_holdsEachChangeAgainstThePositionItWasExecutedOn() {
    var firstPosition = executedPosition(LocalDate.of(2026, Month.JULY, 26));
    var secondPosition = executedPosition(LocalDate.of(2026, Month.AUGUST, 1));
    var changeId = UUID.randomUUID();
    var change = liveAdminChange(changeId, firstPosition);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(firstPosition, secondPosition));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(firstPosition, secondPosition)))
        .thenReturn(List.of(change));

    var result = licencePositionViewService.getLiveChronologicalPositions(LICENCE);

    assertThat(result).isEqualTo(List.of(
        ChronologicalPosition.fromLicencePosition(
            firstPosition,
            firstPosition.getLicenceTransaction().getRegulatorReference(),
            firstPosition.getPositionDate(),
            firstPosition.getPositionDateOrder(),
            List.of(new PositionChange(changeId.toString(), 1, null, List.of(ADMINISTRATOR_OPERATION)))),
        ChronologicalPosition.fromLicencePosition(
            secondPosition,
            secondPosition.getLicenceTransaction().getRegulatorReference(),
            secondPosition.getPositionDate(),
            secondPosition.getPositionDateOrder(),
            List.of())));
  }

  private static LicencePosition executedPosition(LocalDate positionDate) {
    return LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withIsExecuted(true)
        .withPositionDate(positionDate)
        .withPositionOrder(1)
        .build();
  }

  private static LicencePositionChange liveAdminChange(UUID changeId, LicencePosition licencePosition) {
    return LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(licencePosition)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .withChangeOrder(1)
        .build();
  }

  @Test
  void getCorrectionPositionPageView_whenAdminChangeRemoved_rendersExecutedAdminWithRemoveTypeAndRevertsState() {
    var correctionId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(correctionId).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
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

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, executed);

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.joiningOrganisationName()).isEqualTo("Executed Admin Org");
    assertThat(adminChange.changeType()).isEqualTo(LicencePositionChangeType.REMOVE_CHANGE);
    assertThat(adminChange.urls().remove()).isNull();
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""), List.of(), List.of()));
  }

  @Test
  void getCorrectedChronologicalPositions_whenReferenceCorrected_usesTheCorrectedReference() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().withCorrectionReference("COR-1").build();
    var referenceCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(payload)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(referenceCorrection));

    var result = licencePositionViewService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result).containsExactly(ChronologicalPosition.fromLicencePosition(
        executed,
        "COR-1",
        payload.effectiveDate(),
        payload.effectiveDateOrder(),
        List.of()));
  }

  @Test
  void getCorrectedChronologicalPositions_whenReferenceNotCorrected_usesTheExecutedReference() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("EXEC-1").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionViewService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result).containsExactly(ChronologicalPosition.fromLicencePosition(
        executed,
        "EXEC-1",
        executed.getPositionDate(),
        executed.getPositionDateOrder(),
        List.of()));
  }

  @Test
  void getCorrectedChronologicalPositions_whenChangeOrderCorrected_appliesNewOrderKeepingTypeAndOperations() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();
    var adminChangeId = UUID.randomUUID();
    var setEquityChangeId = UUID.randomUUID();

    var executed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).build();

    var liveAdminChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(adminChangeId).withLicencePosition(executed).withChangeOrder(1)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(5).build()))
        .build();
    var liveSetEquityChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(setEquityChangeId).withLicencePosition(executed).withChangeOrder(2)
        .withOperations(List.of(
            LicenceOperation.newSetEquityOperation().withTransferTo(5).withEquity(BigDecimal.valueOf(100)).build()))
        .build();

    var orderCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(
                LicencePositionChangeType.updateChangeOrder()
                    .withChangeId(setEquityChangeId.toString()).withChangeOrder(1).build(),
                LicencePositionChangeType.updateChangeOrder()
                    .withChangeId(adminChangeId.toString()).withChangeOrder(2).build()))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(liveAdminChange, liveSetEquityChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(orderCorrection));

    var result = licencePositionViewService.getCorrectedChronologicalPositions(correction, POSITION_ID);

    assertThat(result)
        .singleElement()
        .extracting(ChronologicalPosition::changes, list(PositionChange.class))
        .extracting(PositionChange::changeId, PositionChange::changeOrder, PositionChange::changeType)
        .containsExactly(
            tuple(setEquityChangeId.toString(), 1, null),
            tuple(adminChangeId.toString(), 2, null));
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

    var result = licencePositionViewService.getCorrectionAddedPositionPageView(correction, positionCorrection);

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls().correct())
        .isEqualTo(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(correctionId, positionCorrection.getId(), null)));
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

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(removed, current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed, current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, current);

    // the removed position is excluded from recalculation, and the current position carries no administrator
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""), List.of(), List.of()));
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

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));

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
  void getCorrectionPositionPageView_whenPositionRemovedInThisCorrection_omitsChangeActionUrlsAndAddChangeAction() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(UUID.randomUUID()).withLicence(LICENCE).build();

    var removed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID).withLicence(LICENCE).withIsExecuted(true)
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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed))).thenReturn(List.of(liveChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of(5, namePeriods(
        "Executed Admin Org", namePeriod("Executed Admin Org", LocalDate.of(1990, Month.JANUARY, 1), null))));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, removed);

    var adminChange = (AdministratorChangeView) changeViewOfType(result, LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(adminChange.urls()).isEqualTo(new ChangeViewUrls(null, null, null, null));
    assertThat(result.actions()).isEqualTo(LicencePositionPageView.Actions.none());
  }

  @Test
  void getCorrectionPositionPageView_whenViewingRemovedPosition_excludedFromValidationAndValidationStates() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var removed = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();
    var following = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JULY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var removedAdminChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(removed)
        .withOperations(List.of(LicenceOperation.newAdministratorChange().withOperator(7).build()))
        .build();

    var removeCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(removed, following));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed, following)))
        .thenReturn(List.of(removedAdminChange));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(removeCorrection));

    licencePositionViewService.getCorrectionPositionPageView(correction, removed);

    verify(licencePositionValidationService).validate(
        validationPositionsCaptor.capture(),
        validationStatesCaptor.capture(),
        isCarbonStorageCaptor.capture());

    assertThat(validationPositionsCaptor.getValue())
        .extracting(ChronologicalPosition::id)
        .containsExactly(following.getId());

    assertThat(isCarbonStorageCaptor.getValue()).isFalse();

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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));

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
            tuple("CORR-REF", "15 August 2026 (3)", true, expectedCorrectDateUrl));
  }

  @Test
  void getCorrectionPositionPageView_whenPayloadHasNoChangesOrDate_doesNotMarkPositionAsCorrected() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var current = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("CURRENT").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withIsExecuted(true).build();

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

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(current));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(current))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of(updateCorrection));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, current);

    assertThat(result.timelineViews())
        .extracting(
            LicencePositionTimelineView::regulatorReference,
            LicencePositionTimelineView::formattedPositionDate,
            LicencePositionTimelineView::correctedInThisCorrection)
        .containsExactly(
            tuple("CURRENT", "1 June 2026", false));
  }

  @Test
  void getCorrectionPositionPageView_whenSameDateOrderCorrected_recalculatesStateInCorrectedOrder() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var sameDate = LocalDate.of(2026, Month.JUNE, 1);
    var moved = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("MOVED").build())
        .withPositionDate(sameDate).withPositionOrder(1).withIsExecuted(true).build();
    var other = LicencePositionTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("OTHER").build())
        .withPositionDate(sameDate).withPositionOrder(2).withIsExecuted(true).build();

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

    var stateView = new LicencePositionStateView(new AdministratorStateView(""), List.of(), List.of());

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(moved, other));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(moved, other))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction))
        .thenReturn(List.of(movedUpdate, otherUpdate));

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, moved);

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

  @Test
  void getPositionPageView_resolvesOrganisationNamesForEquityOperations() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var setEquityOp = new SetEquityOperation(
        2, BigDecimal.TEN);
    var transferEquityOp = new TransferEquityOperation(
        1, 3, BigDecimal.TEN, true);

    var setEquityChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withChangeOrder(1)
        .withOperations(List.of(setEquityOp))
        .build();
    var transferEquityChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withChangeOrder(2)
        .withOperations(List.of(transferEquityOp))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position)))
        .thenReturn(List.of(setEquityChange, transferEquityChange));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(any())).thenReturn(Map.of());

    licencePositionViewService.getPositionPageView(position);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Integer>> idsCaptor = ArgumentCaptor.forClass(List.class);

    verify(organisationUnitQueryService).getOrganisationNameHistoriesByIds(idsCaptor.capture());

    assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1, 2, 3);
  }

  @Test
  void getPositionPageView_whenPartialSurrender_resolvesFeatureNamesIntoTheChangeView() {
    var positionDate = LocalDate.of(2026, Month.JANUARY, 1);
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(positionDate).withPositionOrder(1).withIsExecuted(true).build();

    var firstBlock = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
    var secondBlock = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);
    var partialSurrenderOp = new PartialSurrenderOperation(
        null, List.of(firstBlock.getId(), secondBlock.getId()),
        Map.of(
            firstBlock.getId(), new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.FULL_SURRENDER, UUID.randomUUID(), List.of()),
            secondBlock.getId(), new PartialSurrenderOperation.SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER, UUID.randomUUID(), List.of())));

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(partialSurrenderOp))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(featureService.getFeaturesByIds(List.of(firstBlock.getId(), secondBlock.getId())))
        .thenReturn(List.of(firstBlock, secondBlock));

    var result = licencePositionViewService.getPositionPageView(position);

    var expected = new PartialSurrenderChangeView(
        "1 January 2026",
        List.of(
            new PartialSurrenderChangeView.BlockRow(firstBlock.getFeatureName(), "Full surrender"),
            new PartialSurrenderChangeView.BlockRow(secondBlock.getFeatureName(), "Partial surrender")),
        null,
        ChangeViewUrls.none());
    assertThat(result.orderedChangeViews())
        .containsExactly(expected);
  }

  @Test
  void getPositionPageView_whenSubarea_resolvesFeatureNamesIntoTheChangeView() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var subarea = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "Subarea A");
    var subareaOp = new SubareaOperation(subarea.getId());

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(subareaOp))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(featureService.getFeaturesByIds(List.of(subarea.getId()))).thenReturn(List.of(subarea));

    var result = licencePositionViewService.getPositionPageView(position);

    var expected = new SubareaChangeView(subarea.getFeatureName(), null, ChangeViewUrls.none());
    assertThat(result.orderedChangeViews())
        .containsExactly(expected);
  }

  @Test
  void getOrderableChangeLabels_whenTwoSubareaChangesOnDifferentBlocks_labelsEachByChangeIdWithFeatureName() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var positionId = UUID.randomUUID();
    var position = LicencePositionTestUtil.newBuilder()
        .withId(positionId)
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true)
        .build();

    var firstBlock = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 1);
    var secondBlock = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 2);

    var firstChangeId = UUID.randomUUID();
    var firstChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(firstChangeId)
        .withLicencePosition(position)
        .withChangeOrder(1)
        .withOperations(List.of(new SubareaOperation(firstBlock.getId())))
        .build();

    var secondChangeId = UUID.randomUUID();
    var secondChange = LicencePositionChangeTestUtil.newBuilder()
        .withId(secondChangeId)
        .withLicencePosition(position)
        .withChangeOrder(2)
        .withOperations(List.of(new SubareaOperation(secondBlock.getId())))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position)))
        .thenReturn(List.of(firstChange, secondChange));
    when(featureService.getFeaturesByIds(List.of(firstBlock.getId(), secondBlock.getId())))
        .thenReturn(List.of(firstBlock, secondBlock));

    var result = licencePositionViewService.getOrderableChangeLabels(correction, positionId);

    assertThat(result).containsExactly(
        entry(firstChangeId, "Subarea change – %s".formatted(firstBlock.getFeatureName())),
        entry(secondChangeId, "Subarea change – %s".formatted(secondBlock.getFeatureName())));
  }

  @Test
  void getOrderableChangeLabels_whenNonSubareaChange_labelsByDisplayNameWithoutFeature() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var positionId = UUID.randomUUID();
    var position = LicencePositionTestUtil.newBuilder()
        .withId(positionId)
        .withLicence(LICENCE)
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true)
        .build();

    var changeId = UUID.randomUUID();
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withId(changeId)
        .withLicencePosition(position)
        .withChangeOrder(1)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));

    var result = licencePositionViewService.getOrderableChangeLabels(correction, positionId);

    assertThat(result).containsExactly(entry(changeId, ADMINISTRATOR_OPERATION.displayName()));
    verifyNoInteractions(featureService);
  }

  @Test
  void getPositionPageView_whenNoPartialSurrender_doesNotLookUpFeatureNames() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(ADMINISTRATOR_OPERATION))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1))).thenReturn(Map.of());

    licencePositionViewService.getPositionPageView(position);

    verifyNoInteractions(featureService);
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
        .withPositionDate(sameDate).withPositionOrder(1).withIsExecuted(true).build();

    var pos2 = LicencePositionTestUtil.newBuilder()
        .withId(pos2Id).withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-2").build())
        .withPositionDate(sameDate).withPositionOrder(2).withIsExecuted(true).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(pos1, pos2));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(pos1, pos2))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, pos1);

    var expectedCorrectOrderUrl1 = ReverseRouter.route(
        on(uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(correctionId, pos1Id, null));
    var expectedCorrectOrderUrl2 = ReverseRouter.route(
        on(uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionOrderChangeController.class)
            .renderCorrectionLicencePositionOrder(correctionId, pos2Id, null));

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
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withIsExecuted(true).build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(pos1));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(pos1))).thenReturn(List.of());
    when(licencePositionCorrectionService.getPositionCorrections(correction)).thenReturn(List.of());

    var result = licencePositionViewService.getCorrectionPositionPageView(correction, pos1);

    assertThat(result.timelineViews())
        .extracting(uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTimelineView::correctOrderUrl)
        .containsOnlyNulls();
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
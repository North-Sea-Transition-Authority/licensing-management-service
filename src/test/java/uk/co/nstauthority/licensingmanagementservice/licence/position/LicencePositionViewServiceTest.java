package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BlockStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.SubareaStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;
import uk.co.nstauthority.licensingmanagementservice.licence.position.partialsurrender.PartialSurrenderSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.subareachange.SubareaChangeSummaryController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

@ExtendWith(MockitoExtension.class)
class LicencePositionViewServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
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

  private LicencePositionViewService licencePositionViewService;

  @BeforeEach
  void setUp() {
    licencePositionViewService = new LicencePositionViewService(
        licencePositionService,
        new ChronologicalPositionService(
            licencePositionService,
            licencePositionChangeService,
            licencePositionCorrectionService
        ),
        new LicencePositionNameService(organisationUnitQueryService, featureService)
    );
  }

  @Test
  void getPositionPageView_returnsNameHistoryForTheStateOrganisationsSplitOnThePositionDate() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
  void getPositionPageView_namesOrganisationsAsTheyWereOnThePositionDate() {
    var changeId = UUID.randomUUID();

    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
            new ChangeViewUrls(null, null, null, null, null, null, null)
        ));
    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Shell Expro"),
            new LicenseeStateView(List.of()),
            "",
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Shell Expro",
                List.of(),
                List.of(new NameHistoryEntryView("Shell plc", "from 22 January 2022"))
            )),
            List.of()
        ));
  }

  @Test
  void getPositionPageView_whenBeneficialInterestHoldersHaveBeenRenamed_namesThemAsTheyWereOnThePositionDate() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
            new LicenseeStateView(List.of()),
            "",
            List.of(
                new BeneficialInterestView("BP Exploration", new BigDecimal("40"), OrganisationMarker.NOT_A_LICENSEE),
                new BeneficialInterestView("Shell Expro", new BigDecimal("60"), OrganisationMarker.NOT_A_LICENSEE)
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
            ),
            List.of()
        ));
  }

  @Test
  void getPositionPageView_whenBlockWithSubareaHeld_returnsBlockWithSubareaAndItsBeneficialInterests() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var block = FeatureTestUtil.blockFeature(UUID.randomUUID(), "21", 1);
    var subareaFeature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "A");
    var subarea = new SubareaDetails(subareaFeature.getId(), "Alpha", "A");

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(
            new BlockCreateOperation(List.of(block.getId()), Map.of(block.getId(), List.of(subarea))),
            new SubareaSetEquityOperation(block.getId(), subarea, 1, new BigDecimal("100"))))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(featureService.getFeaturesByIds(Set.of(block.getId(), subareaFeature.getId())))
        .thenReturn(List.of(block, subareaFeature));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1))).thenReturn(Map.of(
        1, namePeriods("Shell plc",
            namePeriod("Shell Expro", LocalDate.of(1999, Month.MARCH, 3), LocalDate.of(2022, Month.JANUARY, 22)),
            namePeriod("Shell plc", LocalDate.of(2022, Month.JANUARY, 22), null))
    ));

    var result = licencePositionViewService.getPositionPageView(position);

    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView(""),
            new LicenseeStateView(List.of()),
            "",
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Shell Expro",
                List.of(),
                List.of(new NameHistoryEntryView("Shell plc", "from 22 January 2022"))
            )),
            List.of(new BlockStateView(
                "Block %s".formatted(block.getFeatureName()),
                "0.00\u00A0km²",
                new SummaryMapView(List.of(block.getId()), 4230, block.getId()),
                List.of(new SubareaStateView(
                    "Subarea Alpha",
                    "0.00\u00A0km²",
                    new SummaryMapView(List.of(subareaFeature.getId()), 4230, subareaFeature.getId()),
                    List.of(new BeneficialInterestView(
                        "Shell Expro", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE))
                ))
            ))
        ));
  }

  @Test
  void getPositionPageView_whenBlockWithSubareaHeldFromAnEarlierPosition_returnsItAtTheLaterPosition() {
    var earlierPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();
    var laterPosition = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-2").build())
        .withPositionDate(LocalDate.of(2015, Month.JUNE, 1)).withPositionOrder(2).withStatus(LicencePositionStatus.EXECUTED).build();

    var block = FeatureTestUtil.blockFeature(UUID.randomUUID(), "21", 1);
    var subareaFeature = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "A");
    var subarea = new SubareaDetails(subareaFeature.getId(), "Alpha", "A");

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(earlierPosition)
        .withOperations(List.of(
            new BlockCreateOperation(List.of(block.getId()), Map.of(block.getId(), List.of(subarea))),
            new SubareaSetEquityOperation(block.getId(), subarea, 1, new BigDecimal("100"))))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(earlierPosition, laterPosition));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(earlierPosition, laterPosition)))
        .thenReturn(List.of(change));
    when(featureService.getFeaturesByIds(Set.of(block.getId(), subareaFeature.getId())))
        .thenReturn(List.of(block, subareaFeature));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1))).thenReturn(Map.of(
        1, namePeriods("Shell plc", namePeriod("Shell plc", LocalDate.of(2000, Month.JANUARY, 1), null))
    ));

    var result = licencePositionViewService.getPositionPageView(laterPosition);

    assertThat(result.stateView().blockStateViews())
        .containsExactly(new BlockStateView(
            "Block %s".formatted(block.getFeatureName()),
            "0.00\u00A0km²",
            new SummaryMapView(List.of(block.getId()), 4230, block.getId()),
            List.of(new SubareaStateView(
                "Subarea Alpha",
                "0.00\u00A0km²",
                new SummaryMapView(List.of(subareaFeature.getId()), 4230, subareaFeature.getId()),
                List.of(new BeneficialInterestView("Shell plc", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE))
            ))
        ));
  }

  @Test
  void getPositionPageView_whenNoNamePeriodCoversThePositionDate_namesTheOrganisationByItsCurrentName() {
    var changeId = UUID.randomUUID();

    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-1").build())
        .withPositionDate(LocalDate.of(2010, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
            new ChangeViewUrls(null, null, null, null, null, null, null)
        ));
    assertThat(result.stateView())
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView("Current Name Ltd"),
            new LicenseeStateView(List.of()),
            "",
            List.of(),
            List.of(new OrganisationNameHistoryView(
                "Current Name Ltd",
                List.of(),
                List.of(new NameHistoryEntryView("Interim Ltd", "from 4 April 2015"))
            )),
            List.of()
        ));
  }

  @Test
  void getPositionPageView_batchesAdministratorNameLookupIntoSingleRequest() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();
    var newer = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF-2").build())
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.SUBMITTED).build();

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
    assertThat(result.stateView()).isEqualTo(new LicencePositionStateView(
        new AdministratorStateView(""),
        new LicenseeStateView(List.of()),
        "",
        List.of(),
        List.of(),
        List.of()));
    assertThat(result.licenceType()).isEqualTo(LICENCE.getType());
  }

  @Test
  void getLatestPositionPageView_whenNoExecutedPositions_thenEmptyView() {
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of());

    var result = licencePositionViewService.getLatestPositionPageView(LICENCE, changeTypeFilter(LicenceOperation.SET_EQUITY));

    assertThat(result).isEqualTo(LicencePositionPageView.empty());
  }

  @Test
  void getLatestPositionPageView_whenFilterEmpty_thenLatestPositionIsUnfiltered() {
    var older = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var newer = executedPosition("REF-2", LocalDate.of(2026, Month.JUNE, 1));
    stubPositionsWithChanges(
        List.of(older, newer),
        List.of(
            changeOn(older, 1, ADMINISTRATOR_OPERATION),
            changeOn(newer, 1, setEquityOperation(1))
        )
    );

    var result = licencePositionViewService.getLatestPositionPageView(LICENCE, LicenceTimelineFilter.empty());

    assertThat(result.selectedPositionId()).isEqualTo(newer.getId());
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("REF-2", "REF-1");
    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.SET_EQUITY);
    assertThat(result.filterApplied()).isFalse();
  }

  @Test
  void getLatestPositionPageView_whenChangeTypes_thenLatestMatchingPositionIsShownWithMatchingTimeline() {
    var older = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var newer = executedPosition("REF-2", LocalDate.of(2026, Month.JUNE, 1));
    stubPositionsWithChanges(
        List.of(older, newer),
        List.of(
            changeOn(older, 1, ADMINISTRATOR_OPERATION),
            changeOn(newer, 1, setEquityOperation(1))
        )
    );

    var result = licencePositionViewService.getLatestPositionPageView(
        LICENCE,
        changeTypeFilter(LicenceOperation.LICENCE_ADMINISTRATOR)
    );

    var expectedChangeTypeOptions = new LinkedHashMap<String, String>();
    expectedChangeTypeOptions.put(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change");
    expectedChangeTypeOptions.put(LicenceOperation.SET_EQUITY, "Set equity");
    assertThat(result.selectedPositionId()).isEqualTo(older.getId());
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("REF-1");
    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.LICENCE_ADMINISTRATOR);
    assertThat(result.filterOptions()).isEqualTo(new LicenceTimelineFilterOptions(
        expectedChangeTypeOptions,
        Map.of("1", "Not available")
    ));
    assertThat(result.filterApplied()).isTrue();
  }

  @Test
  void getLatestPositionPageView_whenOrganisation_thenLatestPositionInvolvingItIsShownWithMatchingTimeline() {
    var older = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var newer = executedPosition("REF-2", LocalDate.of(2026, Month.JUNE, 1));
    stubPositionsWithChanges(
        List.of(older, newer),
        List.of(
            changeOn(older, 1, ADMINISTRATOR_OPERATION),
            changeOn(newer, 1, setEquityOperation(2))
        ),
        List.of(1, 2)
    );

    var result = licencePositionViewService.getLatestPositionPageView(LICENCE, organisationFilter(1));

    assertThat(result.selectedPositionId()).isEqualTo(older.getId());
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("REF-1");
    assertThat(result.filterApplied()).isTrue();
  }

  @Test
  void getLatestPositionPageView_whenOrganisationIsReplacedAsAdministrator_thenThatPositionIsShown() {
    var older = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var newer = executedPosition("REF-2", LocalDate.of(2026, Month.JUNE, 1));
    stubPositionsWithChanges(
        List.of(older, newer),
        List.of(
            changeOn(older, 1, ADMINISTRATOR_OPERATION),
            changeOn(newer, 1, LicenceOperation.newAdministratorChange().withOperator(2).build())
        ),
        List.of(1, 2)
    );

    var result = licencePositionViewService.getLatestPositionPageView(LICENCE, organisationFilter(1));

    assertThat(result.selectedPositionId()).isEqualTo(newer.getId());
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::regulatorReference)
        .containsExactly("REF-2", "REF-1");
  }

  @Test
  void getLatestPositionPageView_whenNoPositionMatches_thenNoMatchingPositionsView() {
    var position = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position)))
        .thenReturn(List.of(changeOn(position, 1, ADMINISTRATOR_OPERATION)));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd")));

    var result = licencePositionViewService.getLatestPositionPageView(
        LICENCE,
        changeTypeFilter(LicenceOperation.PARTIAL_SURRENDER)
    );

    assertThat(result).isEqualTo(LicencePositionPageView.noMatchingPositions(
        LICENCE.getType(),
        new LicenceTimelineFilterOptions(
            Map.of(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change"),
            Map.of("1", "Admin Ltd")
        )
    ));
  }

  @Test
  void getPositionPageView_whenChangeTypes_thenOnlyMatchingChangeViewsAreShown() {
    var position = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    stubPositionsWithChanges(
        List.of(position),
        List.of(
            changeOn(position, 1, ADMINISTRATOR_OPERATION),
            changeOn(position, 2, setEquityOperation(1))
        )
    );

    var result = licencePositionViewService.getPositionPageView(position, changeTypeFilter(LicenceOperation.SET_EQUITY));

    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.SET_EQUITY);
    assertThat(result.timelineViews())
        .extracting(LicencePositionTimelineView::positionId)
        .containsExactly(position.getId());
    assertThat(result.filterApplied()).isTrue();
  }

  @Test
  void getPositionPageView_whenOrganisation_thenEveryChangeViewIsShown() {
    var position = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    stubPositionsWithChanges(
        List.of(position),
        List.of(
            changeOn(position, 1, ADMINISTRATOR_OPERATION),
            changeOn(position, 2, setEquityOperation(2))
        ),
        List.of(1, 2)
    );

    var result = licencePositionViewService.getPositionPageView(position, organisationFilter(1));

    assertThat(result.orderedChangeViews())
        .extracting(LicencePositionChangeView::type)
        .containsExactly(LicenceOperation.LICENCE_ADMINISTRATOR, LicenceOperation.SET_EQUITY);
    assertThat(result.filterApplied()).isTrue();
  }

  @Test
  void getFilterOptions() {
    var position = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(
        changeOn(position, 1, ADMINISTRATOR_OPERATION),
        changeOn(position, 2, LicenceOperation.newLicenseeOperation()
            .withLicenseesToAdd(List.of(2))
            .withLicenseesToRemove(List.of(3))
            .build())
    ));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2, 3))).thenReturn(Map.of(
        1, namePeriods("Zeta Energy",
            namePeriod("Aardvark Oil", LocalDate.of(2000, Month.JANUARY, 1), LocalDate.of(2010, Month.JANUARY, 1))),
        2, namePeriods("beta Petroleum"),
        3, namePeriods("Alpha Resources")
    ));

    var result = licencePositionViewService.getFilterOptions(LICENCE);

    var expectedChangeTypeOptions = new LinkedHashMap<String, String>();
    expectedChangeTypeOptions.put(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change");
    expectedChangeTypeOptions.put(LicenceOperation.LICENSEE, "Licensee change");
    var expectedOrganisationOptions = new LinkedHashMap<String, String>();
    expectedOrganisationOptions.put("3", "Alpha Resources");
    expectedOrganisationOptions.put("2", "beta Petroleum");
    expectedOrganisationOptions.put("1", "Zeta Energy");
    assertThat(result.changeTypeOptions()).containsExactlyEntriesOf(expectedChangeTypeOptions);
    assertThat(result.organisationOptions()).containsExactlyEntriesOf(expectedOrganisationOptions);
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

  private void stubPositionsWithChanges(List<LicencePosition> positions, List<LicencePositionChange> changes) {
    stubPositionsWithChanges(positions, changes, List.of(1));
  }

  private void stubPositionsWithChanges(
      List<LicencePosition> positions,
      List<LicencePositionChange> changes,
      List<Integer> organisationIds
  ) {
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(positions);
    when(licencePositionChangeService.findByLicencePositionIn(positions)).thenReturn(changes);
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(organisationIds)).thenReturn(Map.of());
  }

  @Test
  void getPositionPageView_whenLicensee_resolvesOrganisationNames(){
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var licenseeOp = new LicenseeOperation(LicenseeOperation.LICENSEE_OPERATION_ID, List.of(1,2), List.of(3));

    var licenseeChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withChangeOrder(1)
        .withOperations(List.of(licenseeOp))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position)))
        .thenReturn(List.of(licenseeChange));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1,2,3))).thenReturn(Map.of());

    licencePositionViewService.getPositionPageView(position);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Integer>> idsCaptor = ArgumentCaptor.forClass(List.class);

    verify(organisationUnitQueryService).getOrganisationNameHistoriesByIds(idsCaptor.capture());

    assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1, 2, 3);
  }

  @Test
  void getPositionPageView_resolvesOrganisationNamesForEquityOperations() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
        .withPositionDate(positionDate).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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
    when(featureService.getFeaturesByIds(Set.of(firstBlock.getId(), secondBlock.getId())))
        .thenReturn(List.of(firstBlock, secondBlock));

    var result = licencePositionViewService.getPositionPageView(position);

    var expected = new PartialSurrenderChangeView(
        "1 January 2026",
        List.of(
            new PartialSurrenderChangeView.BlockRow("Block SHAPE 1 (0.00\u00A0km²)", List.of("Full surrender")),
            new PartialSurrenderChangeView.BlockRow("Block SHAPE 2 (0.00\u00A0km²)", List.of("Partial surrender"))),
        null,
        null,
        new ChangeViewUrls(null, null, null, null, null, null,
            ReverseRouter.route(on(PartialSurrenderSummaryController.class)
                .renderPartialSurrenderSummary(LICENCE, position, change, null))));
    assertThat(result.orderedChangeViews())
        .containsExactly(expected);
  }

  @Test
  void getPositionPageView_whenPartialSurrenderKeepsPartOfABlock_thenResolvesTheRetainedFeature() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1)
        .withStatus(LicencePositionStatus.EXECUTED).build();
    var block = FeatureTestUtil.builder()
        .withFeatureName("21/5")
        .withFeatureArea(new BigDecimal("219100000"))
        .build();
    var retainedPart = FeatureTestUtil.builder()
        .withFeatureName("21/5a")
        .withFeatureArea(new BigDecimal("164200000"))
        .build();
    var partialSurrenderOp = new PartialSurrenderOperation(
        null,
        List.of(block.getId()),
        Map.of(block.getId(), new PartialSurrenderOperation.SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER, null, List.of(), List.of(retainedPart.getId()))));
    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(partialSurrenderOp))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(featureService.getFeaturesByIds(Set.of(block.getId(), retainedPart.getId())))
        .thenReturn(List.of(block, retainedPart));

    var result = licencePositionViewService.getPositionPageView(position);

    var expected = new PartialSurrenderChangeView(
        "1 January 2026",
        List.of(new PartialSurrenderChangeView.BlockRow(
            "Block 21/5 (219.10\u00A0km²)",
            List.of("Block 21/5a retained (164.20\u00A0km²)", "54.90\u00A0km² surrendered"))),
        null,
        null,
        new ChangeViewUrls(null, null, null, null, null, null,
            ReverseRouter.route(on(PartialSurrenderSummaryController.class)
                .renderPartialSurrenderSummary(LICENCE, position, change, null))));
    assertThat(result.orderedChangeViews()).containsExactly(expected);
  }

  @Test
  void getPositionPageView_whenSubarea_resolvesFeatureNamesIntoTheChangeView() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

    var subarea = FeatureTestUtil.subareaFeature(UUID.randomUUID(), "Subarea A");
    var subareaOp = new SubareaOperation(subarea.getId(), List.of(), List.of());

    var change = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(position)
        .withOperations(List.of(subareaOp))
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(position));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(position))).thenReturn(List.of(change));
    when(featureService.getFeaturesByIds(Set.of(subarea.getId()))).thenReturn(List.of(subarea));

    var result = licencePositionViewService.getPositionPageView(position);

    var expected = new SubareaChangeView(
        subarea.getFeatureName(),
        List.of(),
        null,
        new ChangeViewUrls(null, null, null, null, null, null,
            ReverseRouter.route(on(SubareaChangeSummaryController.class)
                .renderSubareaChangeSummary(LICENCE, position, change, null))));
    assertThat(result.orderedChangeViews())
        .containsExactly(expected);
  }

  @Test
  void getPositionPageView_whenNoPartialSurrender_doesNotLookUpFeatureNames() {
    var position = LicencePositionTestUtil.newBuilder()
        .withLicence(LICENCE)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder().withRegulatorReference("REF").build())
        .withPositionDate(LocalDate.of(2026, Month.JANUARY, 1)).withPositionOrder(1).withStatus(LicencePositionStatus.EXECUTED).build();

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

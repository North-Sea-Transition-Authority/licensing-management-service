package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionStagedPayloadFilterServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
  private static final AdministratorOperation ADMINISTRATOR_OPERATION = LicenceOperation.newAdministratorChange()
      .withOperator(1)
      .build();

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FeatureService featureService;

  private LicencePositionStagedPayloadFilterService licencePositionStagedPayloadFilterService;

  @BeforeEach
  void setUp() {
    licencePositionStagedPayloadFilterService = new LicencePositionStagedPayloadFilterService(
        licencePositionStagedPayloadService,
        new ChronologicalPositionService(
            licencePositionService,
            licencePositionChangeService,
            licencePositionStagedPayloadService
        ),
        new LicencePositionNameService(organisationUnitQueryService, featureService)
    );
  }

  @Test
  void getCorrectionFilterOptions() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var removed = executedPosition("REMOVED", LocalDate.of(2026, Month.JANUARY, 1));
    var removeStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionStagedPayloadChangeType.REMOVE_POSITION)
        .withTargetLicencePosition(removed)
        .build();
    var addedPositionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(setEquityOperation(2)), 1)))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(removed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(removed)))
        .thenReturn(List.of(changeOn(removed, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction))
        .thenReturn(List.of(removeStagedPayload, addedPositionStagedPayload));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2))).thenReturn(Map.of(
        1, namePeriods("Zeta Energy"),
        2, namePeriods("Alpha Resources")
    ));

    var result = licencePositionStagedPayloadFilterService.getCorrectionFilterOptions(correction);

    var expectedChangeTypeOptions = new LinkedHashMap<String, String>();
    expectedChangeTypeOptions.put(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change");
    expectedChangeTypeOptions.put(LicenceOperation.SET_EQUITY, "Set equity");
    var expectedOrganisationOptions = new LinkedHashMap<String, String>();
    expectedOrganisationOptions.put("2", "Alpha Resources");
    expectedOrganisationOptions.put("1", "Zeta Energy");
    assertThat(result.changeTypeOptions()).containsExactlyEntriesOf(expectedChangeTypeOptions);
    assertThat(result.organisationOptions()).containsExactlyEntriesOf(expectedOrganisationOptions);
  }

  @Test
  void getFilteredCorrectionTimeline_whenLatestMatchIsAnAddedPosition_thenAddedPosition() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var executed = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var addedPositionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(LocalDate.of(2026, Month.JUNE, 1))
            .withChanges(List.of(AddChange.buildOperationsChange(List.of(ADMINISTRATOR_OPERATION), 1)))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(changeOn(executed, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction))
        .thenReturn(List.of(addedPositionStagedPayload));
    var result = licencePositionStagedPayloadFilterService.getFilteredCorrectionTimeline(correction, organisationFilter(1));

    assertThat(result).isEqualTo(new FilteredCorrectionTimeline.LatestMatchingPosition(
        LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, addedPositionStagedPayload.getPositionId())
    ));
  }

  @Test
  void getFilteredCorrectionTimeline_whenLaterPositionsDoNotMatch_thenLatestMatchingExecutedPosition() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var matching = executedPosition("MATCHING", LocalDate.of(2026, Month.JANUARY, 1));
    var later = executedPosition("LATER", LocalDate.of(2026, Month.MARCH, 1));
    var laterAdded = addedStagedPayload(correction, "ADD-REF", LocalDate.of(2026, Month.JUNE, 1), 1);

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(matching, later));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(matching, later)))
        .thenReturn(List.of(changeOn(matching, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction)).thenReturn(List.of(laterAdded));

    var result = licencePositionStagedPayloadFilterService.getFilteredCorrectionTimeline(correction, organisationFilter(1));

    assertThat(result).isEqualTo(new FilteredCorrectionTimeline.LatestMatchingPosition(
        LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, matching.getId())
    ));
  }

  @Test
  void getFilteredCorrectionTimeline_whenNothingMatches_thenNoMatchingPositions() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var executed = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(changeOn(executed, 1, ADMINISTRATOR_OPERATION)));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction)).thenReturn(List.of());
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Admin Ltd")));

    var result = licencePositionStagedPayloadFilterService.getFilteredCorrectionTimeline(
        correction,
        changeTypeFilter(LicenceOperation.PARTIAL_SURRENDER)
    );

    assertThat(result).isEqualTo(new FilteredCorrectionTimeline.NoMatchingPositions(
        LicencePositionPageView.noMatchingPositions(
            LICENCE.getType(),
            new LicenceTimelineFilterOptions(
                Map.of(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change"),
                Map.of("1", "Admin Ltd")
            )
        )
    ));
  }

  @Test
  void getFilteredCorrectionTimeline_whenOnlyMatchingChangeIsRemovedInTheCorrection_thenNoMatchingPositions() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var executed = executedPosition("REF-1", LocalDate.of(2026, Month.JANUARY, 1));
    var administratorChange = changeOn(executed, 1, ADMINISTRATOR_OPERATION);
    var licenseeChange = changeOn(executed, 2, LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(2)).build());
    var removeAdministratorChange = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionStagedPayloadChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(executed)
        .withPayload(UpdateLicencePositionPayloadTestUtil.newBuilder()
            .withChanges(List.of(new RemoveChange(administratorChange.getId().toString())))
            .build())
        .build();

    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(executed));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(executed)))
        .thenReturn(List.of(administratorChange, licenseeChange));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction))
        .thenReturn(List.of(removeAdministratorChange));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2))).thenReturn(Map.of(
        1, namePeriods("Admin Ltd"),
        2, namePeriods("Licensee Ltd")
    ));

    var result = licencePositionStagedPayloadFilterService.getFilteredCorrectionTimeline(
        correction,
        changeTypeFilter(LicenceOperation.LICENCE_ADMINISTRATOR)
    );

    assertThat(result).isEqualTo(new FilteredCorrectionTimeline.NoMatchingPositions(
        LicencePositionPageView.noMatchingPositions(
            LICENCE.getType(),
            new LicenceTimelineFilterOptions(
                Map.of(LicenceOperation.LICENSEE, "Licensee change"),
                Map.of("2", "Licensee Ltd")
            )
        )
    ));
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

  private static LicencePositionStagedPayload addedStagedPayload(
      LicenceCorrection correction,
      String correctionReference,
      LocalDate effectiveDate,
      int effectiveDateOrder
  ) {
    return LicencePositionStagedPayloadTestUtil.newBuilder()
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withEffectiveDate(effectiveDate)
            .withEffectiveDateOrder(effectiveDateOrder)
            .withCorrectionReference(correctionReference)
            .build())
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

  private static OrganisationNamePeriods namePeriods(String currentName) {
    return new OrganisationNamePeriods(currentName, List.of());
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionNameService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayloadTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenseeChangeServiceTest {

  @Captor
  private ArgumentCaptor<LicencePositionStagedPayload> licencePositionStagedPayloadCaptor;

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private ChronologicalPositionService chronologicalPositionService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FeatureService featureService;

  private LicenseeChangeService licenseeChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    licenseeChangeService = new LicenseeChangeService(
        licencePositionStagedPayloadService,
        chronologicalPositionService,
        new LicencePositionNameService(organisationUnitQueryService, featureService)
    );
  }

  @Test
  void addLicenseeChange_whenStagedPosition() {
    var payload = CreateLicencePositionPayloadTestUtil.newBuilder().build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var licencePositionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder().withPayload(payload).build();

    licenseeChangeService.addLicenseeChange(new CorrectionPosition.Staged(licencePositionStagedPayload), joiningLicenseeIds, withdrawingLicenseeIds);

    verify(licencePositionStagedPayloadService).save(licencePositionStagedPayload);
    var expectedLicenseeOperation = (LicenseeOperation)
        licencePositionStagedPayload.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(licencePositionStagedPayload.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void addLicenseeChange_whenExecutedPosition() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(10).build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var licencePositionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder().withPayload(payload).build();

    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition))
        .thenReturn(licencePositionStagedPayload);

    licenseeChangeService.addLicenseeChange(new CorrectionPosition.Executed(licenceCorrection, licencePosition), joiningLicenseeIds, withdrawingLicenseeIds);

    ArgumentCaptor<LicencePositionStagedPayload> captor = ArgumentCaptor.forClass(LicencePositionStagedPayload.class);
    verify(licencePositionStagedPayloadService).save(captor.capture());

    var positionStagedPayload = captor.getValue();
    var expectedLicenseeOperation = (LicenseeOperation)
        positionStagedPayload.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(positionStagedPayload.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void correctExistingLicenseeChange() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withPositionOrder(10).build();
    var licenceCorrection = LicenceCorrectionTestUtil.newBuilder().withCorrectionReference("correctionReference").build();
    var joiningLicenseeIds = List.of(2, 3, 4);
    var withdrawingLicenseeIds = List.of(1);
    var changeId = UUID.randomUUID().toString();
    var payload = UpdateLicencePositionPayloadTestUtil.newBuilder().build();
    var licencePositionStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder().withPayload(payload).build();

    when(licencePositionStagedPayloadService.getOrBuildUpdatePositionStagedPayload(licenceCorrection, licencePosition))
        .thenReturn(licencePositionStagedPayload);

    licenseeChangeService.correctExistingLicenseeChange(
        licencePosition,
        licenceCorrection,
        joiningLicenseeIds,
        withdrawingLicenseeIds,
        changeId
    );

    ArgumentCaptor<LicencePositionStagedPayload> captor = ArgumentCaptor.forClass(LicencePositionStagedPayload.class);
    verify(licencePositionStagedPayloadService).save(captor.capture());

    var positionStagedPayload = captor.getValue();
    var expectedLicenseeOperation = (LicenseeOperation)
        positionStagedPayload.getPayload().changes().getFirst().operations().getFirst().operation();
    assertThat(positionStagedPayload.getPayload().changes()).hasSize(1);
    assertThat(expectedLicenseeOperation.licenseesToAdd()).isEqualTo(joiningLicenseeIds);
    assertThat(expectedLicenseeOperation.licenseesToRemove()).isEqualTo(withdrawingLicenseeIds);
  }

  @Test
  void getUndoLicenseeChangeContext_whenPositionHasBeenChanged_restoresPreviousVersion() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var positionStagedPayloadToUndo = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();
    var otherStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();

    var previousPosition = licenseePosition(UUID.randomUUID(), 1, List.of(1, 2), List.of());
    var correctedPositions = List.of(previousPosition, licenseePosition(POSITION_ID, 2, List.of(), List.of(1)));
    var targetPositions = List.of(previousPosition, licenseePosition(POSITION_ID, 2, List.of(3), List.of(1)));

    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID))
        .thenReturn(correctedPositions);
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2)))
        .thenReturn(Map.of(1, namePeriods("BP"), 2, namePeriods("FLAMSTONE")));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction))
        .thenReturn(List.of(positionStagedPayloadToUndo, otherStagedPayload));
    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, List.of(otherStagedPayload), Set.of(POSITION_ID)))
        .thenReturn(targetPositions);
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2, 3)))
        .thenReturn(Map.of(1, namePeriods("BP"), 2, namePeriods("FLAMSTONE"), 3, namePeriods("SHELL")));

    var result = licenseeChangeService.getUndoLicenseeChangeContext(
        correction, positionStagedPayloadToUndo, POSITION_ID);

    assertThat(result.currentWithdrawingLicenseeNames()).containsExactly("BP");
    assertThat(result.currentJoiningLicenseeNames()).isEmpty();
    assertThat(result.restoredWithdrawingLicenseeNames()).containsExactly("BP");
    assertThat(result.restoredJoiningLicenseeNames()).containsExactly("SHELL");
    assertThat(result.resultingLicenseeNames()).containsExactlyInAnyOrder("FLAMSTONE", "SHELL");
  }

  @Test
  void getUndoLicenseeChangeContext_whenPositionHasNotBeenChanged_returnsRemovalToExistingPosition() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();

    var positionStagedPayloadToUndo = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();
    var otherStagedPayload = LicencePositionStagedPayloadTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .build();

    var correctedPositions = List.of(
        licenseePosition(UUID.randomUUID(), 1, List.of(1, 2), List.of()),
        licenseePosition(POSITION_ID, 2, List.of(3), List.of(1))
    );

    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID))
        .thenReturn(correctedPositions);
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2, 3)))
        .thenReturn(Map.of(1, namePeriods("BP"), 2, namePeriods("FLAMSTONE"), 3, namePeriods("SHELL")));
    when(licencePositionStagedPayloadService.getPositionStagedPayloads(correction))
        .thenReturn(List.of(positionStagedPayloadToUndo, otherStagedPayload));
    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, List.of(otherStagedPayload), Set.of(POSITION_ID)))
        .thenReturn(List.of());

    var result = licenseeChangeService.getUndoLicenseeChangeContext(
        correction, positionStagedPayloadToUndo, POSITION_ID);

    assertThat(result.currentWithdrawingLicenseeNames()).containsExactly("BP");
    assertThat(result.currentJoiningLicenseeNames()).containsExactly("SHELL");
    assertThat(result.restoredWithdrawingLicenseeNames()).isEmpty();
    assertThat(result.restoredJoiningLicenseeNames()).isEmpty();
    assertThat(result.resultingLicenseeNames()).containsExactlyInAnyOrder("BP", "FLAMSTONE");
  }

  @Test
  void getLicenseeChangeContext_emptyContext() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var position = ChronologicalPositionTestUtil.newBuilder().withId(POSITION_ID).build();

    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID))
        .thenReturn(List.of(position));

    var result = licenseeChangeService.getLicenseeChangeContext(correction, POSITION_ID);

    assertThat(result).isEqualTo(new LicenseeChangeContext(List.of(), List.of(), List.of(), List.of(), Map.of()));
  }

  @Test
  void getLicenseeChangeContext_resolvesLicenseeNames() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var previousPosition = licenseePosition(UUID.randomUUID(), 1, List.of(1), List.of());
    var currentPosition = ChronologicalPositionTestUtil.newBuilder().withId(POSITION_ID).withOrder(2).build();

    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID))
        .thenReturn(List.of(previousPosition, currentPosition));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1)))
        .thenReturn(Map.of(1, namePeriods("Current Licensee")));

    var result = licenseeChangeService.getLicenseeChangeContext(correction, POSITION_ID);

    assertThat(result).isEqualTo(
        new LicenseeChangeContext(List.of(), List.of(), List.of(1), List.of(1), Map.of(1, "Current Licensee")));
  }

  @Test
  void getLicenseeChangeContext_resolvesCurrentIds() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withLicence(LICENCE).build();
    var previousPosition = licenseePosition(UUID.randomUUID(), 1, List.of(1, 2), List.of());
    var currentPosition = licenseePosition(POSITION_ID, 2, List.of(3, 4), List.of(1, 2));

    when(chronologicalPositionService.getCorrectedChronologicalPositions(correction, POSITION_ID))
        .thenReturn(List.of(previousPosition, currentPosition));
    when(organisationUnitQueryService.getOrganisationNameHistoriesByIds(List.of(1, 2, 3, 4)))
        .thenReturn(Map.of());

    var result = licenseeChangeService.getLicenseeChangeContext(correction, POSITION_ID);

    assertThat(result).isEqualTo(
        new LicenseeChangeContext(List.of(3, 4), List.of(1, 2), List.of(1, 2), List.of(3, 4), Map.of()));
  }

  @ParameterizedTest(name = "{argumentSetName}")
  @MethodSource("getRestoredLicenseeChangeView")
  void getRestoredLicenseeChangeView(
      List<String> joiningNames,
      List<String> withdrawingNames,
      LicenseeChangeView expectedView
  ) {
    assertThat(licenseeChangeService.getRestoredLicenseeChangeView(joiningNames, withdrawingNames))
        .usingRecursiveComparison()
        .isEqualTo(expectedView);
  }

  private static Stream<Arguments> getRestoredLicenseeChangeView() {
    return Stream.of(
        Arguments.argumentSet("No names given",
            List.of(),
            List.of(),
            null),
        Arguments.argumentSet("Only joining names",
            List.of(),
            List.of("joiningOrg1"),
            new LicenseeChangeView(
                List.of(),
                List.of("joiningOrg1"),
                "",
                ChangeViewUrls.none()
            )),
        Arguments.argumentSet("Only withdrawing names",
            List.of("withdrawingOrg1"),
            List.of(),
            new LicenseeChangeView(
                List.of("withdrawingOrg1"),
                List.of(),
                "",
                ChangeViewUrls.none()
            )),
        Arguments.argumentSet("Joining and withdrawing names",
            List.of("withdrawingOrg1"),
            List.of("joiningOrg1"),
            new LicenseeChangeView(
                List.of("withdrawingOrg1"),
                List.of("joiningOrg1"),
                "",
                ChangeViewUrls.none()
            ))
    );
  }

  private static ChronologicalPosition licenseePosition(
      UUID positionId,
      int order,
      List<Integer> licenseesToAdd,
      List<Integer> licenseesToRemove
  ) {
    return ChronologicalPositionTestUtil.newBuilder()
        .withId(positionId)
        .withOrder(order)
        .withChanges(List.of(PositionChangeTestUtil.newBuilder()
            .withLicenseeOperation(licenseesToAdd, licenseesToRemove)
            .build()))
        .build();
  }

  private static OrganisationNamePeriods namePeriods(String currentName) {
    return new OrganisationNamePeriods(currentName, List.of());
  }
}

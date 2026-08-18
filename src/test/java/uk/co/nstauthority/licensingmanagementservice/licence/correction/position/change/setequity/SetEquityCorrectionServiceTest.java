package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation.LicencePositionChangeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

@ExtendWith(MockitoExtension.class)
class SetEquityCorrectionServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final String CORRECTION_REFERENCE = "TEST-REF";
  private static final LicencePosition LICENCE_POSITION = LicencePositionTestUtil.newBuilder()
      .withLicence(LICENCE)
      .build();
  private static final int TRANSFER_TO_ID = 116;

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private SetEquityCorrectionService setEquityCorrectionService;

  @Test
  void getCommittedSetEquityOperations_returnsSetEquityOperationsFromThePayload() {
    var change = setEquityAddChange(UUID.randomUUID().toString());
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE, List.of(change));
    var correction = LicencePositionCorrectionTestUtil.newBuilder().withPayload(payload).build();
    var operations = List.of(setEquityOp(TRANSFER_TO_ID, 10));

    when(licencePositionCorrectionService.getAddOperationsOfType(List.of(change), SetEquityOperation.class))
        .thenReturn(operations);

    assertThat(setEquityCorrectionService.getCommittedSetEquityOperations(correction)).isEqualTo(operations);
  }

  @Test
  void commitSetEquity_replacesTheSetEquityAddChangeOnTheCorrection() {
    var correction = LicencePositionCorrectionTestUtil.newBuilder().build();
    var operations = List.of(setEquityOp(TRANSFER_TO_ID, 10));

    setEquityCorrectionService.commitSetEquity(correction, operations);

    verify(licencePositionCorrectionService).replaceAddChangeFor(correction, SetEquityOperation.class, operations);
  }

  @Test
  void getSetEquityViews_mapsOperationsToViewsWithResolvedNames() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1, 2)))
        .thenReturn(Map.of(1, "Org One", 2, "Org Two"));

    var views = setEquityCorrectionService.getSetEquityViews(List.of(setEquityOp(1, 40), setEquityOp(2, 60)));

    assertThat(views)
        .extracting(SetEquityRow::organisationName, SetEquityRow::equity)
        .containsExactly(
            tuple("Org One", BigDecimal.valueOf(40)),
            tuple("Org Two", BigDecimal.valueOf(60))
        );
  }

  @Test
  void getSetEquityViews_whenNameUnknown_usesEmptyString() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1))).thenReturn(Map.of());

    var views = setEquityCorrectionService.getSetEquityViews(List.of(setEquityOp(1, 40)));

    assertThat(views).singleElement().extracting(SetEquityRow::organisationName).isEqualTo("");
  }

  @Test
  void commitSetEquityForExecutedPosition_replacesSetEquityOnTheUpdateCorrection() {
    var operations = List.of(setEquityOp(TRANSFER_TO_ID, 10));
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .build();

    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(positionCorrection);

    setEquityCorrectionService.commitSetEquityForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION, operations);

    verify(licencePositionCorrectionService)
        .replaceAddChangeFor(positionCorrection, SetEquityOperation.class, operations);
  }

  @Test
  void getCommittedSetEquityOperationsForExecutedPosition_whenUpdateCorrectionExists_returnsOperations() {
    var change = setEquityAddChange(UUID.randomUUID().toString());
    var payload = new UpdateLicencePositionPayload(null, null, CORRECTION_REFERENCE, List.of(change));
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(LICENCE_POSITION)
        .withPayload(payload)
        .build();
    var operations = List.of(setEquityOp(TRANSFER_TO_ID, 10));

    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.of(positionCorrection));
    when(licencePositionCorrectionService.getAddOperationsOfType(List.of(change), SetEquityOperation.class))
        .thenReturn(operations);

    assertThat(setEquityCorrectionService
        .getCommittedSetEquityOperationsForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .isEqualTo(operations);
  }

  @Test
  void getCommittedSetEquityOperationsForExecutedPosition_whenNoCorrection_returnsEmpty() {
    when(licencePositionCorrectionService.findUpdatePositionCorrection(LICENCE_CORRECTION, LICENCE_POSITION))
        .thenReturn(Optional.empty());

    assertThat(setEquityCorrectionService
        .getCommittedSetEquityOperationsForExecutedPosition(LICENCE_CORRECTION, LICENCE_POSITION))
        .isEmpty();
  }

  private static SetEquityOperation setEquityOp(int transferTo, int equity) {
    return new SetEquityOperation(transferTo, BigDecimal.valueOf(equity));
  }

  private LicencePositionChangeType setEquityAddChange(String changeId) {
    var operation = LicenceOperation.newSetEquityOperation()
        .withTransferTo(TRANSFER_TO_ID)
        .withEquity(BigDecimal.TEN)
        .build();
    return LicencePositionChangeType.addChange()
        .withChangeId(changeId)
        .withChangeOrder(1)
        .withOperations(List.of(LicencePositionChangeOperation.newLicencePositionAddOperation()
            .withOperationId(operation.id())
            .withOperation(operation)
            .build()))
        .build();
  }
}
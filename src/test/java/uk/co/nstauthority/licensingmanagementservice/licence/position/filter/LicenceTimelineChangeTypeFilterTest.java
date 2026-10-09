package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

class LicenceTimelineChangeTypeFilterTest {

  @Test
  void getAvailableChangeTypeOptions_whenChangesSpanPositions_thenTypesInChangeTypeOrder() {
    var positions = List.of(
        ChronologicalPositionTestUtil.newBuilder()
            .withChanges(List.of(
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(LicenceOperation.newSubAreaOperation()
                        .withBlockFeatureId(UUID.randomUUID())
                        .build()))
                    .build(),
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(LicenceOperation.newTransferEquityOperation()
                        .withTransferFrom(1)
                        .withTransferTo(2)
                        .withEquity(new BigDecimal("10"))
                        .build()))
                    .build(),
                PositionChangeTestUtil.newBuilder()
                    .withSetEquityOperation(1, new BigDecimal("100"))
                    .build(),
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(LicenceOperation.newBlockEndOperation().build()))
                    .build()
            ))
            .build(),
        ChronologicalPositionTestUtil.newBuilder()
            .withChanges(List.of(
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(partialSurrenderOperation()))
                    .build(),
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(LicenceOperation.newLicenseeOperation().withLicenseesToAdd(List.of(3)).build()))
                    .build(),
                PositionChangeTestUtil.newBuilder().withAdministratorOperation(1).build(),
                PositionChangeTestUtil.newBuilder().withSetEquityOperation(2, new BigDecimal("50")).build()
            ))
            .build()
    );

    var result = LicenceTimelineChangeTypeFilter.getAvailableChangeTypeOptions(positions);

    var expected = new LinkedHashMap<String, String>();
    expected.put(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change");
    expected.put(LicenceOperation.LICENSEE, "Licensee change");
    expected.put(LicenceOperation.SET_EQUITY, "Set equity");
    expected.put(LicenceOperation.TRANSFER_EQUITY, "Transfer equity");
    expected.put(LicenceOperation.PARTIAL_SURRENDER, "Partial surrender");
    expected.put(LicenceOperation.SUBAREA, "Subarea change");
    assertThat(result).containsExactlyEntriesOf(expected);
  }

  @Test
  void getAvailableChangeTypeOptions_whenChangeMixesVisibleAndHiddenOperations_thenOnlyVisibleTypesOffered() {
    var positions = List.of(
        ChronologicalPositionTestUtil.newBuilder()
            .withChanges(List.of(
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(
                        partialSurrenderOperation(),
                        LicenceOperation.newBlockEndOperation().build()
                    ))
                    .build()
            ))
            .build()
    );

    var result = LicenceTimelineChangeTypeFilter.getAvailableChangeTypeOptions(positions);

    var expected = new LinkedHashMap<String, String>();
    expected.put(LicenceOperation.PARTIAL_SURRENDER, "Partial surrender");
    assertThat(result).containsExactlyEntriesOf(expected);
  }

  @Test
  void getAvailableChangeTypeOptions_whenVisibleTypeMissingFromChangeTypeOrder_thenOfferedLastByDisplayName() {
    var laterUnorderedOperation = unorderedOperation("unordered-b", "Beta change");
    var earlierUnorderedOperation = unorderedOperation("unordered-a", "Alpha change");

    var positions = List.of(
        ChronologicalPositionTestUtil.newBuilder()
            .withChanges(List.of(
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(laterUnorderedOperation))
                    .build(),
                PositionChangeTestUtil.newBuilder()
                    .withOperations(List.of(earlierUnorderedOperation))
                    .build(),
                PositionChangeTestUtil.newBuilder().withSetEquityOperation(1, new BigDecimal("100")).build()
            ))
            .build()
    );

    var result = LicenceTimelineChangeTypeFilter.getAvailableChangeTypeOptions(positions);

    var expected = new LinkedHashMap<String, String>();
    expected.put(LicenceOperation.SET_EQUITY, "Set equity");
    expected.put("unordered-a", "Alpha change");
    expected.put("unordered-b", "Beta change");
    assertThat(result).containsExactlyEntriesOf(expected);
  }

  private static LicenceOperation partialSurrenderOperation() {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .build();
  }

  private static VisibleLicenceOperation unorderedOperation(String type, String displayName) {
    var operation = mock(VisibleLicenceOperation.class);
    when(operation.type()).thenReturn(type);
    when(operation.displayName()).thenReturn(displayName);
    return operation;
  }
}

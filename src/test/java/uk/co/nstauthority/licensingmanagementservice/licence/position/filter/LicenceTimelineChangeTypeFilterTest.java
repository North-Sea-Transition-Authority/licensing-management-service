package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

class LicenceTimelineChangeTypeFilterTest {

  @Test
  void getAvailableChangeTypeOptions() {
    var positions = List.of(
        ChronologicalPositionTestUtil.newBuilder()
            .withChanges(List.of(
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
                PositionChangeTestUtil.newBuilder().withAdministratorOperation(1).build(),
                PositionChangeTestUtil.newBuilder().withSetEquityOperation(2, new BigDecimal("50")).build()
            ))
            .build()
    );

    var result = LicenceTimelineChangeTypeFilter.getAvailableChangeTypeOptions(positions);

    var expected = new LinkedHashMap<String, String>();
    expected.put(LicenceOperation.LICENCE_ADMINISTRATOR, "Licence administrator change");
    expected.put(LicenceOperation.SET_EQUITY, "Set equity");
    assertThat(result).containsExactlyEntriesOf(expected);
  }
}

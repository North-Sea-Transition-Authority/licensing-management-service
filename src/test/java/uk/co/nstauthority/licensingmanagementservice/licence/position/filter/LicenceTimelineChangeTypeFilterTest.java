package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

class LicenceTimelineChangeTypeFilterTest {

  private static final ChronologicalPosition ADMINISTRATOR_AND_SET_EQUITY_POSITION =
      ChronologicalPositionTestUtil.newBuilder()
          .withChanges(List.of(
              PositionChangeTestUtil.newBuilder().withAdministratorOperation(1).build(),
              PositionChangeTestUtil.newBuilder().withSetEquityOperation(1, new BigDecimal("100")).build()
          ))
          .build();

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

  @ParameterizedTest
  @MethodSource("positionMatchesArguments")
  void positionMatches(Set<String> changeTypes, boolean expected) {
    var result = LicenceTimelineChangeTypeFilter.positionMatches(ADMINISTRATOR_AND_SET_EQUITY_POSITION, changeTypes);

    assertThat(result).isEqualTo(expected);
  }

  private static List<Arguments> positionMatchesArguments() {
    return List.of(
        Arguments.of(Set.of(), true),
        Arguments.of(Set.of(LicenceOperation.SET_EQUITY), true),
        Arguments.of(Set.of(LicenceOperation.LICENSEE, LicenceOperation.LICENCE_ADMINISTRATOR), true),
        Arguments.of(Set.of(LicenceOperation.PARTIAL_SURRENDER), false)
    );
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.position.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

class LicenceTimelineFilterTest {

  private static final Integer ORGANISATION_ID = 1;
  private static final Integer OTHER_ORGANISATION_ID = 2;

  @ParameterizedTest
  @MethodSource("matchesArguments")
  void matches(ChronologicalPosition position, Integer outgoingAdministratorId, LicenceTimelineFilter filter,
               boolean expected) {
    var result = filter.matches(position, outgoingAdministratorId);

    assertThat(result).isEqualTo(expected);
  }

  private static List<Arguments> matchesArguments() {
    var organisationFilter = new LicenceTimelineFilter(Set.of(), Set.of(ORGANISATION_ID));
    return List.of(
        Arguments.of(
            named("empty filter on a position with no card", position(LicenceOperation.newBlockEndOperation().build())),
            null,
            LicenceTimelineFilter.empty(),
            true
        ),
        Arguments.of(
            named("incoming administrator", position(administrator(ORGANISATION_ID))),
            null,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("outgoing administrator", position(administrator(OTHER_ORGANISATION_ID))),
            ORGANISATION_ID,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("outgoing administrator on a position without an administrator change",
                position(setEquity(OTHER_ORGANISATION_ID))),
            ORGANISATION_ID,
            organisationFilter,
            false
        ),
        Arguments.of(
            named("set equity", position(setEquity(ORGANISATION_ID))),
            null,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("transfer equity from", position(transferEquity(ORGANISATION_ID, OTHER_ORGANISATION_ID))),
            null,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("transfer equity to", position(transferEquity(OTHER_ORGANISATION_ID, ORGANISATION_ID))),
            null,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("licensee added", position(licensee(List.of(ORGANISATION_ID), List.of()))),
            null,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("licensee removed", position(licensee(List.of(), List.of(ORGANISATION_ID)))),
            null,
            organisationFilter,
            true
        ),
        Arguments.of(
            named("another organisation", position(setEquity(OTHER_ORGANISATION_ID))),
            null,
            organisationFilter,
            false
        ),
        Arguments.of(
            named("operation involving no organisation", position(LicenceOperation.newBlockEndOperation().build())),
            null,
            organisationFilter,
            false
        ),
        Arguments.of(
            named("change type only", position(administrator(ORGANISATION_ID), setEquity(ORGANISATION_ID))),
            null,
            new LicenceTimelineFilter(Set.of(LicenceOperation.LICENSEE, LicenceOperation.SET_EQUITY), Set.of()),
            true
        ),
        Arguments.of(
            named("change type not on the position", position(administrator(ORGANISATION_ID))),
            null,
            new LicenceTimelineFilter(Set.of(LicenceOperation.PARTIAL_SURRENDER), Set.of()),
            false
        ),
        Arguments.of(
            named("change type and organisation on the same operation",
                position(licensee(List.of(ORGANISATION_ID), List.of()), setEquity(OTHER_ORGANISATION_ID))),
            null,
            new LicenceTimelineFilter(Set.of(LicenceOperation.LICENSEE), Set.of(ORGANISATION_ID)),
            true
        ),
        Arguments.of(
            named("change type and organisation on different operations",
                position(licensee(List.of(ORGANISATION_ID), List.of()), setEquity(OTHER_ORGANISATION_ID))),
            null,
            new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of(ORGANISATION_ID)),
            false
        )
    );
  }

  private static ChronologicalPosition position(LicenceOperation... operations) {
    return ChronologicalPositionTestUtil.newBuilder()
        .withChanges(List.of(PositionChangeTestUtil.newBuilder().withOperations(List.of(operations)).build()))
        .build();
  }

  private static LicenceOperation administrator(Integer operatorId) {
    return LicenceOperation.newAdministratorChange().withOperator(operatorId).build();
  }

  private static LicenceOperation setEquity(Integer transferTo) {
    return LicenceOperation.newSetEquityOperation().withTransferTo(transferTo).withEquity(new BigDecimal("100")).build();
  }

  private static LicenceOperation transferEquity(Integer transferFrom, Integer transferTo) {
    return LicenceOperation.newTransferEquityOperation()
        .withTransferFrom(transferFrom)
        .withTransferTo(transferTo)
        .withEquity(new BigDecimal("10"))
        .build();
  }

  private static LicenceOperation licensee(List<Integer> licenseesToAdd, List<Integer> licenseesToRemove) {
    return LicenceOperation.newLicenseeOperation()
        .withLicenseesToAdd(licenseesToAdd)
        .withLicenseesToRemove(licenseesToRemove)
        .build();
  }
}

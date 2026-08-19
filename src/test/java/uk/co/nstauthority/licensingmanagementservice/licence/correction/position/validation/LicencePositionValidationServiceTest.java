package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

class LicencePositionValidationServiceTest {

  private final LicencePositionValidationService service = new LicencePositionValidationService();

  @Test
  void validate_whenNoPositions_returnsNoErrors() {
    assertThat(service.validate(List.of(), LicencePositionStateResolver.resolve(List.of()), true)).isEmpty();
  }

  @Test
  void validate_marksOnlyTheFirstPositionAsFirst() {
    // The first position has no admin change, so the flag is true and causes an error
    var first = ChronologicalPositionTestUtil.live(LicencePositionTestUtil.newBuilder().build());
    var second = ChronologicalPositionTestUtil.live(LicencePositionTestUtil.newBuilder().build());

    var chronologicalPositions = List.of(first, second);
    var states = LicencePositionStateResolver.resolve(chronologicalPositions);

    assertThat(service.validate(chronologicalPositions, states, false))
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), AdministratorPositionRule.FIRST_POSITION_MUST_HAVE_ADMINISTRATOR.getMessage()));
  }

  @Test
  void validate_aggregatesAdministratorErrorsAcrossPositions() {
    var first = LicencePositionTestUtil.newBuilder().build();
    var second = LicencePositionTestUtil.newBuilder().build();
    var chronologicalPositions = List.of(
        ChronologicalPositionTestUtil.live(first),
        ChronologicalPositionTestUtil.live(
            second,
            LicenceOperation.newAdministratorChange().withOperator(2).build(),
            LicenceOperation.newAdministratorChange().withOperator(3).build())
    );
    var states = LicencePositionStateResolver.resolve(chronologicalPositions);

    assertThat(service.validate(chronologicalPositions, states, false))
        .extracting(PositionValidationError::message)
        .containsExactlyInAnyOrder(AdministratorPositionRule.FIRST_POSITION_MUST_HAVE_ADMINISTRATOR.getMessage(),
            AdministratorPositionRule.ONLY_ONE_ADMINISTRATOR_CHANGE.getMessage());
  }

  @Test
  void validate_whenPositionsShareTransactionEachWithBeneficialInterestChange_flagsEachPosition() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "100"));
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "100"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), true);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()),
            tuple(second.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()));
  }

  @Test
  void validate_whenPositionsHaveDistinctTransactions_doesNotFlagSingleChangeError() {
    var first = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "100"));
    var second = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "100"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), true);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenSinglePositionHasTwoBeneficialInterestChanges_flagsSingleChangeError() {
    var position = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18),
        setEquityChange(1, "100"), setEquityChange(1, "100"));

    var positions = List.of(position);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), true);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(tuple(position.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()));
  }

  @Test
  void validate_whenNotCarbonStorageLicence_flagsEachBeneficialInterestChangeAsCarbonStorageOnly() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18),
        administratorChange(), setEquityChange(1, "100"));
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "100"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), false);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), EquityOperationRule.CARBON_STORAGE_LICENCE_ONLY.getMessage()),
            tuple(second.id(), EquityOperationRule.CARBON_STORAGE_LICENCE_ONLY.getMessage()));
  }

  @Test
  void validate_delegatesEquityValidationToEachPosition() {
    var position = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "60"));

    var positions = List.of(position);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), true);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(position.id(), EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED.getMessage()));
  }

  @Test
  void validate_whenOriginatingPositionSharesTransaction_returnsEquityErrorThenSingleChangeErrorForOriginOnly() {
    var transactionId = UUID.randomUUID();
    var first = position(transactionId, LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "60"));
    var second = position(transactionId, LocalDate.of(2026, Month.JULY, 9), setEquityChange(1, "60"));

    var positions = List.of(first, second);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), true);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(first.id(), EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED.getMessage()),
            tuple(first.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()),
            tuple(second.id(), EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION.getMessage()));
  }

  @Test
  void validate_whenInvalidTotalCarriedAcrossPositions_flagsOnlyTheOriginatingPosition() {
    var origin = position(UUID.randomUUID(), LocalDate.of(2026, Month.JUNE, 18), setEquityChange(1, "60"));
    var carriesForwardFirst = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 9));
    var carriesForwardSecond = position(UUID.randomUUID(), LocalDate.of(2026, Month.JULY, 16));

    var positions = List.of(origin, carriesForwardFirst, carriesForwardSecond);
    var errors = service.validate(positions, LicencePositionStateResolver.resolve(positions), true);

    assertThat(errors)
        .extracting(PositionValidationError::positionId, PositionValidationError::message)
        .containsExactly(
            tuple(origin.id(), EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED.getMessage()));
  }

  private static ChronologicalPosition position(UUID transactionId, LocalDate date, PositionChange... changes) {
    return new ChronologicalPosition(UUID.randomUUID(), transactionId, date, 1, List.of(changes));
  }

  private static PositionChange administratorChange() {
    return new PositionChange(
        UUID.randomUUID().toString(),
        0,
        null,
        List.of(LicenceOperation.newAdministratorChange().withOperator(999).build())
    );
  }

  private static PositionChange setEquityChange(int organisationId, String equity) {
    return new PositionChange(
        UUID.randomUUID().toString(),
        1,
        null,
        List.of(LicenceOperation.newSetEquityOperation()
            .withTransferTo(organisationId)
            .withEquity(new BigDecimal(equity))
            .build())
    );
  }
}
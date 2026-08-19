package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

class PositionValidationErrorTest {

  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID TRANSACTION_ID = UUID.randomUUID();

  @Test
  void toErrorSummaryItems_reversesOrderAndFormatsMessage() {
    var errorOne = new PositionValidationError(UUID.randomUUID(), "Position one", null, null, "message one");
    var errorTwo = new PositionValidationError(UUID.randomUUID(), "Position two", null, null, "message two");

    var items = PositionValidationError.toErrorSummaryItems(List.of(errorOne, errorTwo));

    assertThat(items)
        .extracting(ErrorSummaryItem::getErrorMessage)
        .containsExactly("Position two - message two", "Position one - message one");
    assertThat(items).extracting(ErrorSummaryItem::getDisplayOrder).containsExactly(0, 1);
    assertThat(items)
        .extracting(ErrorSummaryItem::getFieldName)
        .containsExactly(errorTwo.positionId().toString(), errorOne.positionId().toString());
  }

  @Test
  void forPosition_withMessage_setsPositionNameWithOrderAndNoChangeOrOperation() {
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withDate(LocalDate.of(2026, Month.AUGUST, 5))
        .withOrder(2)
        .build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withIsFirstPosition(true)
        .build();

    PositionValidationRule rule = () -> "message";
    var error = PositionValidationError.forPosition(context, rule);

    assertThat(error.positionId()).isEqualTo(position.id());
    assertThat(error.positionName())
        .isEqualTo(DateUtil.formatLongDateWithOrder(position.date(), position.order()));
    assertThat(error.changeId()).isNull();
    assertThat(error.operationType()).isNull();
    assertThat(error.message()).isEqualTo("message");
  }

  @Test
  void forPosition_fromContextWithRule_setsPositionDetailsAndRuleMessage() {
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position(POSITION_ID, LocalDate.of(2026, Month.JANUARY, 1), 2))
        .build();

    var error = PositionValidationError.forPosition(context, EquityPositionRule.EQUITY_HOLDER_OUT_OF_RANGE);

    assertThat(error)
        .extracting(
            PositionValidationError::positionId,
            PositionValidationError::positionName,
            PositionValidationError::changeId,
            PositionValidationError::operationType,
            PositionValidationError::message)
        .containsExactly(
            POSITION_ID, "1 January 2026 (2)", null, null, "Each holder must have between 0% and 100% equity");
  }

  @Test
  void forPosition_fromPositionWithRule_setsPositionDetailsAndRuleMessage() {
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position(POSITION_ID, LocalDate.of(2026, Month.JANUARY, 1), 1))
        .build();

    var error = PositionValidationError.forPosition(context, EquityPositionRule.SINGLE_CHANGE_PER_TRANSACTION);

    assertThat(error)
        .extracting(
            PositionValidationError::positionId,
            PositionValidationError::positionName,
            PositionValidationError::changeId,
            PositionValidationError::operationType,
            PositionValidationError::message)
        .containsExactly(
            POSITION_ID, "1 January 2026", null, null,
            "A licence on a transaction can only have one carbon storage beneficial interest change");
  }

  @Test
  void forOperation_setsOperationType() {
    var context = PositionValidationContextTestUtil.newBuilder().build();

    var error = PositionValidationError.forOperation(context, "SOME_TYPE", "message");

    assertThat(error.operationType()).isEqualTo("SOME_TYPE");
    assertThat(error.changeId()).isNull();
    assertThat(error.message()).isEqualTo("message");
  }

  @Test
  void withChangeId_returnsCopyWithChangeIdSet() {
    var original = new PositionValidationError(UUID.randomUUID(), "name", null, "TYPE", "message");

    var updated = original.withChangeId("change-1");

    assertThat(updated.changeId()).isEqualTo("change-1");
    assertThat(updated.positionId()).isEqualTo(original.positionId());
    assertThat(updated.positionName()).isEqualTo(original.positionName());
    assertThat(updated.operationType()).isEqualTo("TYPE");
    assertThat(updated.message()).isEqualTo("message");
  }

  @Test
  void toErrorSummaryItems_whenPositionsHaveDistinctDates_reversesOrderAndUsesDateOnly() {
    var earlier = PositionValidationError.forPosition(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position(POSITION_ID, LocalDate.of(2026, Month.JUNE, 18), 1))
            .build(),
        EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED);
    var laterPositionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    var later = PositionValidationError.forPosition(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position(laterPositionId, LocalDate.of(2026, Month.JULY, 9), 1))
            .build(),
        EquityPositionRule.EQUITY_HOLDER_OUT_OF_RANGE);

    var items = PositionValidationError.toErrorSummaryItems(List.of(earlier, later));

    var expected = List.of(
        new ErrorSummaryItem(0, laterPositionId.toString(),
            "9 July 2026 - Each holder must have between 0% and 100% equity"),
        new ErrorSummaryItem(1, POSITION_ID.toString(),
            "18 June 2026 - The sum of all beneficial interests at a given licence position must equal 100%")
    );

    assertThat(items).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void toErrorSummaryItems_whenPositionsShareADate_theOrderDisambiguatesThePositionName() {
    var orderTwo = PositionValidationError.forPosition(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position(POSITION_ID, LocalDate.of(2026, Month.JUNE, 18), 2))
            .build(),
        EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED);
    var orderThreePositionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    var orderThree = PositionValidationError.forPosition(
        PositionValidationContextTestUtil.newBuilder()
            .withPosition(position(orderThreePositionId, LocalDate.of(2026, Month.JUNE, 18), 3))
            .build(),
        EquityPositionRule.BENEFICIAL_INTERESTS_MUST_TOTAL_ONE_HUNDRED);

    var items = PositionValidationError.toErrorSummaryItems(List.of(orderTwo, orderThree));

    var expected = List.of(
        new ErrorSummaryItem(0, orderThreePositionId.toString(),
            "18 June 2026 (3) - The sum of all beneficial interests at a given licence position must equal 100%"),
        new ErrorSummaryItem(1, POSITION_ID.toString(),
            "18 June 2026 (2) - The sum of all beneficial interests at a given licence position must equal 100%")
    );

    assertThat(items).usingRecursiveComparison().isEqualTo(expected);
  }

  private static ChronologicalPosition position(UUID id, LocalDate date, int order) {
    return new ChronologicalPosition(id, TRANSACTION_ID, date, order, List.of());
  }
}
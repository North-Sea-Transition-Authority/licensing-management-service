package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

class PositionValidationErrorTest {

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
  void forPosition_setsPositionNameWithOrderAndNoChangeOrOperation() {
    var position = ChronologicalPositionTestUtil.newBuilder()
        .withDate(LocalDate.of(2026, Month.AUGUST, 5))
        .withOrder(2)
        .build();
    var context = PositionValidationContextTestUtil.newBuilder()
        .withPosition(position)
        .withIsFirstPosition(true)
        .build();

    var error = PositionValidationError.forPosition(context, "message");

    assertThat(error.positionId()).isEqualTo(position.id());
    assertThat(error.positionName())
        .isEqualTo(DateUtil.formatLongDateWithOrder(position.date(), position.order()));
    assertThat(error.changeId()).isNull();
    assertThat(error.operationType()).isNull();
    assertThat(error.message()).isEqualTo("message");
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
}

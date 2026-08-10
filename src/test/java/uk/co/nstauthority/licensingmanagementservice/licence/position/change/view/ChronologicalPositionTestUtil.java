package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

public class ChronologicalPositionTestUtil {

  private UUID id = UUID.randomUUID();
  private LocalDate date = LocalDate.of(2026, Month.AUGUST, 5);
  private int order = 1;
  private List<PositionChange> changes = List.of();

  public static ChronologicalPositionTestUtil newBuilder() {
    return new ChronologicalPositionTestUtil();
  }

  public ChronologicalPositionTestUtil withId(UUID id) {
    this.id = id;
    return this;
  }

  public ChronologicalPositionTestUtil withDate(LocalDate date) {
    this.date = date;
    return this;
  }

  public ChronologicalPositionTestUtil withOrder(int order) {
    this.order = order;
    return this;
  }

  public ChronologicalPositionTestUtil withChanges(List<PositionChange> changes) {
    this.changes = changes;
    return this;
  }

  public ChronologicalPosition build() {
    return new ChronologicalPosition(id, date, order, changes);
  }

  public static ChronologicalPosition live(LicencePosition position, LicenceOperation... operations) {
    var changes = (operations.length == 0)
        ? List.<PositionChange>of()
        : List.of(new PositionChange(UUID.randomUUID().toString(), 1, null, List.of(operations)));

    return ChronologicalPosition.fromLicencePosition(
        position,
        position.getPositionDate(),
        position.getPositionDateOrder(),
        changes
    );
  }
}

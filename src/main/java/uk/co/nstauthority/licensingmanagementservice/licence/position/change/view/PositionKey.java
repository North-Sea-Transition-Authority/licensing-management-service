package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import java.time.LocalDate;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;

public record PositionKey(LocalDate date, int order) implements Comparable<PositionKey> {

  private static final Comparator<PositionKey> COMPARATOR = Comparator
      .comparing(PositionKey::date)
      .thenComparingInt(PositionKey::order);

  public static PositionKey from(ChronologicalPosition position) {
    return new PositionKey(position.date(), position.order());
  }

  @Override
  public int compareTo(@NotNull PositionKey other) {
    return COMPARATOR.compare(this, other);
  }
}

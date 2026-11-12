package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsCompanyListType;

/**
 * What a licence holds at one of its positions, as PEARS records it in the copy of the licence it keeps at every data
 * point. Organisations are Energy Portal organisation unit ids, and blocks and subareas are PEARS SI_IDs, which is what
 * a migrated GIS feature carries as its legacy id.
 *
 * @param administratorIds a set rather than a single id, so a copy holding none, or more than one, can be seen
 * @param licenseeIds      a set of organisation unit ids who are the licensees
 * @param blockSiIds       may hold null, for a block PEARS holds no shape for
 * @param subareas         a set of subareas for the position
 */
public record PearsPositionState(
    Set<Integer> administratorIds,
    Set<Integer> licenseeIds,
    Set<Integer> blockSiIds,
    Set<Subarea> subareas
) {

  public static final PearsPositionState EMPTY = new PearsPositionState(Set.of(), Set.of(), Set.of(), Set.of());

  static final String BLOCK = "BLOCK";

  static final String SUBAREA = "SUBAREA";

  /**
   * One row of {@code data-point-states.sql}: one thing the licence holds at one position.
   *
   * @param heldKind {@code LICENCE_ADMINISTRATOR}, {@code LICENSEE}, {@code BLOCK} or {@code SUBAREA}
   */
  public record Row(
      LocalDate positionDate,
      int positionSequence,
      String heldKind,
      @Nullable Integer organisationUnitId,
      @Nullable Integer blockSiId,
      @Nullable Integer subareaSiId,
      @Nullable String subareaShortName
  ) {
  }

  /**
   * One subarea held, by the block it divides.
   *
   * @param blockSiId null where PEARS holds no shape for the block
   * @param siId      null where PEARS holds no shape for the subarea
   */
  public record Subarea(@Nullable Integer blockSiId, @Nullable Integer siId, String shortName) {
  }

  /**
   * Whether the licence holds nothing at all here, which is how PEARS records a licence that has ended.
   */
  public boolean holdsNothing() {
    return administratorIds.isEmpty() && licenseeIds.isEmpty() && blockSiIds.isEmpty() && subareas.isEmpty();
  }

  /**
   * The rows of one licence grouped into what it holds at each position.
   */
  public static Map<PearsLicencePositions.PositionKey, PearsPositionState> byPosition(List<Row> rows) {
    var heldByPosition = new LinkedHashMap<PearsLicencePositions.PositionKey, Held>();
    for (var row : rows) {
      heldByPosition
          .computeIfAbsent(new PearsLicencePositions.PositionKey(row.positionDate(), row.positionSequence()), key -> new Held())
          .add(row);
    }

    var states = new LinkedHashMap<PearsLicencePositions.PositionKey, PearsPositionState>();
    heldByPosition.forEach((key, held) -> states.put(key, held.toState()));
    return Collections.unmodifiableMap(states);
  }

  /**
   * What one position holds, gathered row by row.
   */
  private static final class Held {

    private final Set<Integer> administratorIds = new LinkedHashSet<>();
    private final Set<Integer> licenseeIds = new LinkedHashSet<>();
    private final Set<Integer> blockSiIds = new LinkedHashSet<>();
    private final Set<Subarea> subareas = new LinkedHashSet<>();

    private void add(Row row) {
      switch (row.heldKind()) {
        case PearsCompanyListType.LICENCE_ADMINISTRATOR -> administratorIds.add(row.organisationUnitId());
        case PearsCompanyListType.LICENSEE -> licenseeIds.add(row.organisationUnitId());
        case BLOCK -> blockSiIds.add(row.blockSiId());
        case SUBAREA -> subareas.add(new Subarea(row.blockSiId(), row.subareaSiId(), row.subareaShortName()));
        default -> throw new IllegalStateException("Unexpected held kind %s".formatted(row.heldKind()));
      }
    }

    // Not Set.copyOf, which refuses the null SI_ID of a block or subarea PEARS holds no shape for.
    private PearsPositionState toState() {
      return new PearsPositionState(
          Collections.unmodifiableSet(administratorIds),
          Collections.unmodifiableSet(licenseeIds),
          Collections.unmodifiableSet(blockSiIds),
          Collections.unmodifiableSet(subareas)
      );
    }
  }
}

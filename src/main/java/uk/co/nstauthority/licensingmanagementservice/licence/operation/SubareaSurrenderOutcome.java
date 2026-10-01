package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import uk.co.fivium.grpc.gis.IntersectionStatus;

/**
 * What a block surrender did to one of the block's subareas, relative to one part of the block kept.
 *
 * @param subarea        The subarea as it was going into the surrender.
 * @param status         FULLY_INSIDE when the subarea is kept as it is, FULLY_OUTSIDE when it lies off the part kept, or
 *                       CROPPED when it is cut back to the part kept.
 * @param croppedSubarea The subarea replacing it, holding only the part kept. Only present when CROPPED.
 */
public record SubareaSurrenderOutcome(
    SubareaDetails subarea,
    IntersectionStatus status,
    @Nullable SubareaDetails croppedSubarea
) {

  private static final Set<IntersectionStatus> VALID_STATUSES = Set.of(
      IntersectionStatus.FULLY_INSIDE,
      IntersectionStatus.FULLY_OUTSIDE,
      IntersectionStatus.CROPPED
  );

  public SubareaSurrenderOutcome {
    Objects.requireNonNull(subarea, "subarea must not be null");
    if (!VALID_STATUSES.contains(status)) {
      throw new IllegalArgumentException(
          "status must be FULLY_INSIDE, FULLY_OUTSIDE or CROPPED but was %s".formatted(status));
    }
    if ((status == IntersectionStatus.CROPPED) != (croppedSubarea != null)) {
      throw new IllegalArgumentException("croppedSubarea must be present only when status is CROPPED");
    }
  }

  public static SubareaSurrenderOutcome kept(SubareaDetails subarea) {
    return new SubareaSurrenderOutcome(subarea, IntersectionStatus.FULLY_INSIDE, null);
  }

  public static SubareaSurrenderOutcome relinquished(SubareaDetails subarea) {
    return new SubareaSurrenderOutcome(subarea, IntersectionStatus.FULLY_OUTSIDE, null);
  }

  public static SubareaSurrenderOutcome cropped(
      SubareaDetails subarea,
      SubareaDetails croppedSubarea
  ) {
    return new SubareaSurrenderOutcome(subarea, IntersectionStatus.CROPPED, croppedSubarea);
  }

  /**
   * The subarea left on the part kept once surrendered, or null when the subarea lies off it.
   */
  @JsonIgnore
  @Nullable
  public SubareaDetails outputSubarea() {
    if (status == IntersectionStatus.FULLY_INSIDE) {
      return subarea;
    }
    return croppedSubarea;
  }
}

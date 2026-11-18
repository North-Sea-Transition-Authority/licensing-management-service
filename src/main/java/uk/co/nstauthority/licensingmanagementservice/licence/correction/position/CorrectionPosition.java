package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableSource;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

/**
 * A position on a correction's timeline, whether it is an executed position on the licence or one staged by the
 * correction. Both are identified by the same position id, as a staged position keeps its id once applied.
 */
public sealed interface CorrectionPosition extends Comparable<CorrectionPosition>, PathVariableSource {

  Comparator<CorrectionPosition> NATURAL_ORDER =
      Comparator.comparing(CorrectionPosition::effectiveDate)
          .thenComparing(CorrectionPosition::effectiveDateOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(CorrectionPosition::positionId);

  LicenceCorrection correction();

  UUID positionId();

  LocalDate effectiveDate();

  /** Order within the effective date, or null if the position has none. */
  Integer effectiveDateOrder();

  @Override
  default int compareTo(@NonNull CorrectionPosition o) {
    return NATURAL_ORDER.compare(this, o);
  }

  /**
   * The position a position correction stages changes against: the position it adds, or the executed position it
   * updates.
   */
  static CorrectionPosition of(LicencePositionCorrection positionCorrection) {
    return switch (positionCorrection.getChangeType()) {
      case ADD_POSITION -> new Staged(positionCorrection);
      case UPDATE_POSITION ->
          new Executed(positionCorrection.getLicenceCorrection(), positionCorrection.getTargetLicencePosition());
      case REMOVE_POSITION -> throw new IllegalStateException(
          "Licence position correction %s removes a position so cannot have changes made against it"
              .formatted(positionCorrection.getId()));
    };
  }

  @Override
  default Map<String, Object> getPathVariables() {
    return Map.of(
        CorrectionPositionArgumentResolver.CORRECTION_ID, correction().getId(),
        CorrectionPositionArgumentResolver.LICENCE_POSITION_ID, positionId()
    );
  }

  /**
   * An executed position on the correction's licence.
   */
  record Executed(LicenceCorrection correction, LicencePosition licencePosition) implements CorrectionPosition {

    @Override
    public UUID positionId() {
      return licencePosition.getId();
    }

    @Override
    public LocalDate effectiveDate() {
      return licencePosition().getPositionDate();
    }

    @Override
    public Integer effectiveDateOrder() {
      return licencePosition().getPositionDateOrder();
    }
  }

  /**
   * A position the correction adds, which is not executed until the correction is applied.
   */
  record Staged(LicencePositionCorrection licencePositionCorrection) implements CorrectionPosition {

    @Override
    public LicenceCorrection correction() {
      return licencePositionCorrection.getLicenceCorrection();
    }

    @Override
    public UUID positionId() {
      return licencePositionCorrection.getPositionId();
    }

    @Override
    public LocalDate effectiveDate() {
      return licencePositionCorrection().getPayload().effectiveDate();
    }

    @Override
    public Integer effectiveDateOrder() {
      return licencePositionCorrection().getPayload().effectiveDateOrder();
    }
  }
}

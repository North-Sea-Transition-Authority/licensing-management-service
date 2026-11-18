package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Service
public class ChangeFormService {

  private final LicencePositionChangeService licencePositionChangeService;
  private final Map<AddPositionChangeType, ChangeFormHandler<?>> handlersByChangeType;
  private final Map<Class<?>, ChangeFormHandler<?>> handlersByOperationType;

  ChangeFormService(
      LicencePositionChangeService licencePositionChangeService,
      List<ChangeFormHandler<?>> changeFormHandlers
  ) {
    this.licencePositionChangeService = licencePositionChangeService;
    this.handlersByChangeType = changeFormHandlers.stream()
        .collect(Collectors.toMap(ChangeFormHandler::changeType, Function.identity()));
    this.handlersByOperationType = changeFormHandlers.stream()
        .collect(Collectors.toMap(ChangeFormHandler::operationType, Function.identity()));
  }

  /**
   * The handler for adding a change of the given type to a position.
   *
   * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when no handler adds changes of the type, or
   *                                 {@link HttpStatus#FORBIDDEN} when the correction's licence cannot have them
   */
  public ChangeFormHandler<?> getHandler(LicenceCorrection correction, AddPositionChangeType changeType) {
    var handler = handlersByChangeType.get(changeType);

    if (handler == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No change form for %s".formatted(changeType));
    }

    return checkAvailable(correction, handler);
  }

  /**
   * The handler for correcting a live change, chosen by the first of its operations that a handler corrects.
   *
   * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when no handler corrects the change, or
   *                                 {@link HttpStatus#FORBIDDEN} when the correction's licence cannot have it
   */
  public ChangeFormHandler<?> getHandler(LicenceCorrection correction, LicencePositionChange liveChange) {
    var handler = LicencePositionChange.operationsOf(liveChange).stream()
        .map(operation -> handlersByOperationType.get(operation.getClass()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No change form for change %s".formatted(liveChange.getId())
        ));

    return checkAvailable(correction, handler);
  }

  /**
   * A position holds at most one live change of each kind with a change form, so a new one cannot be added while a
   * live one exists; the live one is corrected instead. A position the correction adds has no live changes.
   *
   * @throws ResponseStatusException with {@link HttpStatus#CONFLICT} when the position has a live change of the kind
   */
  public void checkNoLiveChangeOfType(ChangeFormHandler<?> handler, CorrectionPosition correctionPosition) {
    if (!(correctionPosition instanceof CorrectionPosition.Executed executed)) {
      return;
    }

    var licencePositionId = executed.positionId();

    if (licencePositionChangeService.changeExists(licencePositionId, handler.operationType())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Licence position %s already has a live %s".formatted(
              licencePositionId,
              handler.operationType().getSimpleName()
          )
      );
    }
  }

  private static ChangeFormHandler<?> checkAvailable(LicenceCorrection correction, ChangeFormHandler<?> handler) {
    var licenceType = correction.getLicence().getType();

    if (!handler.changeType().isAvailableFor(licenceType)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "%s is not available for %s licences".formatted(handler.changeType(), licenceType)
      );
    }

    return handler;
  }
}

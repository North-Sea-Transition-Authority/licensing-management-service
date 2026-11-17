package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public sealed interface SubareaChangeSummaryContext {

  UUID blockFeatureId();

  List<SubareaDetails> originalSubareas();

  List<SubareaTransfer> transfers();

  LicencePositionState stateBefore();

  String changeSubareasUrl();

  default List<SubareaDetails> unchangedSubareas() {
    var replacedSubareas = transfers().stream()
        .flatMap(transfer -> transfer.replacedSubareas().stream())
        .collect(Collectors.toSet());

    return originalSubareas().stream()
        .filter(subarea -> !replacedSubareas.contains(subarea))
        .toList();
  }

  record Staged(
      UUID blockFeatureId,
      List<SubareaDetails> originalSubareas,
      List<SubareaTransfer> transfers,
      LicencePositionState stateBefore,
      String changeSubareasUrl
  ) implements SubareaChangeSummaryContext {
  }

  record LiveChange(
      UUID blockFeatureId,
      List<SubareaDetails> originalSubareas,
      List<SubareaTransfer> transfers,
      LicencePositionState stateBefore,
      LicencePositionState stateAtEnd,
      String changeSubareasUrl
  ) implements SubareaChangeSummaryContext {
  }
}

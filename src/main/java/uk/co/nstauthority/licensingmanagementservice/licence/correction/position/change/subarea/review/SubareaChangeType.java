package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;

public enum SubareaChangeType {
  SPLIT("Split", TagColour.TURQUOISE),
  MERGE("Merge", TagColour.BLUE),
  RENAME("Rename", TagColour.LIGHT_BLUE),
  TRANSFER("Transfer", TagColour.BLUE),
  SET("Set", TagColour.GREEN),
  REMOVE("Remove", TagColour.RED),
  UNCHANGED("Unchanged", TagColour.GREY);

  private final String displayName;
  private final TagColour tagColour;

  SubareaChangeType(
      String displayName,
      TagColour tagColour
  ) {
    this.displayName = displayName;
    this.tagColour = tagColour;
  }

  public Tag toTag() {
    return new Tag(displayName, tagColour);
  }

  public static SubareaChangeType getStagedChangeType(
      SubareaTransfer transfer,
      List<SubareaTransfer> transfers
  ) {
    var replacedSubareas = transfer.replacedSubareas();

    if (replacedSubareas.isEmpty()) {
      return SubareaChangeType.SET;
    }

    if (replacedSubareas.size() > 1) {
      return SubareaChangeType.MERGE;
    }

    var isUsedByAnotherTransfer = transfers.stream()
        .filter(otherTransfer -> otherTransfer != transfer)
        .anyMatch(otherTransfer -> otherTransfer.replacedSubareas().contains(replacedSubareas.getFirst()));

    return transfer.outputSubareas().size() > 1 || isUsedByAnotherTransfer
        ? SubareaChangeType.SPLIT
        : SubareaChangeType.RENAME;
  }

  public static SubareaChangeType getLiveChangeType(SubareaTransfer transfer) {
    if (transfer.replacedSubareas().isEmpty()) {
      return SubareaChangeType.SET;
    }

    if (transfer.outputSubareas().isEmpty()) {
      return SubareaChangeType.REMOVE;
    }

    if (transfer.replacedSubareas().equals(transfer.outputSubareas())) {
      return SubareaChangeType.UNCHANGED;
    }

    return SubareaChangeType.TRANSFER;
  }
}

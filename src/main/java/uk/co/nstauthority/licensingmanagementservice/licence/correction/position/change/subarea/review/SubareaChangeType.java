package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;

enum SubareaChangeType {
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

  Tag toTag() {
    return new Tag(displayName, tagColour);
  }
}

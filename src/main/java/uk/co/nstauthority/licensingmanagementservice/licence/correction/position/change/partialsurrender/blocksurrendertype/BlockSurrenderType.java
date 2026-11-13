package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum BlockSurrenderType implements Displayable {
  FULL_SURRENDER(10, "Full surrender", TagColour.RED),
  PARTIAL_SURRENDER(20, "Partial surrender", TagColour.BLUE);

  private final int displayOrder;
  private final String displayName;
  private final TagColour tagColour;

  BlockSurrenderType(int displayOrder, String displayName, TagColour tagColour) {
    this.displayOrder = displayOrder;
    this.displayName = displayName;
    this.tagColour = tagColour;
  }

  public TagColour getTagColour() {
    return tagColour;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public static Map<String, String> getOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(BlockSurrenderType.class);
  }
}
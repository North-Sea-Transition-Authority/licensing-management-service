package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum BlockSurrenderType implements Displayable {
  FULL_SURRENDER(10, "Full surrender"),
  PARTIAL_SURRENDER(20, "Partial surrender");

  private final int displayOrder;
  private final String displayName;

  BlockSurrenderType(int displayOrder, String displayName) {
    this.displayOrder = displayOrder;
    this.displayName = displayName;
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
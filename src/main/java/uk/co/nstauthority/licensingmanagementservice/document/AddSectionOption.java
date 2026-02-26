package uk.co.nstauthority.licensingmanagementservice.document;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.IgnoreMaterialisationRule;

@IgnoreMaterialisationRule
public enum AddSectionOption {
  ADD_BEFORE,
  ADD_AFTER,
  ADD_SUBSECTION;

  public static int getDisplayOrder(AddSectionOption addSectionOption, int displayOrder) {
    return switch (addSectionOption) {
      case ADD_BEFORE -> displayOrder;
      case ADD_AFTER -> displayOrder + 1;
      case ADD_SUBSECTION -> 1;
    };
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Arrays;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceType implements Displayable {
  CARBON_STORAGE("Carbon storage", "CS", true),
  GAS_STORAGE("Gas storage", "GS", true),
  LANDWARD_EXPLORATION("Landward exploration", "LX", true),
  LANDWARD_PRODUCTION("Landward production", "PEDL", false),
  METHANE_DRAINAGE("Methane drainage", "MDL", true),
  SEAWARD_EXPLORATION("Seaward exploration", "E", true),
  SEAWARD_PRODUCTION("Seaward production", "P", false),
  // Unknown mappings
  A("", "A", false),
  AL("", "AL", false),
  B("", "B", false),
  CE("", "CE", false),
  DL("", "DL", false),
  NA("", "NA", false),
  XL("", "XL", false);

  private final String displayName;
  private final String prefix;
  private final Boolean managedByLms;

  LicenceType(
      String displayName,
      String prefix,
      Boolean managedByLms
  ) {
    this.displayName = displayName;
    this.prefix = prefix;
    this.managedByLms = managedByLms;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getEnumName() {
    return this.name();
  }

  public String getPrefix() {
    return prefix;
  }

  public Boolean getManagedByLms() {
    return managedByLms;
  }

  public static LicenceType getFromPrefix(String prefix) {
    return switch (prefix) {
      case "CS" -> LicenceType.CARBON_STORAGE;
      case "GS" -> LicenceType.GAS_STORAGE;
      case "LX" -> LicenceType.LANDWARD_EXPLORATION;
      case "EXL", "ML", "PEDL", "PL" -> LicenceType.LANDWARD_PRODUCTION;
      case "MDL" -> LicenceType.METHANE_DRAINAGE;
      case "E" -> LicenceType.SEAWARD_EXPLORATION;
      case "P" -> LicenceType.SEAWARD_PRODUCTION;
      case "A" -> LicenceType.A;
      case "AL" -> LicenceType.AL;
      case "B" -> LicenceType.B;
      case "CE" -> LicenceType.CE;
      case "DL" -> LicenceType.DL;
      case "NA" -> LicenceType.NA;
      case "XL" -> LicenceType.XL;
      default -> throw new RuntimeException("Invalid licence type: " + prefix);
    };
  }

  public static List<LicenceType> getLicenceTypesManagedByLms() {
    return Arrays.stream(values())
        .filter(LicenceType::getManagedByLms)
        .toList();
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence;

public enum LicenceType {
  CARBON_STORAGE(),
  GAS_STORAGE(),
  LANDWARD_EXPLORATION(),
  LANDWARD_PRODUCTION(),
  METHANE_DRAINAGE(),
  SEAWARD_EXPLORATION(),
  SEAWARD_PRODUCTION(),
  // Unknown mappings
  A(),
  AL(),
  B(),
  CE(),
  DL(),
  NA(),
  XL();

  public static LicenceType getFromPrefix(String prefix) {
    return switch (prefix) {
      case "CS" -> LicenceType.CARBON_STORAGE;
      case "GS" -> LicenceType.GAS_STORAGE;
      case "LX" -> LicenceType.LANDWARD_EXPLORATION;
      case "PEDL", "EXL", "PL", "ML" -> LicenceType.LANDWARD_PRODUCTION;
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

}

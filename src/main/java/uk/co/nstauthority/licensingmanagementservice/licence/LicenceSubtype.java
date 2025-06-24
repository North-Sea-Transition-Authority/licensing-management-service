package uk.co.nstauthority.licensingmanagementservice.licence;

public enum LicenceSubtype {
  FRONTIER(),
  INNOVATE(),
  PROMOTE(),
  TRADITIONAL();

  public static LicenceSubtype fromEpaLicenceSubtype(String string) {
    return switch (string) {
      case "frontier" -> FRONTIER;
      case "innovate" -> INNOVATE;
      case "promote" -> PROMOTE; //TODO: find out what the two promote strings are
      case "traditional" -> TRADITIONAL;
      default -> null;
    };
  }
}

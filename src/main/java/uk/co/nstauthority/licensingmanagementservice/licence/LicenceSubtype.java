package uk.co.nstauthority.licensingmanagementservice.licence;

public enum LicenceSubtype {
  FRONTIER(),
  FRONTIER_SIX_YEAR(),
  INNOVATE_PHASE_A_B(),
  INNOVATE_PHASE_C(),
  PROMOTE(),
  TRADITIONAL();

  public static LicenceSubtype fromEpaLicenceSubtype(String string) {
    return switch (string) {
      case "Frontier" -> FRONTIER;
      case "Frontier (6 year)" -> FRONTIER_SIX_YEAR;
      case "Innovate with either Phase A or Phase B or both" -> INNOVATE_PHASE_A_B;
      case "Innovate with Phase C only" -> INNOVATE_PHASE_C;
      case "Promote" -> PROMOTE;
      case "Traditional" -> TRADITIONAL;
      default -> null;
    };
  }
}

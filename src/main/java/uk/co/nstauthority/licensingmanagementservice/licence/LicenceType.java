package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceType implements Displayable {
  CARBON_STORAGE("Carbon storage", 10, "CS", "carbon-storage", true),
  GAS_STORAGE("Gas storage", 20, "GS", "gas-storage", true),
  LANDWARD_EXPLORATION("Landward exploration", 30, "LX", "landward-exploration", true),
  LANDWARD_PRODUCTION("Landward production", 40, "PEDL", "landward-production", false),
  METHANE_DRAINAGE("Methane drainage", 50, "MDL", "methane-drainage", true),
  SEAWARD_EXPLORATION("Seaward exploration", 60, "E", "seaward-exploration", true),
  SEAWARD_PRODUCTION("Seaward production", 70, "P", "seaward-production", false),
  // Unknown mappings
  A("", 80, "A", "a", false),
  AL("", 90, "AL", "al", false),
  B("", 100, "B", "b", false),
  CE("", 110, "CE", "ce", false),
  DL("", 120, "DL", "dl", false),
  NA("", 130, "NA", "na", false),
  XL("", 140, "XL", "xl", false);

  private final String displayName;
  private final int displayOrder;
  private final String prefix;
  private final String urlSlug;
  private final Boolean managedByLms;

  LicenceType(
      String displayName,
      int displayOrder,
      String prefix,
      String urlSlug,
      Boolean managedByLms
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.prefix = prefix;
    this.urlSlug = urlSlug;
    this.managedByLms = managedByLms;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public String getEnumName() {
    return this.name();
  }

  public String getPrefix() {
    return prefix;
  }

  public String getUrlSlug() {
    return urlSlug;
  }

  public Boolean isManagedByLms() {
    return managedByLms;
  }

  public Boolean isProduction() {
    return this == LANDWARD_PRODUCTION || this == SEAWARD_PRODUCTION;
  }

  public static List<LicenceType> getFromSlugListOrThrow(@NotNull String slugList) {
    return Arrays.stream(slugList.split(","))
        .map(String::trim)
        .map(LicenceType::getFromSlugOrThrow)
        .toList();
  }

  public static LicenceType getFromSlugOrThrow(@NotNull String slug) {
    return Arrays.stream(values())
        .filter(lt -> lt.getUrlSlug().equals(slug))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid licence type slug: " + slug));
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
        .filter(LicenceType::isManagedByLms)
        .toList();
  }

  public static List<LicenceType> getDisplayableTypes() {
    return Arrays.stream(values())
        .filter(lt -> !lt.getDisplayName().isEmpty())
        .toList();
  }

  /**
   * The licence types with no known display name, i.e. the unknown mappings.
   */
  public static List<LicenceType> getNonDisplayableTypes() {
    return Arrays.stream(values())
        .filter(lt -> lt.getDisplayName().isEmpty())
        .toList();
  }
}

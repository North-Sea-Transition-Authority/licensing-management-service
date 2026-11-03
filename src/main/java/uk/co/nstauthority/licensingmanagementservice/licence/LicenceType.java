package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhaseGated;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceType implements Displayable, PhaseGated {
  CARBON_STORAGE("Carbon storage", 10, "CS", "carbon-storage", true, ReleaseFeature.CURRENT_LICENCE_TYPE),
  GAS_STORAGE("Gas storage", 20, "GS", "gas-storage", true, ReleaseFeature.FUTURE_LICENCE_TYPE),
  LANDWARD_EXPLORATION("Landward exploration", 30, "LX", "landward-exploration", true, ReleaseFeature.FUTURE_LICENCE_TYPE),
  LANDWARD_PRODUCTION("Landward production", 40, "PEDL", "landward-production", false, ReleaseFeature.CURRENT_LICENCE_TYPE),
  METHANE_DRAINAGE("Methane drainage", 50, "MDL", "methane-drainage", true, ReleaseFeature.FUTURE_LICENCE_TYPE),
  SEAWARD_EXPLORATION("Seaward exploration", 60, "E", "seaward-exploration", true, ReleaseFeature.FUTURE_LICENCE_TYPE),
  SEAWARD_PRODUCTION("Seaward production", 70, "P", "seaward-production", false, ReleaseFeature.CURRENT_LICENCE_TYPE);

  private final String displayName;
  private final int displayOrder;
  private final String prefix;
  private final String urlSlug;
  private final Boolean managedByLms;
  private final ReleaseFeature releaseFeature;

  LicenceType(
      String displayName,
      int displayOrder,
      String prefix,
      String urlSlug,
      Boolean managedByLms,
      ReleaseFeature releaseFeature
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.prefix = prefix;
    this.urlSlug = urlSlug;
    this.managedByLms = managedByLms;
    this.releaseFeature = releaseFeature;
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

  @Override
  public ReleaseFeature getReleaseFeature() {
    return releaseFeature;
  }

  public boolean isProduction() {
    return this == LANDWARD_PRODUCTION || this == SEAWARD_PRODUCTION;
  }

  public boolean hasBeneficialInterests() {
    return isProduction() || this == CARBON_STORAGE;
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
      case "AL", "CE", "DL", "EXL", "ML", "PEDL", "PL" -> LicenceType.LANDWARD_PRODUCTION;
      case "MDL" -> LicenceType.METHANE_DRAINAGE;
      case "E" -> LicenceType.SEAWARD_EXPLORATION;
      case "P" -> LicenceType.SEAWARD_PRODUCTION;
      default -> throw new RuntimeException("Invalid licence type: " + prefix);
    };

  }

  public static List<LicenceType> getLicenceTypesManagedByLms() {
    return Arrays.stream(values())
        .filter(LicenceType::isManagedByLms)
        .toList();
  }
}

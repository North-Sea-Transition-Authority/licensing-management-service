package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.UNKNOWN_BLOCK;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureAttribute;

/**
 * Record of the key information that identify a subarea.
 *
 * @param featureId The feature id for the given subarea. Nullable as legacy data doesn't always have an associated feature.
 * @param name      The full name of the subarea.
 * @param shortName The short name of the subarea.
 *
 */
public record SubareaDetails(
    @Nullable UUID featureId,
    @Nullable String name,
    String shortName
) {

  public String displayName() {
    if (StringUtils.isBlank(name)) {
      return "Subarea %s".formatted(shortName);
    }
    return name.toUpperCase().startsWith("SUBAREA ") ? name : "Subarea %s".formatted(name);
  }

  public String keyForBlockFeatureId(UUID blockFeatureId) {
    var subareaReference = featureId != null ? featureId.toString() : shortName;
    return blockFeatureId + ":" + subareaReference;
  }

  public String labelForBlockName(@Nullable String blockName) {
    if (StringUtils.isBlank(blockName)) {
      return "%s - %s".formatted(UNKNOWN_BLOCK, displayName());
    }

    return "Block %s - %s".formatted(blockName, displayName());
  }

  public static SubareaDetails from(Feature feature) {
    return new SubareaDetails(
        feature.getId(),
        feature.getFeatureName(),
        feature.getAttributes().get(FeatureAttribute.NAME.name())
    );
  }

  public static Set<UUID> featureIds(Collection<SubareaDetails> subareas) {
    return subareas.stream()
        .map(SubareaDetails::featureId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public static boolean hasDuplicateNames(Collection<SubareaDetails> subareas) {
    return hasDuplicates(subareas.stream().map(SubareaDetails::name));
  }

  public static boolean hasDuplicateShortNames(Collection<SubareaDetails> subareas) {
    return hasDuplicates(subareas.stream().map(SubareaDetails::shortName));
  }

  private static boolean hasDuplicates(Stream<String> values) {
    var normalisedValues = values
        .filter(StringUtils::isNotBlank)
        .map(value -> value.trim().toLowerCase(Locale.ROOT))
        .toList();
    return normalisedValues.size() != new HashSet<>(normalisedValues).size();
  }
}

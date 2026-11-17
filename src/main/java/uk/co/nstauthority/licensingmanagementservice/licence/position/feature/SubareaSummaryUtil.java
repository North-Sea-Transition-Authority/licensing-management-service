package uk.co.nstauthority.licensingmanagementservice.licence.position.feature;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.formatting.DecimalFormatUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

public final class SubareaSummaryUtil {

  private static final String AREA = "Area";

  private SubareaSummaryUtil() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static String subareaHeading(SubareaDetails subarea) {
    return subarea.displayName();
  }

  public static SummaryCard subareaCard(
      SubareaDetails subarea,
      Map<Integer, BigDecimal> organisationIdToEquity,
      String equityLabel,
      Map<UUID, Feature> featureIdToFeature,
      Map<Integer, String> organisationIdToName
  ) {
    return SummaryCard.simpleSummaryCardWithHeading(
        subareaHeading(subarea),
        subareaDetails(subarea, organisationIdToEquity, equityLabel, featureIdToFeature, organisationIdToName).build()
    );
  }

  public static SummaryDataView.Builder subareaDetails(
      SubareaDetails subarea,
      Map<Integer, BigDecimal> organisationIdToEquity,
      String equityLabel,
      Map<UUID, Feature> featureIdToFeature,
      Map<Integer, String> organisationIdToName
  ) {
    var details = SummaryDataView.newBuilder();

    if (subarea.featureId() != null) {
      details.addStringValue(
          AREA,
          LicenceBlockFeatureUtil.formatArea(
              getFeatureOrThrow(featureIdToFeature, subarea.featureId()).getFeatureArea()
          )
      );
    }

    if (!organisationIdToEquity.isEmpty()) {
      details.addStringValue(
          equityLabel,
          organisationIdToEquity.entrySet().stream()
              .map(entry -> "%s (%s%%)".formatted(
                  organisationIdToName.getOrDefault(entry.getKey(), NOT_AVAILABLE),
                  DecimalFormatUtils.bigDecimalToFormattedString(entry.getValue())
              ))
              .sorted(String.CASE_INSENSITIVE_ORDER)
              .toList()
      );
    }

    return details;
  }

  @Nullable
  public static SummaryMapView toMapSummaryView(
      List<SubareaDetails> subareas,
      int srsWkid
  ) {
    var featureIds = subareas.stream()
        .map(SubareaDetails::featureId)
        .filter(Objects::nonNull)
        .toList();
    return featureIds.isEmpty() ? null : new SummaryMapView(featureIds, srsWkid);
  }

  public static Feature getFeatureOrThrow(
      Map<UUID, Feature> featureIdToFeature,
      UUID featureId
  ) {
    var feature = featureIdToFeature.get(featureId);
    if (feature == null) {
      throw new IllegalStateException("Subarea feature %s not found".formatted(featureId));
    }
    return feature;
  }
}

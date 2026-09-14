package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.List;
import java.util.UUID;

/**
 * A map of the given features, rendered with its textual description beside it. The features are fetched by the browser
 * from the GIS REST endpoints, so only their ids and the coordinate system they are drawn in are needed here.
 *
 * @param featureIds The features to draw.
 * @param srsWkid The well-known id of the features' coordinate system, from
 *     {@link uk.co.fivium.gisframework.feature.CoordinateSystemUtils#getWkid}.
 */
public record SummaryMapView(
    List<UUID> featureIds,
    int srsWkid
) {
}

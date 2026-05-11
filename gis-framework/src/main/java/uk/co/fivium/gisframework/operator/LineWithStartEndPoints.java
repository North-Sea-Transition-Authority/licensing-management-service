package uk.co.fivium.gisframework.operator;

import com.esri.core.geometry.Point;
import uk.co.fivium.gisframework.feature.Line;

public record LineWithStartEndPoints(
    Line line,
    Point start,
    Point end
) {
}

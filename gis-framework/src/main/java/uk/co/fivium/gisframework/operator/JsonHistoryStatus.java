package uk.co.fivium.gisframework.operator;

import java.util.List;
import uk.co.fivium.gisframework.command.TransformationType;

public record JsonHistoryStatus(
    TransformationType undoTransformationType,
    TransformationType redoTransformationType,
    List<JsonHistoryAction> actions
) {
}

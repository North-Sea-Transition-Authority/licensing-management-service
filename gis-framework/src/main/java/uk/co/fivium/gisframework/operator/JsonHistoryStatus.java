package uk.co.fivium.gisframework.operator;

import uk.co.fivium.gisframework.command.TransformationType;

public record JsonHistoryStatus(
    TransformationType undoTransformationType,
    TransformationType redoTransformationType
) {
}

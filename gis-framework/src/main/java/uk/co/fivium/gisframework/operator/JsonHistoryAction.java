package uk.co.fivium.gisframework.operator;

import java.util.List;
import uk.co.fivium.gisframework.command.TransformationType;

public record JsonHistoryAction(
    int number,
    TransformationType transformationType,
    List<String> inputFeatureNames,
    List<String> outputFeatureNames
){}
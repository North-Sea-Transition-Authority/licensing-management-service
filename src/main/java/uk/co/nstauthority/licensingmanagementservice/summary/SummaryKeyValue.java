package uk.co.nstauthority.licensingmanagementservice.summary;

import java.util.Collection;

public record SummaryKeyValue(
    String key,
    SummaryValueType summaryValueType,
    Collection<?> summaryValueData
) {
}

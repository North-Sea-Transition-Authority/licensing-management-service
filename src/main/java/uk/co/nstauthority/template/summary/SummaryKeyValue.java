package uk.co.nstauthority.template.summary;

import java.util.Collection;

public record SummaryKeyValue(
    String key,
    SummaryValueType summaryValueType,
    Collection<?> summaryValueData
) {
}

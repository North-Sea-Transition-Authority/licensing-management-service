package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import jakarta.annotation.Nullable;
import java.util.UUID;

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
}

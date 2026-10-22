package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.UUID;

/**
 * The few fields of an application's latest detail that a read-only listing needs. Selected directly by query so
 * the application and detail entities - and the long chain of eagerly mapped associations hanging off them - are
 * never loaded.
 */
public record LatestApplicationDetailSummary(
    UUID applicationId,
    UUID detailId,
    ApplicationStatus status,
    String applicationReference
) {
}

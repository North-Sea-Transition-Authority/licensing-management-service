package uk.co.nstauthority.licensingmanagementservice.testharness;


/**
 * Record holding the number of positions on a licence and whether any of those positions are linked to features.
 *
 * @param positionCount     the number of positions to give features to
 * @param hasLinkedFeatures true if any of those positions already hold features
 */
record LicencePositionFeatureSeedState(
    int positionCount,
    boolean hasLinkedFeatures
) {
}

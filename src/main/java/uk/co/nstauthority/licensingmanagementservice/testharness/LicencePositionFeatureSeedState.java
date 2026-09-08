package uk.co.nstauthority.licensingmanagementservice.testharness;


/**
 * Record holding the number of positions on a licence and whether its spatial data has already been seeded.
 *
 * @param positionCount     the number of positions the seeded features are derived across
 * @param hasSeededFeatures true if a surrender on the licence already carries output features
 */
record LicencePositionFeatureSeedState(
    int positionCount,
    boolean hasSeededFeatures
) {
}

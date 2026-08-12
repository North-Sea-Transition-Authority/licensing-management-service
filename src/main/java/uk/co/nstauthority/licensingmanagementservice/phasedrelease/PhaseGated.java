package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

/**
 * Implemented by anything whose visibility is gated by a release phase — enum options (e.g. application types) and
 * whole categories of results (e.g. work-area providers). Filtered as a collection via
 * {@link FeatureFlagService#filterEnabled(java.util.Collection)}.
 */
public interface PhaseGated {

  ReleaseFeature getReleaseFeature();
}

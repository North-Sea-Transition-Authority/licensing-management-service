package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import org.springframework.mock.env.MockEnvironment;

/**
 * Builds real {@link FeatureFlagService} instances for unit tests, so a test can pick the release phases it wants
 * without stubbing every flag check.
 */
public class FeatureFlagServiceTestUtil {

  public FeatureFlagServiceTestUtil() {
    throw new IllegalStateException("Cannot instantiate static util class");
  }

  /** Every phase on, as in the test suite and a fully enabled local run. */
  public static FeatureFlagService allPhasesEnabled() {
    return withProfiles(ReleasePhase.LMS1.getProfileName(), ReleasePhase.LMS2.getProfileName());
  }

  /** No phase profiles active, so only {@link ReleasePhase#NOT_FLAGGED} features are on, as in the initial release. */
  public static FeatureFlagService initialReleaseOnly() {
    return withProfiles();
  }

  public static FeatureFlagService withProfiles(String... activeProfiles) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(activeProfiles);
    return new FeatureFlagService(environment);
  }
}

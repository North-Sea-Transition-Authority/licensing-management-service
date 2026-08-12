package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for phased go-live gating. Wraps Spring's {@link Environment} so that every phase / feature
 * check lives in one place. There are deliberately no per-feature methods — callers pass a {@link ReleaseFeature} or
 * {@link ReleasePhase} constant.
 *
 * <p>See {@code documentation/adr/0008-phased-go-live-feature-flag.md}.</p>
 */
@Service
public class FeatureFlagService {

  private final Environment environment;

  public FeatureFlagService(Environment environment) {
    this.environment = environment;
  }

  /**
   * Phase-level check — used by the interceptor and nav filter. {@link ReleasePhase#NOT_FLAGGED} is always on.
   */
  public boolean isEnabled(ReleasePhase phase) {
    return phase == ReleasePhase.NOT_FLAGGED
        || environment.acceptsProfiles(Profiles.of(phase.getProfileName()));
  }

  /** Feature-level check — the generic entry point for actions / in-page features / options. */
  public boolean isEnabled(ReleaseFeature feature) {
    return isEnabled(feature.getReleasePhase());
  }

  /** All features currently switched on — for exposing to templates in one shot. */
  public Set<ReleaseFeature> getEnabledFeatures() {
    return Arrays.stream(ReleaseFeature.values())
        .filter(this::isEnabled)
        .collect(toUnmodifiableSet());
  }

  /** Returns only the members whose feature (hence phase) is currently on. Order is preserved. */
  public <T extends PhaseGated> List<T> filterEnabled(Collection<T> options) {
    return options.stream()
        .filter(option -> isEnabled(option.getReleaseFeature()))
        .toList();
  }
}

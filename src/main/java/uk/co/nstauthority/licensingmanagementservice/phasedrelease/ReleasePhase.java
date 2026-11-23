package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * The phased go-live releases. Each phase (other than {@link #NOT_FLAGGED}) is backed by a Spring profile; the phase is
 * "on" when its profile is active. {@code NOT_FLAGGED} is always on.
 *
 * <p>See {@code documentation/adr/0008-phased-go-live-feature-flag.md}.</p>
 */
public enum ReleasePhase {

  /** Always on — the initial live release (work area, teams, licence contacts). */
  NOT_FLAGGED(null),

  /** Schedules management, licence search and management, schedule and continuation applications, document library. */
  LMS1("enable-lms1"),

  /** All other functionality (licence corrections, licence position/timeline, etc.). */
  LMS2("enable-lms2"),

  /** Functionality not yet planned for a release (gas storage, methane drainage and exploration licence types). */
  FUTURE("enable-future"),

  /**
   * Not a go-live phase: resyncing a production licence from PEARS on demand, switched on only where the PEARS
   * datasource is configured, and only when both LMS2 and the PEARS licence resync profile are active.
   */
  PEARS_LICENCE_RESYNC(LMS2, "enable-pears-licence-resync");

  private final String[] requiresProfiles;

  ReleasePhase(String... requiresProfiles) {
    this.requiresProfiles = requiresProfiles;
  }

  ReleasePhase(ReleasePhase base, String... extraProfiles) {
    this.requiresProfiles = Stream.concat(
        Arrays.stream(base.requiresProfiles),
        Arrays.stream(extraProfiles)
    ).toArray(String[]::new);
  }

  /**
   * The Spring profile that activates this phase, or {@code null} for {@link #NOT_FLAGGED} which is always on.
   */
  public String[] getRequiredProfiles() {
    return requiresProfiles;
  }
}

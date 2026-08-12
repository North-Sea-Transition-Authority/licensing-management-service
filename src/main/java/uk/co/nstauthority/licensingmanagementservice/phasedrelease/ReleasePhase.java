package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

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
  LMS2("enable-lms2");

  private final String profileName;

  ReleasePhase(String profileName) {
    this.profileName = profileName;
  }

  /**
   * The Spring profile that activates this phase, or {@code null} for {@link #NOT_FLAGGED} which is always on.
   */
  public String getProfileName() {
    return profileName;
  }
}

package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

/**
 * Catalogue of individually toggleable actions / in-page features / options, each mapped to the {@link ReleasePhase}
 * it belongs to. Adding a new toggle means adding a constant here, not a method on {@link FeatureFlagService}.
 *
 * <p>See {@code documentation/adr/0008-phased-go-live-feature-flag.md}.</p>
 */
public enum ReleaseFeature {

  // --- Not flagged ---
  /**
   * A team role that is available from the initial release. The default for every role that is not held back by a
   * later phase — regulator and external contributor roles, plus team management and licensee contacts for industry
   * teams.
   */
  TEAM_ROLE(ReleasePhase.NOT_FLAGGED),

  // --- LMS1 ---
  /** The "Start application" button on the work area (coarse: shown when any application type is available). */
  START_APPLICATION(ReleasePhase.LMS1),
  /** The schedule (work programme) amendment application type / work-area category. */
  SCHEDULE_APPLICATION(ReleasePhase.LMS1),
  /** The continuation application type / work-area category. */
  CONTINUATION_APPLICATION(ReleasePhase.LMS1),
  /** Draft licence schedule management (schedule editing) and its work-area category. */
  MANAGE_SCHEDULE(ReleasePhase.LMS1),
  /** The schedule tab on the licence page. */
  VIEW_LICENCE_SCHEDULE(ReleasePhase.LMS1),
  /**
   * The industry team roles that grant licence and application access — application editor, application submitter and
   * licence viewer. Only team management and licensee contacts are offered for industry teams before LMS1.
   */
  INDUSTRY_LICENCE_AND_APPLICATION_ROLE(ReleasePhase.LMS1),

  // --- LMS2 ---
  /** Licence corrections and the corrections work-area category. */
  START_CORRECTION(ReleasePhase.LMS2),
  /** The timeline (licence position) tab on the licence page. */
  VIEW_LICENCE_TIMELINE(ReleasePhase.LMS2);

  private final ReleasePhase releasePhase;

  ReleaseFeature(ReleasePhase releasePhase) {
    this.releasePhase = releasePhase;
  }

  public ReleasePhase getReleasePhase() {
    return releasePhase;
  }
}

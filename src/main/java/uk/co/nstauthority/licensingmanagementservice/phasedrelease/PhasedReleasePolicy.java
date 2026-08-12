package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The default-deny allow-list, as code. Maps each controller (by package) to the {@link ReleasePhase} it belongs to;
 * a controller that is not classified returns {@link Optional#empty()} and is therefore denied by
 * {@link PhasedReleaseInterceptor}.
 *
 * <p>Classification is by controller package rather than URL pattern: URLs do not partition cleanly by phase (the
 * singular {@code licence/...} vs plural {@code licences/...} split cuts across phases, and both LMS1 and LMS2 endpoints
 * sit under {@code licences/{licenceId}/...}), and classifying by package lets {@code PhasedReleasePolicyTest} prove
 * statically that every controller resolves to a phase — the completeness guarantee that makes default-deny safe.</p>
 *
 * <p>See {@code documentation/adr/0008-phased-go-live-feature-flag.md}.</p>
 */
public final class PhasedReleasePolicy {

  private static final String BASE = "uk.co.nstauthority.licensingmanagementservice.";

  /**
   * Ordered, first match wins. More specific / differing-phase packages must precede the broad {@code licence} rule
   * (e.g. {@code licence.contact} is NOT_FLAGGED and {@code licence.correction} is LMS2, both under {@code licence}).
   */
  private static final List<Map.Entry<String, ReleasePhase>> RULES = List.of(
      // --- licence.* — order matters; specific entries before the catch-all "licence" ---
      Map.entry(BASE + "licence.contact", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "licence.correction", ReleasePhase.LMS2),
      Map.entry(BASE + "licence.position", ReleasePhase.LMS2),
      // Everything else under licence.* (search, overview, schedule, schedule-work-programme, continuation,
      // application, internal API, the base LicenceController/redirector) is LMS1.
      Map.entry(BASE + "licence", ReleasePhase.LMS1),

      // --- LMS1 (non-licence) ---
      Map.entry(BASE + "document", ReleasePhase.LMS1),

      // --- LMS2 (non-licence) ---
      // GIS split-from-map endpoint; backs the LMS2 licence-position correction / partial-surrender map editing.
      Map.entry(BASE + "gis", ReleasePhase.LMS2),

      // --- NOT_FLAGGED ---
      Map.entry(BASE + "workarea", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "teams", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "feedback", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "fds", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "file", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "mvc", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "authentication", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "energyportal", ReleasePhase.NOT_FLAGGED),

      // --- Dev / test-only controllers (each already profile-gated); NOT_FLAGGED so they are never phase-blocked ---
      Map.entry(BASE + "migration", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "mockups", ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "testharness", ReleasePhase.NOT_FLAGGED)
  );

  private PhasedReleasePolicy() {
  }

  /**
   * The phase a controller belongs to, or empty when the controller is not on the allow-list (deny).
   */
  public static Optional<ReleasePhase> phaseFor(Class<?> controllerType) {
    var packageName = controllerType.getPackageName();
    return RULES.stream()
        .filter(rule -> packageName.equals(rule.getKey()) || packageName.startsWith(rule.getKey() + "."))
        .map(Map.Entry::getValue)
        .findFirst();
  }
}

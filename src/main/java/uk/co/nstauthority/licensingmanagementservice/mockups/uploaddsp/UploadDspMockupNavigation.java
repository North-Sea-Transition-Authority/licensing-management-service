package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

// Mockup-only user switching. One NSTA user has the case at a time, so the only switch is between the NSTA and industry
@Component
@Profile("mockups")
class UploadDspMockupNavigation {

  // Public so Freemarker can read them
  public record Link(String label, String url) {
  }

  public record Navigation(String current, List<Link> others) {
  }

  private enum Actor {
    CASE_MANAGER,
    DECISION_ISSUER,
    EXECUTOR,
    SIGNATORY,
    LICENSEE
  }

  private record Current(Actor actor, int licensee) {
  }

  private static final String BASE = "/mockups/upload-dsp";
  private static final Pattern LICENSEE_PAGE = Pattern.compile("^/(?:industry|deed-of-variation/signed)/(\\d+)$");

  private final UploadDspMockupJourneyStore journeyStore;

  UploadDspMockupNavigation(UploadDspMockupJourneyStore journeyStore) {
    this.journeyStore = journeyStore;
  }

  Navigation forPath(String path) {
    var current = currentActor(path.substring(BASE.length()));
    var journey = journeyStore.journey();
    var others = new ArrayList<Link>();

    if (current.actor() == Actor.LICENSEE) {
      others.add(regulatorLink(regulatorWithCase(journey), journey));
      for (var i = 0; i < UploadDspMockupJourney.LICENSEES.size(); i++) {
        if (i != current.licensee()) {
          others.add(licenseeLink(i));
        }
      }
    } else {
      // Industry has no page of its own at this point: accepting is the only thing it does
      if (journey.isDecisionLetterIssued() && !journey.isDecisionAccepted()) {
        others.add(new Link("Industry: accept decision", route(controller().industryAcceptDecision())));
      }
      if (journey.deedStatus() != UploadDspMockupJourney.DeedStatus.NOT_STARTED) {
        for (var i = 0; i < UploadDspMockupJourney.LICENSEES.size(); i++) {
          others.add(licenseeLink(i));
        }
      }
    }
    var currentLabel = current.actor() == Actor.LICENSEE
        ? licenseeLabel(current.licensee())
        : regulatorLabel(current.actor(), journey);
    return new Navigation(currentLabel, others);
  }

  // The NSTA user who has the case now, which is where industry switches back to
  private Actor regulatorWithCase(UploadDspMockupJourney journey) {
    if (journey.isDecisionAccepted()) {
      return journey.signatureStatus() == UploadDspMockupJourney.SignatureStatus.AWAITING
          ? Actor.SIGNATORY
          : Actor.EXECUTOR;
    }
    if (journey.isDecisionLetterIssued()) {
      return Actor.CASE_MANAGER;
    }
    return journey.isWithDecisionIssuer() ? Actor.DECISION_ISSUER : Actor.CASE_MANAGER;
  }

  private Link regulatorLink(Actor actor, UploadDspMockupJourney journey) {
    var url = switch (actor) {
      case DECISION_ISSUER -> route(controller().renderOverviewDecisionIssuer(null));
      case EXECUTOR -> route(controller().renderOverviewExecution(null));
      case SIGNATORY -> route(controller().renderOverviewSignatory(null));
      default -> caseManagerUrl(journey);
    };
    return new Link(regulatorLabel(actor, journey), url);
  }

  private String regulatorLabel(Actor actor, UploadDspMockupJourney journey) {
    return switch (actor) {
      case DECISION_ISSUER -> "Decision issuer";
      case EXECUTOR -> "Decision executor";
      case SIGNATORY -> journey.signatoryName() == null
          ? "Authorised signatory"
          : "Authorised signatory (" + journey.signatoryName() + ")";
      default -> "Case manager";
    };
  }

  private Link licenseeLink(int licensee) {
    return new Link(licenseeLabel(licensee), route(controller().renderOverviewIndustry(licensee, null)));
  }

  private String licenseeLabel(int licensee) {
    return "Licensee: " + UploadDspMockupJourney.LICENSEES.get(licensee).name();
  }

  private Current currentActor(String route) {
    var licenseePage = LICENSEE_PAGE.matcher(route);
    if (licenseePage.matches()) {
      var licensee = Integer.parseInt(licenseePage.group(1));
      return new Current(Actor.LICENSEE, licensee < UploadDspMockupJourney.LICENSEES.size() ? licensee : 0);
    }
    if (route.equals("/signatory") || route.equals("/direction-letter/sign")
        || route.equals("/direction-letter/send-back")) {
      return new Current(Actor.SIGNATORY, 0);
    }
    if (route.equals("/execution") || route.startsWith("/deed-of-variation") || route.startsWith("/direction-letter")) {
      return new Current(Actor.EXECUTOR, 0);
    }
    if (route.equals("/decision-issuer") || route.equals("/issue-decision")) {
      return new Current(Actor.DECISION_ISSUER, 0);
    }
    return new Current(Actor.CASE_MANAGER, 0);
  }

  // The case manager's page depends on how far the decision has got. The start page would reset the journey
  private String caseManagerUrl(UploadDspMockupJourney journey) {
    if (!journey.hasDspUploaded()) {
      return route(controller().renderOverview(null));
    }
    if (journey.isDecisionLetterIssued()) {
      return route(controller().renderOverviewDecisionIssued(null));
    }
    return journey.isSentBack()
        ? route(controller().renderOverviewSentBack(null))
        : route(controller().renderOverviewDspUploaded(null));
  }

  private UploadDspMockupController controller() {
    return on(UploadDspMockupController.class);
  }

  private String route(Object methodCall) {
    return ReverseRouter.route(methodCall);
  }
}

package uk.co.nstauthority.licensingmanagementservice.mockups.decisionjourney.licenceextension;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/mockups/licence-extension")
@Profile("mockups")
public class LicenceExtensionMockupController {

  public static final List<MockTerm> TERMS = List.of(
      new MockTerm("phaseA", "Phase A (Initial term)", "17 May 2025", true),
      new MockTerm("phaseB", "Phase B (Initial term)", "17 June 2025", true),
      new MockTerm("phaseC", "Phase C (Initial term)", "17 July 2025", true),
      new MockTerm("secondTerm", "Second term", "17 July 2026", true),
      new MockTerm("thirdTerm", "Third term", "17 July 2030", false)
  );

  @GetMapping("/extension-only")
  ModelAndView renderExtensionOnlyMockup() {
    return new ModelAndView("lms/mockups/licenceextension/extensionOnly")
        .addObject("form", new LicenceExtensionMockupForm())
        .addObject("terms", TERMS)
        .addObject("pageTitle", "Extension details");
  }

  @PostMapping("/extension-only")
  RedirectView submitExtensionOnlyMockup() {
    return new RedirectView("/lms/mockups/decision-journey");
  }

  @GetMapping("/proportional-reduction")
  ModelAndView renderProportionalReductionMockup() {
    return new ModelAndView("lms/mockups/licenceextension/proportionalReduction")
        .addObject("form", new LicenceExtensionMockupForm())
        .addObject("terms", TERMS)
        .addObject("pageTitle", "Extension and Proportional Reduction");
  }

  @GetMapping("/reduction-only")
  ModelAndView renderReductionOnlyMockup() {
    return new ModelAndView("lms/mockups/licenceextension/reductionOnly")
        .addObject("form", new LicenceExtensionMockupForm())
        .addObject("terms", TERMS)
        .addObject("pageTitle", "Proportional reduction details");
  }

  @PostMapping("/reduction-only")
  RedirectView submitReductionOnlyMockup() {
    return new RedirectView("/lms/mockups/decision-journey");
  }

  @PostMapping("/proportional-reduction")
  RedirectView submitProportionalReductionMockup() {
    return new RedirectView("/lms/mockups/decision-journey");
  }

  public record MockTerm(String id, String name, String endDate, boolean canExtend) {}
}

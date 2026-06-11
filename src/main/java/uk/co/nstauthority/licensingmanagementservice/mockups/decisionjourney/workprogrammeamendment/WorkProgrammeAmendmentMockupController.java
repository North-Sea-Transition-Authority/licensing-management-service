package uk.co.nstauthority.licensingmanagementservice.mockups.decisionjourney.workprogrammeamendment;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTypeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;

@Controller
@RequestMapping("/mockups/work-programme-amendment")
@Profile("mockups")
public class WorkProgrammeAmendmentMockupController {

  static final String SESSION_KEY = "wpAmendmentDecisions";

  public static final List<MockWorkProgramme> WORK_PROGRAMMES = List.of(
      new MockWorkProgramme("wp1", "Drill well to 3,000m"),
      new MockWorkProgramme("wp2", "Acquire 3D seismic data"),
      new MockWorkProgramme("wp3", "Geological evaluation of Block A"),
      new MockWorkProgramme("wp4", "Drill a third appraisal well")
  );

  public static final List<MockWorkProgramme> ADDITIONAL_WORK_PROGRAMMES = List.of(
      new MockWorkProgramme("wpx1", "Complete geophysical survey of Block C"),
      new MockWorkProgramme("wpx2", "Plug and abandon legacy well"),
      new MockWorkProgramme("wpx3", "Submit updated field development plan")
  );

  static final List<MockWorkProgramme> ALL_WORK_PROGRAMMES =
      Stream.concat(WORK_PROGRAMMES.stream(), ADDITIONAL_WORK_PROGRAMMES.stream()).toList();

  @GetMapping
  ModelAndView renderSelect(HttpSession session) {
    var decisions = getDecisions(session);
    var addedIds = decisions.stream().map(MockWorkProgrammeDecision::getId).toList();
    var available = ALL_WORK_PROGRAMMES.stream()
        .filter(wp -> !addedIds.contains(wp.getId()))
        .toList();

    return new ModelAndView("lms/mockups/workprogrammeamendment/selectWorkProgramme")
        .addObject("form", new WorkProgrammeSelectMockupForm())
        .addObject("workProgrammes", available)
        .addObject("pageTitle", "Which work programme activity forms part of the decision?");
  }

  @PostMapping
  RedirectView submitSelect(WorkProgrammeSelectMockupForm form) {
    var wpId = form.getSelectedWorkProgrammeId();
    if (wpId == null || wpId.isBlank()) {
      return new RedirectView("/lms/mockups/work-programme-amendment");
    }
    return new RedirectView("/lms/mockups/work-programme-amendment/" + wpId + "/impact");
  }

  @GetMapping("/{wpId}/impact")
  ModelAndView renderImpact(@PathVariable String wpId) {
    var wp = ALL_WORK_PROGRAMMES.stream()
        .filter(w -> w.getId().equals(wpId))
        .findFirst()
        .orElse(new MockWorkProgramme(wpId, wpId));

    var licenceTypeSlugList = LicenceTypeUtil.getUrlSlugList(List.of(
        LicenceType.LANDWARD_PRODUCTION,
        LicenceType.SEAWARD_PRODUCTION
    ));

    return new ModelAndView("lms/mockups/workprogrammeamendment/workProgrammeImpact")
        .addObject("form", new WorkProgrammeImpactMockupForm())
        .addObject("workProgramme", wp)
        .addObject("transferLicences", List.of())
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchActiveLicenceSchedulesByReferenceAndType(
                    licenceTypeSlugList, null, null)))
        .addObject("pageTitle", "What is the decision in relation to this activity?");
  }

  @PostMapping("/{wpId}/impact")
  RedirectView submitImpact(
      @PathVariable String wpId,
      WorkProgrammeImpactMockupForm form,
      HttpSession session) {

    var wp = ALL_WORK_PROGRAMMES.stream()
        .filter(w -> w.getId().equals(wpId))
        .findFirst()
        .orElse(new MockWorkProgramme(wpId, wpId));

    var actionLabel = switch (form.getAction() == null ? "" : form.getAction()) {
      case "amend" -> "Amend deadline or text";
      case "waive" -> "Waive";
      case "transfer" -> "Transferred to another licence";
      case "delay" -> "Delay / No further action";
      default -> "Unknown";
    };

    var decisions = getDecisions(session);
    decisions.add(new MockWorkProgrammeDecision(wp.getId(), wp.getDescription(), actionLabel));
    session.setAttribute(SESSION_KEY, decisions);

    return new RedirectView("/lms/mockups/work-programme-amendment/summary");
  }

  @GetMapping("/summary")
  ModelAndView renderSummary(HttpSession session) {
    var decisions = getDecisions(session);
    if (decisions.isEmpty()) {
      return new ModelAndView(new RedirectView("/lms/mockups/work-programme-amendment"));
    }

    return new ModelAndView("lms/mockups/workprogrammeamendment/workProgrammeSummary")
        .addObject("form", new WorkProgrammeSummaryMockupForm())
        .addObject("decisions", decisions)
        .addObject("pageTitle", "Work programme amendments");
  }

  @PostMapping("/summary")
  RedirectView submitSummary(WorkProgrammeSummaryMockupForm form, HttpSession session) {
    if ("yes".equals(form.getAddAnotherOption())) {
      return new RedirectView("/lms/mockups/work-programme-amendment");
    }
    session.removeAttribute(SESSION_KEY);
    return new RedirectView("/lms/mockups/decision-journey");
  }

  @SuppressWarnings("unchecked")
  private List<MockWorkProgrammeDecision> getDecisions(HttpSession session) {
    var existing = session.getAttribute(SESSION_KEY);
    if (existing instanceof List<?> list) {
      return new ArrayList<>((List<MockWorkProgrammeDecision>) list);
    }
    return new ArrayList<>();
  }

  public static class MockWorkProgramme {
    private final String id;
    private final String description;

    public MockWorkProgramme(String id, String description) {
      this.id = id;
      this.description = description;
    }

    public String getId() {
      return id;
    }

    public String getDescription() {
      return description;
    }
  }

  public static class MockWorkProgrammeDecision implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String description;
    private final String action;

    public MockWorkProgrammeDecision(String id, String description, String action) {
      this.id = id;
      this.description = description;
      this.action = action;
    }

    public String getId() {
      return id;
    }

    public String getDescription() {
      return description;
    }

    public String getAction() {
      return action;
    }
  }
}

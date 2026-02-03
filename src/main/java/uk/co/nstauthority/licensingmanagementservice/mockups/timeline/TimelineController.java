package uk.co.nstauthority.licensingmanagementservice.mockups.timeline;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/timeline")
@Profile("internal-only")
public class TimelineController {

  @GetMapping("/production")
  ModelAndView showTimeline() {
    return new ModelAndView("lms/mockups/timeline/timelineDesign")
        .addObject("form", new TimelineForm()).addObject("options", TimelineFilterOptions.getFilterOptions());
  }

  @GetMapping("/production/view-only")
  ModelAndView showViewOnlyTimeline() {
    return new ModelAndView("lms/mockups/timeline/timelineDesignViewOnly")
        .addObject("form", new TimelineForm()).addObject("options", TimelineFilterOptions.getFilterOptions());
  }

  @GetMapping("/carbon-storage")
  ModelAndView showCsTimeline() {
    return new ModelAndView("lms/mockups/timeline/timelineDesignCs")
        .addObject("form", new TimelineForm()).addObject("options", TimelineFilterOptions.getFilterOptions());
  }

  @GetMapping("/carbon-storage/view-only")
  ModelAndView showViewOnlyCsTimeline() {
    return new ModelAndView("lms/mockups/timeline/timelineDesignCsViewOnly")
        .addObject("form", new TimelineForm()).addObject("options", TimelineFilterOptions.getFilterOptions());
  }
}

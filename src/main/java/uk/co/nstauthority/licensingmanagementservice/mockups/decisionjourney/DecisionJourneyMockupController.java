package uk.co.nstauthority.licensingmanagementservice.mockups.decisionjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;

@Controller
@RequestMapping("/mockups/decision-journey")
@Profile("mockups")
public class DecisionJourneyMockupController {

  @GetMapping
  ModelAndView renderTaskList() {
    var summaryItem = getSignDspSummaryItem();
    return new ModelAndView("lms/mockups/decisionjourney/taskList")
        .addObject("pageTitle", "Record of Decision")
        .addObject("summaryItem", summaryItem);
  }

  @NotNull
  private static SummaryItem getSignDspSummaryItem() {
    var uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setName("signed-dsp.pdf");

    var dspFile = SummaryFileView.newFromUploadedFile(
        String.valueOf(UUID.randomUUID()),
        uploadedFile,
        ReverseRouter.route(on(LicenceScheduleSupportingInformationController.class).downloadFile(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null
        ))
    );

    return SummaryItem.withCards(null,
        List.of(SummaryCard.filesSummaryCardWithHeading("Signed DSP", List.of(dspFile))));
  }

  @GetMapping("/record-decision")
  ModelAndView renderRecordDecision() {
    return new ModelAndView("lms/mockups/decisionjourney/recordDecision")
        .addObject("form", new DecisionJourneyMockupForm())
        .addObject("pageTitle", "What is the decision?");
  }

  @PostMapping("/record-decision")
  RedirectView submitRecordDecision() {
    return new RedirectView("/lms/mockups/decision-journey");
  }

  @GetMapping("/record-decision2")
  ModelAndView renderRecordDecision2() {
    return new ModelAndView("lms/mockups/decisionjourney/recordDecision2")
        .addObject("form", new DecisionJourneyMockupForm())
        .addObject("pageTitle", "What is the decision?");
  }
}

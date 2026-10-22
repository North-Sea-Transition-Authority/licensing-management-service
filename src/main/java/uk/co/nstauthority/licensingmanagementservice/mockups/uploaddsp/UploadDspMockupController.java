package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.file.FileControllerHelperService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationContext;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;

@Controller
@RequestMapping("/mockups/upload-dsp")
@Profile("mockups")
public class UploadDspMockupController {

  private static final String APPLICATION_REFERENCE = "P1234/5678";
  private static final String CAPTION = "Offshore";
  private static final String JOURNEY = "uploadDspMockupJourney";
  private static final String ACTION_CORRECT = "CORRECT";
  private static final String ACTION_ADDENDUM = "ADDENDUM";
  private static final String ADDENDUM_TARGET_PREFIX = "ADDENDUM_";
  private static final String OVERVIEW_VIEW =
      "lms/licence/scheduleWorkProgrammeApplication/scheduleWorkProgrammeApplicationOverview";

  private static final ScheduleWorkProgrammeApplicationContext APPLICATION_CONTEXT =
      new ScheduleWorkProgrammeApplicationContext(
          APPLICATION_REFERENCE,
          "P1234 - Traditional",
          List.of(SummaryDataView.newBuilder()
              .addStringValue("Submitted by", "John Doe")
              .addStringValue("Submission date", "12 Apr 2026 14:30")
              .addStringValue("Steward", "Jane Smith")
              .build())
      );

  private static final List<SummarySection> SUMMARY_SECTIONS = List.of(
      new SummarySection(10, List.of(
          SummaryItem.withCard("Licence",
              SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                  .addStringValue("Licence", "P1234")
                  .addStringValue("Round", "32nd")
                  .addStringValue("Blocks", "21/1a, 21/1b")
                  .addStringValue("Prospect / discovery / field", "Brent")
                  .addStringValue("Licensees", "Shell UK limited, BP Exploration Operating Company Limited")
                  .build()))
      )),
      new SummarySection(20, List.of(
          SummaryItem.withCard("Request summary",
              SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                  .addStringValue("Type of request", List.of("Extension", "Work programme amendment"))
                  .addStringValue("Date of request", "10 Apr 2026")
                  .addStringValue("Requesting licensee", "Shell UK limited")
                  .addStringValue("Made on behalf of all licensees", "Yes")
                  .build()))
      ))
  );

  // Ordered, because the select shows options in map order
  private static final Map<String, String> STEWARD_OPTIONS = Stream.of(
          Map.entry("1", "Jane Smith"),
          Map.entry("2", "Chris Morgan"),
          Map.entry("3", "Priya Shah"))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));

  private enum OverviewView {
    INITIAL,
    SENT_BACK,
    DSP_UPLOADED,
    DECISION_ISSUER,
    EXECUTION
  }

  private final FileControllerHelperService fileControllerHelperService;
  private final HttpSession session;

  public UploadDspMockupController(FileControllerHelperService fileControllerHelperService, HttpSession session) {
    this.fileControllerHelperService = fileControllerHelperService;
    this.session = session;
  }

  // Opening the SUBMITTED overview starts the mockup journey afresh
  @GetMapping
  ModelAndView renderOverview(@RequestParam(name = "tab", defaultValue = "overview") OverviewTab tab) {
    resetJourney();
    return buildOverview(OverviewView.INITIAL, tab);
  }

  // Same actions as the start page, but the journey state carries on instead of being cleared
  @GetMapping("/sent-back")
  ModelAndView renderOverviewSentBack(@RequestParam(name = "tab", defaultValue = "overview") OverviewTab tab) {
    return buildOverview(OverviewView.SENT_BACK, tab);
  }

  @GetMapping("/dsp-uploaded")
  ModelAndView renderOverviewDspUploaded(@RequestParam(name = "tab", defaultValue = "overview") OverviewTab tab) {
    return buildOverview(OverviewView.DSP_UPLOADED, tab);
  }

  // The decision issuer sees the application summary with their own actions
  @GetMapping("/decision-issuer")
  ModelAndView renderOverviewDecisionIssuer(@RequestParam(name = "tab", defaultValue = "overview") OverviewTab tab) {
    return buildOverview(OverviewView.DECISION_ISSUER, tab);
  }

  // Once the decision letter has been approved and issued
  @GetMapping("/execution")
  ModelAndView renderOverviewExecution(@RequestParam(name = "tab", defaultValue = "overview") OverviewTab tab) {
    return buildOverview(OverviewView.EXECUTION, tab);
  }

  private ModelAndView buildOverview(OverviewView view, OverviewTab requestedTab) {
    var selectedTab = requestedTab == OverviewTab.CASE_EVENTS ? OverviewTab.CASE_EVENTS : OverviewTab.OVERVIEW;
    var modelAndView = new ModelAndView(OVERVIEW_VIEW)
        .addObject("availableTabs", List.of(OverviewTab.OVERVIEW, OverviewTab.CASE_EVENTS))
        .addObject("selectedTab", selectedTab)
        .addObject("controllerUrl", overviewUrl(view));

    if (selectedTab == OverviewTab.CASE_EVENTS) {
      return modelAndView.addObject("caseEventsSummaryItem",
          SummaryItem.withCards(OverviewTab.CASE_EVENTS.label(), getCaseEventCards()));
    }
    return modelAndView
        .addObject("applicationContext", APPLICATION_CONTEXT)
        .addObject("summarySections", SUMMARY_SECTIONS)
        .addObject("accordionId", UUID.randomUUID().toString())
        .addObject("applicationActions", getApplicationActions(view));
  }

  private List<ActionItemView> getApplicationActions(OverviewView view) {
    return switch (view) {
      case EXECUTION -> List.of(
          new ActionItemView("Send back to recommendation", 10, false,
              ReverseRouter.route(on(this.getClass()).sendBackToRecommendation()), null)
      );
      case DECISION_ISSUER -> List.of(
          new ActionItemView("Decision letter", 10, true,
              ReverseRouter.route(on(this.getClass()).renderDecisionLetter()), null),
          new ActionItemView("Send back to record decision", 20, false,
              ReverseRouter.route(on(this.getClass()).sendBackToRecordDecision()), null)
      );
      case INITIAL, SENT_BACK -> {
        var sentBack = view == OverviewView.SENT_BACK;
        yield List.of(
            new ActionItemView("Allocate steward", 10, false,
                ReverseRouter.route(on(this.getClass()).renderAllocateSteward(sentBack)), null),
            new ActionItemView("Upload DSP", 20, true,
                ReverseRouter.route(on(this.getClass()).renderUploadDsp(sentBack)), null)
        );
      }
      case DSP_UPLOADED -> {
        var journey = journey();
        var uploadDspAction = new ActionItemView("Upload DSP", 20, false,
            ReverseRouter.route(on(this.getClass()).renderUploadDsp(true)), null);
        if (!journey.isDecisionRecorded()) {
          yield List.of(uploadDspAction, new ActionItemView("Record a decision", 30, true,
              ReverseRouter.route(on(this.getClass()).startRecordDecision()), null));
        }
        var dspChanged = journey.hasDspChangedSinceDecisionRecorded();
        var correctDecisionAction = new ActionItemView("Correct decision record", 30, dspChanged,
            ReverseRouter.route(on(this.getClass()).renderRecordOfDecision()), null);
        // A changed DSP must be dealt with before the case can go back to the issuer
        yield dspChanged
            ? List.of(uploadDspAction, correctDecisionAction)
            : List.of(uploadDspAction, correctDecisionAction,
                new ActionItemView("Send back to issue decision", 40, false,
                    ReverseRouter.route(on(this.getClass()).sendBackToIssueDecision()), null));
      }
    };
  }

  private String overviewUrl(OverviewView view) {
    return switch (view) {
      case INITIAL -> ReverseRouter.route(on(this.getClass()).renderOverview(null));
      case SENT_BACK -> ReverseRouter.route(on(this.getClass()).renderOverviewSentBack(null));
      case DSP_UPLOADED -> ReverseRouter.route(on(this.getClass()).renderOverviewDspUploaded(null));
      case DECISION_ISSUER -> ReverseRouter.route(on(this.getClass()).renderOverviewDecisionIssuer(null));
      case EXECUTION -> ReverseRouter.route(on(this.getClass()).renderOverviewExecution(null));
    };
  }

  // Where Upload DSP's back link and cancel should return to
  private OverviewView caseManagerView(boolean dspUploaded) {
    if (!dspUploaded) {
      return OverviewView.INITIAL;
    }
    return journey().isSentBack() ? OverviewView.SENT_BACK : OverviewView.DSP_UPLOADED;
  }

  // Newest first, like the real case events tab
  private List<SummaryCard> getCaseEventCards() {
    var cards = new ArrayList<SummaryCard>();
    for (var event : journey().eventsNewestFirst()) {
      cards.add(caseEventCard(event.heading(), Map.of(event.detailKey(), event.detailValue()), "Jane Smith",
          event.at()));
    }
    cards.add(caseEventCard("Steward allocated", Map.of("Steward", "Jane Smith"), "Alex Taylor",
        Instant.parse("2026-04-13T09:15:00Z")));
    cards.add(caseEventCard("Application submitted", Map.of(), "John Doe", Instant.parse("2026-04-12T13:30:00Z")));
    return cards;
  }

  private SummaryCard caseEventCard(String heading, Map<String, String> details, String eventBy, Instant eventInstant) {
    var summaryData = SummaryDataView.newBuilder();
    details.forEach(summaryData::addStringValue);
    summaryData
        .addStringValue("Event by", eventBy)
        .addStringValue("Date", DateFormatUtil.convertToDisplayTextWithTime(eventInstant));
    return SummaryCard.simpleSummaryCardWithHeading(heading, summaryData.build());
  }

  // Snapshots the DSP at the start of the decision so later changes can be flagged
  @GetMapping("/record-decision")
  ModelAndView startRecordDecision() {
    updateJourney(UploadDspMockupJourney::startDecision);
    return ReverseRouter.redirect(on(this.getClass()).renderRecordOfDecision());
  }

  @GetMapping("/record-of-decision")
  ModelAndView renderRecordOfDecision() {
    var journey = journey();
    var dspCards = new ArrayList<SummaryCard>();
    dspCards.add(SummaryCard.filesSummaryCardWithHeading("Signed DSP",
        List.of(fileView(journey.currentDspFileName()))));
    if (journey.addendumCount() > 0) {
      dspCards.add(SummaryCard.filesSummaryCardWithHeading("DSP addendums",
          journey.addendumFileNames().stream().map(this::fileView).toList()));
    }

    var taskListSections = List.of(
        new TaskListSection("Record of decision", 10, List.of(
            new TaskListItem("What is the decision?", "/mockups/decision-journey/record-decision"),
            new TaskListItem("Extension decision details", "/mockups/licence-extension/extension-only"),
            new TaskListItem("Corresponding reduction details", "/mockups/licence-extension/reduction-only"),
            new TaskListItem("Work programme amendment details", "/mockups/work-programme-amendment"),
            decisionDetailsTaskListItem(journey)
        )),
        new TaskListSection("Review", 20, List.of(new TaskListItem("Review record of decision",
            ReverseRouter.route(on(this.getClass()).renderReview()))))
    );

    var viewApplicationUrl = overviewUrl(caseManagerView(true));
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/recordOfDecision")
        .addObject("pageTitle", "Record of Decision")
        .addObject("applicationContext", APPLICATION_CONTEXT)
        .addObject("viewApplicationUrl", viewApplicationUrl)
        .addObject("signedDspSummaryItem", SummaryItem.withCards(null, dspCards))
        .addObject("dspChanges", journey.changesSinceDecisionStarted())
        .addObject("taskListSections", taskListSections);

    var breadcrumbs = Breadcrumbs.builder("Record of Decision")
        .addWorkAreaBreadcrumb()
        .addBreadcrumb(APPLICATION_REFERENCE, viewApplicationUrl)
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @GetMapping("/record-of-decision/decision-details")
  ModelAndView renderDecisionDetails() {
    var form = journey().decisionDetails().orElseGet(DecisionDetailsMockupForm::new);
    var preselectedConsultees = form.getRaciConsulteeIds().stream()
        .map(id -> new DecisionDetailsPerson(id, DecisionDetailsMockupData.consulteeName(id)))
        .toList();

    return new ModelAndView("lms/mockups/uploaddsp/decisionDetails")
        .addObject("pageTitle", "Decision details")
        .addObject("caption", CAPTION)
        .addObject("form", form)
        .addObject("decisionMakerOptions", DecisionDetailsMockupData.DECISION_MAKER_OPTIONS)
        .addObject("preselectedConsultees", preselectedConsultees)
        .addObject("consulteesUrl", SearchSelectorService.route(
            on(DecisionDetailsMockupRestController.class).searchRaciConsultees(null)))
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderRecordOfDecision()));
  }

  @PostMapping("/record-of-decision/decision-details")
  ModelAndView saveDecisionDetails(@ModelAttribute("form") DecisionDetailsMockupForm form) {
    updateJourney(journey -> journey.saveDecisionDetails(form));
    return ReverseRouter.redirect(on(this.getClass()).renderRecordOfDecision());
  }

  @GetMapping("/record-of-decision/review")
  ModelAndView renderReview() {
    var taskListUrl = ReverseRouter.route(on(this.getClass()).renderRecordOfDecision());
    var summarySections = new ArrayList<SummarySection>();
    summarySections.add(new SummarySection(10, List.of(
        SummaryItem.withCard("Decision",
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Extension decision", "Granted")
                .addStringValue("Work programme amendment decision", "Granted")
                .build()))
    )));
    journey().decisionDetails().ifPresent(details -> summarySections.add(new SummarySection(20, List.of(
        SummaryItem.withCard("Decision details", SummaryCard.simpleSummaryCard(decisionDetailsSummary(details)))
    ))));

    var modelAndView = new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/reviewRecordOfDecision")
        .addObject("pageTitle", "Review record of decision")
        .addObject("pageCaption", CAPTION)
        .addObject("summarySections", summarySections)
        .addObject("accordionId", UUID.randomUUID().toString())
        .addObject("isSubmittable", true)
        .addObject("cancelUrl", taskListUrl);

    var breadcrumbs = Breadcrumbs.builder("Review record of decision")
        .addWorkAreaBreadcrumb()
        .addTaskListBreadcrumb(taskListUrl)
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  // The real screen redirects to the work area; the mockup returns to the overview to show the next state.
  @PostMapping("/record-of-decision/review")
  ModelAndView submitReview(RedirectAttributes redirectAttributes) {
    updateJourney(UploadDspMockupJourney::recordDecision);
    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Record of decision submitted for " + APPLICATION_REFERENCE)
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDecisionIssuer(null));
  }

  @GetMapping("/issue-decision")
  ModelAndView renderDecisionLetter() {
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/issueDecision")
        .addObject("pageTitle", "Decision letter for Shell UK limited")
        .addObject("approveUrl", ReverseRouter.route(on(this.getClass()).approveAndIssueLetter()));

    var breadcrumbs = Breadcrumbs.builder("Decision letter")
        .addWorkAreaBreadcrumb()
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @PostMapping("/issue-decision")
  ModelAndView approveAndIssueLetter() {
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewExecution(null));
  }

  // Execution stage: goes further back than the issuer's send back, to the Upload DSP stage
  @GetMapping("/send-back-to-recommendation")
  ModelAndView sendBackToRecommendation() {
    updateJourney(journey -> journey.setSentBack(true));
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewSentBack(null));
  }

  // Undoes a send back made by mistake: returns the case to the decision issuer
  @GetMapping("/send-back-to-issue-decision")
  ModelAndView sendBackToIssueDecision() {
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDecisionIssuer(null));
  }

  @GetMapping("/send-back-to-record-decision")
  ModelAndView sendBackToRecordDecision() {
    updateJourney(journey -> journey.setSentBack(false));
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDspUploaded(null));
  }

  // Stub so the Allocate steward action isn't a dead end; the selection isn't kept
  @GetMapping("/allocate-steward")
  ModelAndView renderAllocateSteward(@RequestParam(defaultValue = "false") boolean sentBack) {
    return new ModelAndView("lms/mockups/uploaddsp/allocateSteward")
        .addObject("form", new AllocateStewardMockupForm())
        .addObject("pageTitle", "Who is the steward for this application?")
        .addObject("caption", CAPTION)
        .addObject("stewardOptions", STEWARD_OPTIONS)
        .addObject("backUrl", overviewUrl(sentBack ? OverviewView.SENT_BACK : OverviewView.INITIAL));
  }

  @PostMapping("/allocate-steward")
  ModelAndView saveAllocateSteward(@RequestParam(defaultValue = "false") boolean sentBack) {
    return sentBack
        ? ReverseRouter.redirect(on(this.getClass()).renderOverviewSentBack(null))
        : ReverseRouter.redirect(on(this.getClass()).renderOverview(null));
  }

  @GetMapping("/upload")
  ModelAndView renderUploadDsp(@RequestParam(defaultValue = "false") boolean dspUploaded) {
    return getUploadModelAndView(new UploadDspMockupForm(), dspUploaded);
  }

  // Files uploaded in the mockup are never linked to an application, so download and delete are stubs.
  @GetMapping("/files/{fileId}")
  ResponseEntity<InputStreamResource> downloadFile(@PathVariable UUID fileId) {
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/files/delete/{fileId}")
  ResponseEntity<FileDeleteResponse> deleteFile(@PathVariable UUID fileId) {
    return ResponseEntity.ok().build();
  }

  private ModelAndView getUploadModelAndView(UploadDspMockupForm form, boolean dspUploaded) {
    var journey = journey();
    var addendumCount = journey.addendumCount();
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/uploadDsp")
        .addObject("pageTitle", "Upload DSP")
        .addObject("caption", CAPTION)
        .addObject("form", form)
        .addObject("dspUploaded", dspUploaded)
        .addObject("decisionInProgress", dspUploaded && journey.isDecisionInProgress())
        .addObject("currentDspFileName", journey.currentDspFileName())
        .addObject("dspCorrected", journey.dspCorrected())
        .addObject("addendumFileNames", journey.addendumFileNames())
        .addObject("backUrl", overviewUrl(caseManagerView(dspUploaded)));

    if (!dspUploaded) {
      return modelAndView.addObject("initialUpload", fileUploadAttributes(
          form.getFinalDecisionSupportPapers(), "form.finalDecisionSupportPapers"));
    }

    modelAndView.addObject("addendumUpload", fileUploadAttributes(form.getDspAddendums(), "form.dspAddendums"));
    if (addendumCount == 0) {
      return modelAndView.addObject("correctedUpload", fileUploadAttributes(form.getCorrectedDsp(), "form.correctedDsp"));
    }

    // One (empty) file list per uploaded addendum so each can bind its own upload widget
    while (form.getCorrectedAddendums().size() < addendumCount) {
      form.getCorrectedAddendums().add(new ArrayList<>());
    }
    return modelAndView
        .addObject("correctedUpload", fileUploadAttributes(form.getCorrectedDsp(), "form.correctedDsp"))
        .addObject("correctedAddendumUploads", IntStream.range(0, addendumCount)
            .mapToObj(i -> fileUploadAttributes(
                form.getCorrectedAddendums().get(i), "form.correctedAddendums[" + i + "]"))
            .toList());
  }

  private FileUploadComponentAttributes fileUploadAttributes(List<UploadedFileForm> files, String path) {
    return fileControllerHelperService.fileUploadComponentAttributes(
        files,
        this.getClass(),
        controller -> controller.downloadFile(null),
        controller -> controller.deleteFile(null),
        path
    );
  }

  // The real screen redirects to the work area; the mockup goes back to the overview to show the next state.
  @PostMapping("/upload")
  ModelAndView submitUploadDsp(
      @RequestParam(defaultValue = "false") boolean dspUploaded,
      @ModelAttribute("form") UploadDspMockupForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (dspUploaded && !isUploadChoiceValid(form, bindingResult)) {
      return getUploadModelAndView(form, true);
    }

    var action = String.valueOf(form.getDspAction());
    var message = switch (action) {
      case ACTION_CORRECT -> "DSP corrected on ";
      case ACTION_ADDENDUM -> "DSP addendum uploaded on ";
      default -> {
        resetJourney();
        yield "DSP uploaded on ";
      }
    };
    updateJourney(journey -> applyUpload(journey, action, form.getReplaceTarget()));
    NotificationBanner.newSuccessBanner()
        .withHeadingContent(message + APPLICATION_REFERENCE)
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDspUploaded(null));
  }

  private void applyUpload(UploadDspMockupJourney journey, String action, String replaceTarget) {
    journey.setSentBack(false);
    switch (action) {
      case ACTION_CORRECT -> {
        var addendumNumber = parseAddendumNumber(replaceTarget);
        if (journey.isValidAddendumNumber(addendumNumber)) {
          journey.correctAddendum(addendumNumber);
        } else {
          journey.correctFinalDsp();
        }
      }
      case ACTION_ADDENDUM -> journey.addAddendum();
      default -> journey.uploadFirstDsp();
    }
  }

  // 0 (never a valid addendum number) when the target is the final DSP or not an addendum target
  private int parseAddendumNumber(String replaceTarget) {
    if (replaceTarget == null || !replaceTarget.startsWith(ADDENDUM_TARGET_PREFIX)) {
      return 0;
    }
    return NumberUtils.toInt(replaceTarget.substring(ADDENDUM_TARGET_PREFIX.length()), 0);
  }

  private boolean isUploadChoiceValid(UploadDspMockupForm form, Errors errors) {
    var action = String.valueOf(form.getDspAction());
    if (!ACTION_CORRECT.equals(action) && !ACTION_ADDENDUM.equals(action)) {
      errors.rejectValue("dspAction", "dspAction.required", "Select what you want to do");
    } else if (ACTION_CORRECT.equals(action) && journey().addendumCount() > 0
        && StringUtils.isBlank(form.getReplaceTarget())) {
      errors.rejectValue("replaceTarget", "replaceTarget.required", "Select which document you want to replace");
    }
    return !errors.hasErrors();
  }

  private TaskListItem decisionDetailsTaskListItem(UploadDspMockupJourney journey) {
    var url = ReverseRouter.route(on(this.getClass()).renderDecisionDetails());
    return journey.decisionDetails().isPresent()
        ? new TaskListItem("Decision details", TaskListLabel.COMPLETE, url)
        : new TaskListItem("Decision details", url);
  }

  private SummaryDataView decisionDetailsSummary(DecisionDetailsMockupForm details) {
    var delegated = "YES".equals(details.getDelegated());
    var summary = SummaryDataView.newBuilder()
        .addStringValue("Decision maker", DecisionDetailsMockupData.decisionMakerName(details.getDecisionMaker()))
        .addStringValue("Delegated", delegated ? "Yes" : "No");
    if (delegated) {
      summary.addStringValue("Delegated to", DecisionDetailsMockupData.decisionMakerName(details.getDelegatedTo()));
    }
    summary
        .addStringValue("Document type",
            "DOV".equals(details.getDocumentType()) ? "Deed of Variation (DoV)" : "Direction letter")
        .addStringValue("RACI consultees", details.getRaciConsulteeIds().stream()
            .map(DecisionDetailsMockupData::consulteeName)
            .toList())
        .addStringValue("Legal approval for obligations/milestones wording",
            switch (String.valueOf(details.getLegalApproval())) {
              case "YES" -> "Yes";
              case "NO" -> "No";
              default -> "N/A (no obligations/milestones are included)";
            });
    return summary.build();
  }

  private UploadDspMockupJourney journey() {
    return session.getAttribute(JOURNEY) instanceof UploadDspMockupJourney existing
        ? existing
        : new UploadDspMockupJourney();
  }

  // Spring Session JDBC only persists an attribute when it is set, so always save after changing the journey
  private void updateJourney(Consumer<UploadDspMockupJourney> change) {
    var journey = journey();
    change.accept(journey);
    session.setAttribute(JOURNEY, journey);
  }

  private void resetJourney() {
    session.removeAttribute(JOURNEY);
  }

  private SummaryFileView fileView(String fileName) {
    var fileId = UUID.randomUUID();
    var uploadedFile = new UploadedFile(fileId);
    uploadedFile.setName(fileName);
    return SummaryFileView.newFromUploadedFile(
        fileName,
        uploadedFile,
        ReverseRouter.route(on(this.getClass()).downloadFile(fileId))
    );
  }
}

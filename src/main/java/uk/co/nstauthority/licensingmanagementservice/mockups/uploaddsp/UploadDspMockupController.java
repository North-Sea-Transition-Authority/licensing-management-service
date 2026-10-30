package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Controller
@RequestMapping("/mockups/upload-dsp")
@Profile("mockups")
public class UploadDspMockupController {

  static final String APPLICATION_REFERENCE = "P1234/5678";
  static final String CAPTION = "Offshore";
  private static final String ACTION_CORRECT = "CORRECT";
  private static final String ACTION_ADDENDUM = "ADDENDUM";
  private static final String ADDENDUM_TARGET_PREFIX = "ADDENDUM_";
  private static final String OVERVIEW_VIEW = "lms/mockups/uploaddsp/applicationOverview";

  static final ScheduleWorkProgrammeApplicationContext APPLICATION_CONTEXT =
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
      .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

  private enum OverviewView {
    INITIAL,
    SENT_BACK,
    DSP_UPLOADED,
    DECISION_ISSUER,
    DECISION_ISSUED,
    EXECUTION,
    SIGNATORY,
    INDUSTRY
  }

  private final FileControllerHelperService fileControllerHelperService;
  private final UploadDspMockupJourneyStore journeyStore;

  UploadDspMockupController(
      FileControllerHelperService fileControllerHelperService,
      UploadDspMockupJourneyStore journeyStore
  ) {
    this.fileControllerHelperService = fileControllerHelperService;
    this.journeyStore = journeyStore;
  }

  // Opening the SUBMITTED overview starts the mockup journey afresh
  @GetMapping
  ModelAndView renderOverview(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    resetJourney();
    return buildOverview(OverviewView.INITIAL, tab);
  }

  // Same actions as the start page, but the journey state carries on instead of being cleared
  @GetMapping("/sent-back")
  ModelAndView renderOverviewSentBack(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    return buildOverview(OverviewView.SENT_BACK, tab);
  }

  @GetMapping("/dsp-uploaded")
  ModelAndView renderOverviewDspUploaded(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    return buildOverview(OverviewView.DSP_UPLOADED, tab);
  }

  // The decision issuer sees the application summary with their own actions
  @GetMapping("/decision-issuer")
  ModelAndView renderOverviewDecisionIssuer(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    return buildOverview(OverviewView.DECISION_ISSUER, tab);
  }

  // Once the decision letter has been approved and issued
  @GetMapping("/execution")
  ModelAndView renderOverviewExecution(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    return buildOverview(OverviewView.EXECUTION, tab);
  }

  // The decision letter has been issued but industry has not accepted yet, so the case manager still has the case
  @GetMapping("/decision-issued")
  ModelAndView renderOverviewDecisionIssued(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    return buildOverview(OverviewView.DECISION_ISSUED, tab);
  }

  // Mockup navigation: switches from the case manager to industry accepting, which hands the case to the executor
  @GetMapping("/industry-accept")
  ModelAndView industryAcceptDecision() {
    updateJourney(UploadDspMockupJourney::acceptDecision);
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewExecution(null));
  }

  // Fresh journey that skips ahead: DSP uploaded, decision recorded and issued, industry accepted
  @GetMapping("/decision-accepted-dov")
  ModelAndView startAtDecisionAcceptedDov() {
    startAcceptedScenario("DOV");
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewExecution(null));
  }

  // Same as above, but the recorded document type is a direction letter
  @GetMapping("/decision-accepted-dl")
  ModelAndView startAtDecisionAcceptedDirectionLetter() {
    startAcceptedScenario("DIRECTION_LETTER");
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewExecution(null));
  }

  private void startAcceptedScenario(String documentType) {
    resetJourney();
    var details = new DecisionDetailsMockupForm();
    details.setDecisionMaker("1");
    details.setDelegated("NO");
    details.setDocumentType(documentType);
    details.setRaciConsulteeIds(List.of("1", "5"));
    details.setLegalApproval("YES");
    updateJourney(journey -> {
      journey.uploadFirstDsp(null);
      journey.startDecision();
      journey.saveDecisionDetails(details);
      journey.recordDecision();
      journey.issueDecisionLetter();
      journey.acceptDecision();
    });
  }

  // The NSTA authorised signatory's view of the application while the direction letter awaits signature
  @GetMapping("/signatory")
  ModelAndView renderOverviewSignatory(@RequestParam(name = "tab", defaultValue = "overview") String tab) {
    return buildOverview(OverviewView.SIGNATORY, tab);
  }

  // A licensee's view of the application, once there is a deed of variation to sign
  @GetMapping("/industry/{licensee}")
  ModelAndView renderOverviewIndustry(
      @PathVariable("licensee") int licensee,
      @RequestParam(name = "tab", defaultValue = "overview") String tab
  ) {
    return buildOverview(OverviewView.INDUSTRY, tab, licensee);
  }

  private ModelAndView buildOverview(OverviewView view, String requestedTab) {
    return buildOverview(view, requestedTab, 0);
  }

  private ModelAndView buildOverview(OverviewView view, String requestedTab, int licensee) {
    var issuedLetters = getIssuedLetters();
    var regulator = view != OverviewView.INDUSTRY;
    List<MockupTab> availableTabs = new ArrayList<>(List.of(OVERVIEW_TAB));
    if (!issuedLetters.isEmpty()) {
      availableTabs.add(LETTER_TAB);
    }
    // Like the real overview, only the regulator sees case events. The DPEA is for them once industry has accepted
    if (regulator && journey().isDecisionAccepted()) {
      availableTabs.add(DPEA_TAB);
    }
    if (regulator) {
      availableTabs.add(CASE_EVENTS_TAB);
    }
    var selectedTab = availableTabs.stream()
        .filter(tab -> tab.value().equals(requestedTab))
        .findFirst()
        .orElse(OVERVIEW_TAB);
    var modelAndView = new ModelAndView(OVERVIEW_VIEW)
        .addObject("availableTabs", availableTabs)
        .addObject("selectedTab", selectedTab)
        .addObject("controllerUrl", view == OverviewView.INDUSTRY ? industryUrl(licensee) : overviewUrl(view));

    // Like the continuation Letter tab: the issued document and who issued it, where licensees download it
    if (selectedTab == LETTER_TAB) {
      return modelAndView
          .addObject("accordionId", UUID.randomUUID().toString())
          .addObject("issuedLetters", issuedLetters);
    }

    if (selectedTab == DPEA_TAB) {
      return modelAndView.addObject("dpeaSections", DpeaSummary.build(journey()));
    }

    if (selectedTab == CASE_EVENTS_TAB) {
      return modelAndView.addObject("caseEventsSummaryItem",
          SummaryItem.withCards(CASE_EVENTS_TAB.label(), getCaseEventCards()));
    }
    modelAndView
        .addObject("applicationContext", APPLICATION_CONTEXT)
        .addObject("summarySections", SUMMARY_SECTIONS)
        .addObject("accordionId", UUID.randomUUID().toString())
        .addObject("applicationActions", getApplicationActions(view, licensee));
    if (view == OverviewView.INDUSTRY) {
      getLicenseeNotice(licensee).ifPresent(notice -> modelAndView.addObject("licenseeNotice", notice));
    }
    return modelAndView;
  }

  // Not the real OverviewTab, which has no DPEA tab. The tabs macro reads label(), value() and anchor()
  public record MockupTab(String label, String value, String anchor) {
  }

  private static final MockupTab OVERVIEW_TAB = new MockupTab("Overview", "overview", "overview");
  private static final MockupTab LETTER_TAB = new MockupTab("Letter", "letter", "letter");
  private static final MockupTab DPEA_TAB = new MockupTab("DPEA", "dpea", "dpea");
  private static final MockupTab CASE_EVENTS_TAB = new MockupTab("Case events", "case-events", "case-events");

  public record IssuedLetter(String name, String issuedBy, String issuedDate, String fileName) {
  }

  private List<IssuedLetter> getIssuedLetters() {
    var journey = journey();
    var letters = new ArrayList<IssuedLetter>();
    addIssuedLetter(letters, "Decision letter", "decision-letter.pdf",
        journey.latestEvent(UploadDspMockupJourney.DECISION_LETTER_ISSUED));
    addIssuedLetter(letters, "Direction letter", "direction-letter.pdf",
        journey.latestEvent(UploadDspMockupJourney.DIRECTION_LETTER_ISSUED));
    addIssuedLetter(letters, "Deed of variation", "deed-of-variation.pdf",
        journey.latestEvent(UploadDspMockupJourney.DEED_SENT));
    return letters;
  }

  private void addIssuedLetter(
      List<IssuedLetter> letters,
      String name,
      String fileName,
      Optional<UploadDspMockupJourney.CaseEvent> event
  ) {
    event.ifPresent(e -> letters.add(new IssuedLetter(
        name, e.eventBy(), DateFormatUtil.convertToDisplayTextWithTime(e.at()), fileName)));
  }

  // A licensee is asked to post their hardcopies as soon as their own signed deed is accepted
  private Optional<String> getLicenseeNotice(int licensee) {
    var journey = journey();
    if (journey.canUploadSignedDeed(licensee) && journey.sendBackReason(licensee) != null) {
      return Optional.of("The NSTA sent your signed deed back: " + journey.sendBackReason(licensee));
    }
    if (journey.isAwaitingHardcopy(licensee)) {
      return Optional.of("Your signed deed has been accepted. Post the 2 signed hardcopies to the NSTA.");
    }
    return Optional.empty();
  }

  static String executionUrl() {
    return ReverseRouter.route(on(UploadDspMockupController.class).renderOverviewExecution(null));
  }

  static String industryUrl(int licensee) {
    return ReverseRouter.route(on(UploadDspMockupController.class).renderOverviewIndustry(licensee, null));
  }

  private List<ActionItemView> getApplicationActions(OverviewView view, int licensee) {
    return switch (view) {
      case EXECUTION -> getExecutionActions();
      case DECISION_ISSUED -> journey().isDecisionAccepted()
          ? List.of()
          : List.of(new ActionItemView("Send back to recommendation", 10, false,
              ReverseRouter.route(on(this.getClass()).sendBackToRecommendation()), null));
      case INDUSTRY -> journey().canUploadSignedDeed(licensee)
          ? List.of(new ActionItemView("Upload signed deed", 10, true,
              ReverseRouter.route(on(DeedOfVariationMockupController.class).renderSignedDeed(licensee)), null))
          : List.of();
      case SIGNATORY -> journey().signatureStatus() == UploadDspMockupJourney.SignatureStatus.AWAITING
          ? List.of(
              new ActionItemView("Sign direction letter", 10, true,
                  ReverseRouter.route(on(DirectionLetterMockupController.class).renderSign()), null),
              new ActionItemView("Send back to decision executor", 20, false,
                  ReverseRouter.route(on(DirectionLetterMockupController.class).renderSendBack()), null))
          : List.of();
      case DECISION_ISSUER -> List.of(
          new ActionItemView("Prepare decision letter", 10, true,
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

  private List<ActionItemView> getDeedActions(UploadDspMockupJourney.DeedStatus deedStatus) {
    return switch (deedStatus) {
      case LICENSEE_SIGNING -> List.of(new ActionItemView("Signed deeds and hardcopies", 10, true,
          ReverseRouter.route(on(DeedOfVariationMockupController.class).renderSignedCopies()), null));
      case NSTA_SIGNING -> List.of(new ActionItemView("Send conformed deed", 10, true,
          ReverseRouter.route(on(DeedOfVariationMockupController.class).renderComplete()), null));
      case POSTING_BACK -> List.of(new ActionItemView("Record hardcopies posted", 10, true,
          ReverseRouter.route(on(DeedOfVariationMockupController.class).renderPosted()), null));
      case NOT_STARTED, COMPLETE -> List.of();
    };
  }

  // The decision executor's actions: the recorded document type is only a starting point they can ignore
  private List<ActionItemView> getExecutionActions() {
    var journey = journey();
    if (journey.deedStatus() != UploadDspMockupJourney.DeedStatus.NOT_STARTED) {
      return getDeedActions(journey.deedStatus());
    }
    if (journey.isLetterIssued()) {
      return List.of();
    }
    var directionLetterUrl = ReverseRouter.route(on(DirectionLetterMockupController.class).renderDirectionLetter());
    return switch (journey.signatureStatus()) {
      case AWAITING -> List.of(
          new ActionItemView("Send reminder", 10, false,
              ReverseRouter.route(on(DirectionLetterMockupController.class).sendReminder(null)), null),
          new ActionItemView("Change signatory", 20, false, directionLetterUrl, null)
      );
      case SENT_BACK -> List.of(new ActionItemView("Adjust direction letter", 10, true, directionLetterUrl, null));
      case SIGNED -> List.of(
          new ActionItemView("Issue direction letter", 10, true,
              ReverseRouter.route(on(DirectionLetterMockupController.class).issueLetter(null)), null)
      );
      case NOT_REQUESTED -> {
        var deedRecorded = "DOV".equals(journey.recordedDocumentType());
        yield List.of(
            new ActionItemView("Prepare direction letter", 10, !deedRecorded, directionLetterUrl, null),
            new ActionItemView("Prepare deed of variation", 20, deedRecorded,
                ReverseRouter.route(on(DeedOfVariationMockupController.class).renderDeed()), null)
        );
      }
    };
  }

  private String overviewUrl(OverviewView view) {
    return switch (view) {
      case INITIAL -> ReverseRouter.route(on(this.getClass()).renderOverview(null));
      case SENT_BACK -> ReverseRouter.route(on(this.getClass()).renderOverviewSentBack(null));
      case DSP_UPLOADED -> ReverseRouter.route(on(this.getClass()).renderOverviewDspUploaded(null));
      case DECISION_ISSUER -> ReverseRouter.route(on(this.getClass()).renderOverviewDecisionIssuer(null));
      case DECISION_ISSUED -> ReverseRouter.route(on(this.getClass()).renderOverviewDecisionIssued(null));
      case EXECUTION -> ReverseRouter.route(on(this.getClass()).renderOverviewExecution(null));
      case SIGNATORY -> ReverseRouter.route(on(this.getClass()).renderOverviewSignatory(null));
      case INDUSTRY -> industryUrl(0);
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
      cards.add(caseEventCard(event.heading(), Map.of(event.detailKey(), event.detailValue()), event.eventBy(),
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
    var modelAndView = new ModelAndView("lms/mockups/uploaddsp/decisionLetter")
        .addObject("pageTitle", "Decision letter for Shell UK limited")
        .addObject("signUrl", ReverseRouter.route(on(this.getClass()).approveAndIssueLetter()))
        .addObject("previewUrl", "#")
        .addObject("reloadUrl", "#")
        .addObject("documentInstanceSectionsSummaryView", MockLetter.decisionLetter())
        .addObject("accordionId", UUID.randomUUID().toString())
        .addObject("hasMoreThanOneSection", true)
        .addObject("userHasValidPermission", true)
        .addObject("errorList", List.of());

    var breadcrumbs = Breadcrumbs.builder("Decision letter")
        .addWorkAreaBreadcrumb()
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  @PostMapping("/issue-decision")
  ModelAndView approveAndIssueLetter() {
    updateJourney(UploadDspMockupJourney::issueDecisionLetter);
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDecisionIssued(null));
  }

  // Goes further back than the issuer's send back, to the Upload DSP stage
  @GetMapping("/send-back-to-recommendation")
  ModelAndView sendBackToRecommendation() {
    updateJourney(journey -> {
      journey.setSentBack(true);
      journey.returnToCaseManager();
    });
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewSentBack(null));
  }

  // Undoes a send back made by mistake: returns the case to the decision issuer
  @GetMapping("/send-back-to-issue-decision")
  ModelAndView sendBackToIssueDecision() {
    updateJourney(UploadDspMockupJourney::passToDecisionIssuer);
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDecisionIssuer(null));
  }

  @GetMapping("/send-back-to-record-decision")
  ModelAndView sendBackToRecordDecision() {
    updateJourney(journey -> {
      journey.setSentBack(false);
      journey.returnToCaseManager();
    });
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
        .addObject("decisionDate", DateFormatUtil.convertToDisplayText(journey.decisionDate()))
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
    var decisionDate = form.getDecisionDate().getAsLocalDate().orElse(null);
    updateJourney(journey -> applyUpload(journey, action, form.getReplaceTarget(), decisionDate));
    NotificationBanner.newSuccessBanner()
        .withHeadingContent(message + APPLICATION_REFERENCE)
        .applyTo(redirectAttributes);
    return ReverseRouter.redirect(on(this.getClass()).renderOverviewDspUploaded(null));
  }

  private void applyUpload(
      UploadDspMockupJourney journey,
      String action,
      String replaceTarget,
      LocalDate decisionDate
  ) {
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
      default -> journey.uploadFirstDsp(decisionDate);
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
            DpeaSummary.legalApproval(details.getLegalApproval()));
    return summary.build();
  }

  private UploadDspMockupJourney journey() {
    return journeyStore.journey();
  }

  private void updateJourney(Consumer<UploadDspMockupJourney> change) {
    journeyStore.update(change);
  }

  private void resetJourney() {
    journeyStore.reset();
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

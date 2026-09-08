package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineAction;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineActionView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineOtherScheduleEventView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelinePhaseView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineRateView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineSummaryCardView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineTermView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.TimelineWorkProgrammeActivityView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;

/**
 * Hardcoded stand-in for {@code LicenceScheduleTimelineService}. Produces the same view objects the real schedule
 * timeline templates consume, modelling a seaward production licence with an initial term split into phases A, B and C,
 * followed by a second term and a third term.
 */
class LicenceScheduleMockupData {

  /**
   * Every link on the mockup points back at the mockup itself so that nothing navigates away from the page.
   */
  private static final String MOCK_URL = LicenceScheduleMockupController.PAGE_URL;

  private static final LocalDate INITIAL_TERM_START_DATE = LocalDate.of(2020, 1, 1);
  private static final LocalDate SECOND_TERM_START_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate THIRD_TERM_START_DATE = LocalDate.of(2028, 1, 1);

  /**
   * Anything due on or before this date renders as progressed (a filled in timeline node). Fixed rather than taken from
   * the clock so the mockup always shows a part completed schedule.
   */
  private static final LocalDate PROGRESS_DATE = LocalDate.of(2026, 9, 7);

  private LicenceScheduleMockupData() {
  }

  static TimelineSummaryCardView getSummaryCardView() {
    return new TimelineSummaryCardView(
        "1 January 2020",
        "1 January 2046",
        true,
        "33",
        "Extant",
        "",
        "1 January 2046",
        List.of("Example Energy (UK) Limited", "Example North Sea Partners Limited")
    );
  }

  /**
   * Comments recorded against the schedule as a whole rather than against an individual timeline event.
   */
  static List<ScheduleCommentView> getScheduleComments() {
    return List.of(
        new ScheduleCommentView(
            "Schedule rebuilt from the signed licence document following the 33rd round award.",
            "Jane Smith",
            "12 January 2020 11:04:27",
            MOCK_URL,
            "",
            ""
        ),
        new ScheduleCommentView(
            "Initial term end of term relinquishment agreed with the licensee. Retained acreage covers the " +
                "21/10-3 development plan area only.",
            "Andy Admin",
            "18 December 2023 16:41:02",
            MOCK_URL,
            "",
            ""
        ),
        new ScheduleCommentView(
            "Alternative work programme for drill one exploration well to a minimum depth of 3,000m below " +
                "mean sea level to be completed under licence P2500.",
            "John Smith",
            "3 March 2025 08:57:16",
            MOCK_URL,
            "P2500",
            MOCK_URL
        )
    );
  }

  static List<TimelineActionView> getActions() {
    return Arrays.stream(LicenceScheduleTimelineAction.values())
        .map(action -> new TimelineActionView(action, MOCK_URL))
        .toList();
  }

  static List<TimelineTermView> getTermViews() {
    return List.of(initialTerm(), secondTerm(), thirdTerm());
  }

  private static TimelineTermView initialTerm() {
    var events = List.<ScheduleEvent>of(
        rate("Initial Term rate", INITIAL_TERM_START_DATE, "1 January 2020 to 31 December 2023", "15.00"),
        phaseA(),
        phaseB(),
        phaseC()
    );

    var endOfTermEvents = List.<ScheduleEvent>of(
        otherEvent(
            "Mandatory relinquishment",
            "Relinquish 50% of the licensed acreage, excluding any area retained under an approved development plan.",
            LocalDate.of(2024, 1, 1)
        )
    );

    return new TimelineTermView(
        events,
        endOfTermEvents,
        TermType.INITIAL,
        "1 January 2020 to 1 January 2024 (4 years)",
        "1 January 2024",
        MOCK_URL,
        MOCK_URL,
        "",
        true,
        List.of(),
        true,
        true
    );
  }

  private static TimelinePhaseView phaseA() {
    var events = List.<ScheduleEvent>of(
        activity(
            "Reprocess seismic data (Firm)",
            "Reprocess and reinterpret the existing 3D seismic survey covering blocks 21/10 and 21/15.",
            LocalDate.of(2021, 6, 30),
            WorkProgrammeStatus.COMPLETE
        )
    );

    return new TimelinePhaseView(
        events,
        List.of(),
        PhaseType.PHASE_A,
        LocalDate.of(2020, 1, 1),
        "1 January 2020 to 1 January 2022 (2 years)",
        "1 January 2022",
        MOCK_URL,
        MOCK_URL,
        "",
        List.of(),
        true,
        true
    );
  }

  private static TimelinePhaseView phaseB() {
    var events = List.<ScheduleEvent>of(
        activity(
            "Drill or drop well (Firm)",
            "Drill an exploration well to a minimum depth of 2,500m, or relinquish the licence.",
            LocalDate.of(2022, 9, 30),
            WorkProgrammeStatus.COMPLETE
        )
    );

    var endOfPhaseEvents = List.<ScheduleEvent>of(
        activity(
            "Well investment engagement (Firm)",
            "Submit a post well evaluation report and confirm the investment decision for the phase C programme.",
            null,
            WorkProgrammeStatus.COMPLETE
        )
    );

    return new TimelinePhaseView(
        events,
        endOfPhaseEvents,
        PhaseType.PHASE_B,
        LocalDate.of(2022, 1, 1),
        "1 January 2022 to 1 January 2023 (1 year)",
        "1 January 2023",
        MOCK_URL,
        MOCK_URL,
        "",
        List.of(),
        true,
        true
    );
  }

  private static TimelinePhaseView phaseC() {
    var events = List.<ScheduleEvent>of(
        activity(
            "Well test (Contingent)",
            "Carry out a well test on the 21/10-3 discovery if commercial flow rates are indicated.",
            LocalDate.of(2023, 6, 30),
            WorkProgrammeStatus.FULL_WAIVER
        )
    );

    return new TimelinePhaseView(
        events,
        List.of(),
        PhaseType.PHASE_C,
        LocalDate.of(2023, 1, 1),
        "1 January 2023 to 1 January 2024 (1 year)",
        "1 January 2024",
        MOCK_URL,
        MOCK_URL,
        "",
        List.of(),
        true,
        true
    );
  }

  private static TimelineTermView secondTerm() {
    var events = List.<ScheduleEvent>of(
        rate("Second Term rate", SECOND_TERM_START_DATE, "1 January 2024 to 31 December 2027", "30.00"),
        activity(
            "Drill well (Firm)",
            "Drill an appraisal well on the 21/10-3 discovery and submit the resulting well data.",
            LocalDate.of(2026, 3, 31),
            WorkProgrammeStatus.IN_PROGRESS
        ),
        activity(
            "New shoot 3D seismic data (Contingent)",
            "Acquire a new 3D seismic survey over the northern part of the licence area.",
            LocalDate.of(2027, 6, 30),
            WorkProgrammeStatus.OPEN
        )
    );

    var endOfTermEvents = List.<ScheduleEvent>of(
        otherEvent(
            "Mandatory relinquishment",
            "Relinquish all acreage not covered by an approved field development plan.",
            LocalDate.of(2028, 1, 1)
        )
    );

    return new TimelineTermView(
        events,
        endOfTermEvents,
        TermType.SECOND,
        "1 January 2024 to 1 January 2028 (4 years)",
        "1 January 2028",
        MOCK_URL,
        MOCK_URL,
        "",
        false,
        List.of(comment(
            "Second term start date confirmed following the phase C end of term review.",
            "Jane Smith",
            "14 February 2024 09:32:11"
        )),
        true,
        false
    );
  }

  private static TimelineTermView thirdTerm() {
    var events = List.<ScheduleEvent>of(
        rate("Third Term rate", THIRD_TERM_START_DATE, "1 January 2028 to 31 December 2045", "50.00"),
        otherEvent(
            "Other activity",
            "Submit a decommissioning cost estimate covering all licence infrastructure.",
            LocalDate.of(2044, 1, 1)
        )
    );

    return new TimelineTermView(
        events,
        List.of(),
        TermType.THIRD,
        "1 January 2028 to 1 January 2046 (18 years)",
        "1 January 2046",
        MOCK_URL,
        MOCK_URL,
        "",
        false,
        List.of(),
        false,
        false
    );
  }

  private static TimelineRateView rate(
      String title,
      LocalDate startDate,
      String startEndDateString,
      String rentalRate
  ) {
    return new TimelineRateView(
        title,
        startDate,
        startEndDateString,
        "£%s".formatted(rentalRate),
        MOCK_URL,
        MOCK_URL,
        "",
        List.of(),
        !startDate.isAfter(PROGRESS_DATE)
    );
  }

  private static TimelineWorkProgrammeActivityView activity(
      String category,
      String description,
      LocalDate dueDate,
      WorkProgrammeStatus status
  ) {
    return new TimelineWorkProgrammeActivityView(
        category,
        description,
        dueDate,
        dueDate != null ? DateFormatUtil.convertToDisplayText(dueDate) : "",
        MOCK_URL,
        MOCK_URL,
        "",
        "",
        status,
        List.of(),
        dueDate != null && !dueDate.isAfter(PROGRESS_DATE)
    );
  }

  private static TimelineOtherScheduleEventView otherEvent(
      String category,
      String description,
      LocalDate eventDate
  ) {
    return new TimelineOtherScheduleEventView(
        category,
        description,
        eventDate,
        DateFormatUtil.convertToDisplayText(eventDate),
        MOCK_URL,
        MOCK_URL,
        "",
        List.of(),
        !eventDate.isAfter(PROGRESS_DATE)
    );
  }

  private static EventCommentView comment(String text, String author, String datetime) {
    return new EventCommentView(text, author, datetime, MOCK_URL);
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.StartEndDates;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCommitment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTimelineServiceTest {

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private LicenceScheduleRateService licenceScheduleRateService;

  @Mock
  private LicenceScheduleExpiryService licenceScheduleExpiryService;
  
  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  @Mock
  private EventCommentService eventCommentService;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withRoundIssuedOn("1")
        .withStatus(LicenceStatus.EXTANT)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
  }

  @Test
  void getTimelineSummaryCardView() {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    var licenceExpiryDate = new LicenceScheduleExpiry();
    licenceExpiryDate.setExpiryDate(LocalDate.of(2026, 1, 1));

    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.of(licenceExpiryDate));
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(true);

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(
            TimelineSummaryCardView::licenceStartDate,
            TimelineSummaryCardView::licenceExpiryDate,
            TimelineSummaryCardView::showRoundIssuedOn,
            TimelineSummaryCardView::roundIssuedOn,
            TimelineSummaryCardView::status
        )
        .containsExactly(
            DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
            DateFormatUtil.convertToDisplayText(licenceExpiryDate.getExpiryDate()),
            true,
            licence.getRoundIssuedOn(),
            licence.getStatus().getDisplayName()
        );
  }

  @Test
  void getTimelineSummaryCardView_blankExpiryDate() {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    var licenceExpiryDate = new LicenceScheduleExpiry();

    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.of(licenceExpiryDate));
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(true);

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(
            TimelineSummaryCardView::licenceStartDate,
            TimelineSummaryCardView::licenceExpiryDate,
            TimelineSummaryCardView::showRoundIssuedOn,
            TimelineSummaryCardView::roundIssuedOn,
            TimelineSummaryCardView::status
        )
        .containsExactly(
            DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
            "",
            true,
            licence.getRoundIssuedOn(),
            licence.getStatus().getDisplayName()
        );
  }

  @Test
  void getTimelineSummaryCardView_withLicenceEndDate() {
    var startDate = LocalDate.of(2025, 1, 1);
    var endDate = LocalDate.of(2027, 1, 1);
    licence.setEndDate(endDate);

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(startDate);

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);
    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.empty());
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(false);

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(TimelineSummaryCardView::licenceEndedDate)
        .isEqualTo("1 January 2027 (2 years 1 day)");
  }

  @Test
  void getTimelineSummaryCardView_withNoLicenceEndDate() {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);
    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.empty());
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(false);

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(TimelineSummaryCardView::licenceEndedDate)
        .isEqualTo("");
  }

  @Test
  void getTimelineSummaryCardView_withFinalTerm() {
    var startDate = LocalDate.of(2025, 1, 1);
    var finalTermEndDate = LocalDate.of(2026, 1, 1);

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(startDate);

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.SECOND);
    term.setEndDate(finalTermEndDate);

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);
    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.empty());
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(false);
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(TimelineSummaryCardView::finalTermEndDate)
        .isEqualTo("1 January 2026 (1 year 1 day)");
  }

  @Test
  void getTimelineSummaryCardView_withNoTerms() {
    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);
    when(licenceScheduleExpiryService.getExpiryForLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(Optional.empty());
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(false);
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(TimelineSummaryCardView::finalTermEndDate)
        .isEqualTo("");
  }

  @Test
  void getLicenceScheduleTimelineActions() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasRentalRate(licence.getType())).thenReturn(true);

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var expectedResult = List.of(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_PHASE,
            ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderAddNewPhaseForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_WORK_PROGRAMME_ACTIVITY,
            ReverseRouter.route(on(WorkProgrammeActivityController.class).renderAddNewActivityForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_RATE,
            ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_SCHEDULE_EVENT,
            ReverseRouter.route(on(OtherScheduleEventController.class).renderAddNewEventForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleTimelineActions_noScheduleAdminRole() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasRentalRate(licence.getType())).thenReturn(true);

    var allowedActions = List.of(ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var expectedResult = List.of(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_WORK_PROGRAMME_ACTIVITY,
            ReverseRouter.route(on(WorkProgrammeActivityController.class).renderAddNewActivityForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleTimelineActions_noWorkProgrammeAdminRole() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasRentalRate(licence.getType())).thenReturn(true);

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS);

    var expectedResult = List.of(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_PHASE,
            ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderAddNewPhaseForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_RATE,
            ReverseRouter.route(on(LicenceScheduleRateController.class).renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_SCHEDULE_EVENT,
            ReverseRouter.route(on(OtherScheduleEventController.class).renderAddNewEventForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleTimelineActions_actionsDisabled() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(false);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(false);
    when(licenceTypeRulesResolver.hasRentalRate(licence.getType())).thenReturn(false);

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var expectedResult = List.of(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_SCHEDULE_EVENT,
            ReverseRouter.route(on(OtherScheduleEventController.class).renderAddNewEventForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getEditableLicenceScheduleEventViews() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setScheduleEvent(midPhaseActivity);
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midPhaseActivity),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midPhaseActivity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.IN_PROGRESS,
        List.of(),
        true
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setScheduleEvent(endOfPhaseActivity);
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfPhaseActivity),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfPhaseActivity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setScheduleEvent(midTerm2Activity);
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midTerm2Activity),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midTerm2Activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        true
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setScheduleEvent(endOfTerm2Activity);
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfTerm2Activity),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfTerm2Activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setOriginalEventId(midPhaseEvent.getId());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));

    var midPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(midPhaseEvent.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(midPhaseEvent.getId())),
        "",
        List.of(),
        true
    );

    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setOriginalEventId(endOfPhaseEvent.getId());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");

    var endOfPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(endOfPhaseEvent.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(endOfPhaseEvent.getId())),
        "",
        List.of(),
        false
    );

    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setOriginalEventId(midTerm2Event.getId());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));

    var midTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(midTerm2Event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(midTerm2Event.getId())),
        "",
        List.of(),
        true
    );

    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setOriginalEventId(endOfTerm2Event.getId());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");

    var endOfTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(endOfTerm2Event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(endOfTerm2Event.getId())),
        "",
        List.of(),
        false
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(phaseRate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(phaseRate.getId())),
        "",
        List.of(),
        true
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseActivityView, midPhaseEventView),
        List.of(endOfPhaseActivityView, endOfPhaseEventView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(phase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(phase.getId())),
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term.getId())),
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(term2Rate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(term2Rate.getId())),
        "",
        List.of(),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2ActivityView, midTerm2EventView),
        List.of(endOfTerm2ActivityView, endOfTerm2EventView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term2.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term2.getId())),
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getEditableLicenceScheduleEventViews_noEditPermissions() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setScheduleEvent(midPhaseActivity);
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midPhaseActivity),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.IN_PROGRESS,
        List.of(),
        true
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setScheduleEvent(endOfPhaseActivity);
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfPhaseActivity),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.CONTINGENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setScheduleEvent(midTerm2Activity);
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midTerm2Activity),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        true
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setScheduleEvent(endOfTerm2Activity);
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfTerm2Activity),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setOriginalEventId(midPhaseEvent.getId());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));

    var midPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        "",
        List.of(),
        true
    );

    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setOriginalEventId(endOfPhaseEvent.getId());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");

    var endOfPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        List.of(),
        false
    );

    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setOriginalEventId(midTerm2Event.getId());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));

    var midTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        "",
        List.of(),
        true
    );

    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setOriginalEventId(endOfTerm2Event.getId());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");

    var endOfTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        List.of(),
        false
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        "",
        "",
        "",
        List.of(),
        true
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseActivityView, midPhaseEventView),
        List.of(endOfPhaseActivityView, endOfPhaseEventView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "",
        "",
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "",
        "",
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        "",
        "",
        "",
        List.of(),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2ActivityView, midTerm2EventView),
        List.of(endOfTerm2ActivityView, endOfTerm2EventView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        "",
        "",
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, List.of()))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getEditableLicenceScheduleEventViews_rateFilterEnabled() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setScheduleEvent(midPhaseActivity);
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midPhaseActivity),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midPhaseActivity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.IN_PROGRESS,
        List.of(),
        true
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setScheduleEvent(endOfPhaseActivity);
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfPhaseActivity),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfPhaseActivity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setScheduleEvent(midTerm2Activity);
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midTerm2Activity),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midTerm2Activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        true
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setScheduleEvent(endOfTerm2Activity);
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfTerm2Activity),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfTerm2Activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setOriginalEventId(midPhaseEvent.getId());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));

    var midPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(midPhaseEvent.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(midPhaseEvent.getId())),
        "",
        List.of(),
        true
    );

    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setOriginalEventId(endOfPhaseEvent.getId());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");

    var endOfPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(endOfPhaseEvent.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(endOfPhaseEvent.getId())),
        "",
        List.of(),
        false
    );

    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setOriginalEventId(midTerm2Event.getId());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));

    var midTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(midTerm2Event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(midTerm2Event.getId())),
        "",
        List.of(),
        true
    );

    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setOriginalEventId(endOfTerm2Event.getId());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");

    var endOfTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(endOfTerm2Event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(endOfTerm2Event.getId())),
        "",
        List.of(),
        false
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseView = new TimelinePhaseView(
        List.of(midPhaseActivityView, midPhaseEventView),
        List.of(endOfPhaseActivityView, endOfPhaseEventView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(phase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(phase.getId())),
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term.getId())),
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var termView2 = new TimelineTermView(
        List.of(midTerm2ActivityView, midTerm2EventView),
        List.of(endOfTerm2ActivityView, endOfTerm2EventView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term2.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term2.getId())),
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(
        ScheduleEventType.WORK_PROGRAMME_ACTIVITY.name(),
        ScheduleEventType.OTHER.name()
    ));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getEditableLicenceScheduleEventViews_workProgrammeActivityFilterEnabled() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setScheduleEvent(midPhaseActivity);
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setScheduleEvent(endOfPhaseActivity);
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setScheduleEvent(midTerm2Activity);
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.CONTINGENT);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setScheduleEvent(endOfTerm2Activity);
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setOriginalEventId(midPhaseEvent.getId());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));

    var midPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(midPhaseEvent.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(midPhaseEvent.getId())),
        "",
        List.of(),
        true
    );

    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setOriginalEventId(endOfPhaseEvent.getId());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");

    var endOfPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(endOfPhaseEvent.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(endOfPhaseEvent.getId())),
        "",
        List.of(),
        false
    );

    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setOriginalEventId(midTerm2Event.getId());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));

    var midTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(midTerm2Event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(midTerm2Event.getId())),
        "",
        List.of(),
        true
    );

    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setOriginalEventId(endOfTerm2Event.getId());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");

    var endOfTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(endOfTerm2Event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(endOfTerm2Event.getId())),
        "",
        List.of(),
        false
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(phaseRate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(phaseRate.getId())),
        "",
        List.of(),
        true
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseEventView),
        List.of(endOfPhaseEventView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(phase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(phase.getId())),
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term.getId())),
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(term2Rate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(term2Rate.getId())),
        "",
        List.of(),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2EventView),
        List.of(endOfTerm2EventView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term2.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term2.getId())),
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(
        ScheduleEventType.RATE.name(),
        ScheduleEventType.OTHER.name()
    ));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getEditableLicenceScheduleEventViews_otherEventFilterEnabled() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setScheduleEvent(midPhaseActivity);
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midPhaseActivity),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midPhaseActivity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.IN_PROGRESS,
        List.of(),
        true
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setScheduleEvent(endOfPhaseActivity);
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfPhaseActivity),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfPhaseActivity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setScheduleEvent(midTerm2Activity);
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midTerm2Activity),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midTerm2Activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        true
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.CONDITIONAL);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setScheduleEvent(endOfTerm2Activity);
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfTerm2Activity),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfTerm2Activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setOriginalEventId(midPhaseEvent.getId());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));

    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setOriginalEventId(endOfPhaseEvent.getId());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");

    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setOriginalEventId(midTerm2Event.getId());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));

    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setOriginalEventId(endOfTerm2Event.getId());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(phaseRate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(phaseRate.getId())),
        "",
        List.of(),
        true
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseActivityView),
        List.of(endOfPhaseActivityView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(phase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(phase.getId())),
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term.getId())),
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(term2Rate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(term2Rate.getId())),
        "",
        List.of(),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2ActivityView),
        List.of(endOfTerm2ActivityView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term2.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term2.getId())),
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(
        ScheduleEventType.RATE.name(),
        ScheduleEventType.WORK_PROGRAMME_ACTIVITY.name()
    ));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, allowedActions))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getLicenceScheduleEventViewsForOverview_userHasWpStatusPermissions() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.CONTINGENT);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setScheduleEvent(midPhaseActivity);
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midPhaseActivity),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(midPhaseActivity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(midPhaseActivity.getId(), null)),
        WorkProgrammeStatus.IN_PROGRESS,
        List.of(),
        true
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.CONTINGENT);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setScheduleEvent(endOfPhaseActivity);
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfPhaseActivity),
        "description",
        null,
        "",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(endOfPhaseActivity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(endOfPhaseActivity.getId(), null)),
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.CONTINGENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setScheduleEvent(midTerm2Activity);
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midTerm2Activity),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(midTerm2Activity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(midTerm2Activity.getId(), null)),
        WorkProgrammeStatus.OPEN,
        List.of(),
        true
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setScheduleEvent(endOfTerm2Activity);
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfTerm2Activity),
        "description",
        null,
        "",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(endOfTerm2Activity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(endOfTerm2Activity.getId(), null)),
        WorkProgrammeStatus.OPEN,
        List.of(),
        false
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        "",
        "",
        "",
        List.of(),
        true
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseActivityView),
        List.of(endOfPhaseActivityView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "",
        "",
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "",
        "",
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        "",
        "",
        "",
        List.of(),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2ActivityView),
        List.of(endOfTerm2ActivityView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        "",
        "",
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var user = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);
    when(teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR)))
        .thenReturn(true);
    when(teamQueryService.userIsInRegulatorTeam(user.wuaId())).thenReturn(true);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getLicenceScheduleEventViewsForOverview(licenceScheduleDetail, form, user))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getLicenceScheduleEventViewsForOverview_userDoesNotHaveWpStatusPermissions_userIsNotRegulator() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setOriginalEventId(midPhaseActivity.getId());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midPhaseActivity),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        "",
        "",
        null,
        List.of(),
        true
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setOriginalEventId(endOfPhaseActivity.getId());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfPhaseActivity),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        null,
        List.of(),
        false
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setOriginalEventId(midTerm2Activity.getId());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(midTerm2Activity),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        "",
        "",
        null,
        List.of(),
        true
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setOriginalEventId(endOfTerm2Activity.getId());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setCommitment(WorkProgrammeActivityCommitment.FIRM);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        TimelineWorkProgrammeActivityView.getCategoryAndCommitmentString(endOfTerm2Activity),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        null,
        List.of(),
        false
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setOriginalEventId(phase.getId());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setOriginalEventId(phaseRate.getId());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        "",
        "",
        "",
        List.of(),
        true
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseActivityView),
        List.of(endOfPhaseActivityView),
        PhaseType.PHASE_A,
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "",
        "",
        "",
        List.of(),
        true,
        true
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "",
        "",
        "",
        true,
        List.of(),
        true, true);

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setOriginalEventId(term2.getId());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setOriginalEventId(term2Rate.getId());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        "",
        "",
        "",
        List.of(),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2ActivityView),
        List.of(endOfTerm2ActivityView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        "",
        "",
        "",
        false,
        List.of(),
        true, false);

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var user = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userIsInRegulatorTeam(user.wuaId())).thenReturn(false);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            phaseRate.getId(), new StartEndDates(phase.getStartDate(), phase.getEndDate()),
            term2Rate.getId(), new StartEndDates(term2.getStartDate(), term2.getEndDate())
        ));

    assertThat(licenceScheduleTimelineService.getLicenceScheduleEventViewsForOverview(licenceScheduleDetail, form, user))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }

  @Test
  void getEditableLicenceScheduleEventViews_commentsArePopulatedOnViews() {
    when(clock.instant()).thenReturn(LocalDate.of(2026, 7, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setOriginalEventId(term.getId());

    var wpa = new WorkProgrammeActivity();
    wpa.setId(UUID.randomUUID());
    wpa.setOriginalEventId(wpa.getId());
    wpa.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    wpa.setCommitment(WorkProgrammeActivityCommitment.CONTINGENT);
    wpa.setDescription("WPA description");
    wpa.setDueDate(LocalDate.of(2025, 6, 1));

    var wpaStatus = new WorkProgrammeActivityStatus();
    wpaStatus.setScheduleEvent(wpa);
    wpaStatus.setStatus(WorkProgrammeStatus.OPEN);

    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setOriginalEventId(rate.getId());
    rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate.setLicenceScheduleTerm(term);
    rate.setOriginalEventId(rate.getId());
    rate.setRentalRate(new BigDecimal("3.00"));
    rate.setStartDate(LocalDate.of(2025, 1, 1));

    var otherEvent = new OtherScheduleEvent();
    otherEvent.setId(UUID.randomUUID());
    otherEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherEvent.setDescription("Event description");
    otherEvent.setOriginalEventId(otherEvent.getId());
    otherEvent.setEventDate(LocalDate.of(2025, 9, 1));

    var termComment = new EventCommentView("Term note", "Author A", "1 January 2025 12:00:00", "");
    var wpaComment = new EventCommentView("WPA note", "Author B", "2 January 2025 12:00:00", "");
    var rateComment = new EventCommentView("Rate note", "Author C", "3 January 2025 12:00:00", "");
    var eventComment = new EventCommentView("Event note", "Author D", "4 January 2025 12:00:00", "");

    var commentsMap = Map.of(
        term.getId(), List.of(termComment),
        wpa.getId(), List.of(wpaComment),
        rate.getId(), List.of(rateComment),
        otherEvent.getId(), List.of(eventComment)
    );

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(commentsMap);

    var activities = List.of(wpa);
    when(workProgrammeActivityService.getWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities))
        .thenReturn(Map.of(wpa.getId(), wpaStatus));

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of());

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term))
        .thenReturn(List.of(wpa));
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());

    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term)).thenReturn(List.of(rate));

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term)).thenReturn(List.of(otherEvent));
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(rate.getId(), new StartEndDates(rate.getStartDate(), term.getEndDate())));

    var result = licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, allowedActions);

    assertThat(result).hasSize(1);
    var termView = result.getFirst();
    assertThat(termView.comments()).isEqualTo(List.of(termComment));

    var termEvents = termView.events();
    var resultWpaView = termEvents.stream()
        .filter(TimelineWorkProgrammeActivityView.class::isInstance)
        .map(e -> (TimelineWorkProgrammeActivityView) e)
        .findFirst().orElseThrow();
    assertThat(resultWpaView.comments()).isEqualTo(List.of(wpaComment));

    var resultRateView = termEvents.stream()
        .filter(TimelineRateView.class::isInstance)
        .map(e -> (TimelineRateView) e)
        .findFirst().orElseThrow();
    assertThat(resultRateView.comments()).isEqualTo(List.of(rateComment));

    var resultEventView = termEvents.stream()
        .filter(TimelineOtherScheduleEventView.class::isInstance)
        .map(e -> (TimelineOtherScheduleEventView) e)
        .findFirst().orElseThrow();
    assertThat(resultEventView.comments()).isEqualTo(List.of(eventComment));
  }

  @Test
  void getEditableLicenceScheduleEventViews_whenLicenceHasEndedOnDate_thenProgressDateUsesLicenceEndDate() {
    licence.setEndDate(LocalDate.of(2027, 6, 1));

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setOriginalEventId(term.getId());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2026, 1, 1));
    term.setEndDate(LocalDate.of(2026, 12, 31));

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getPhasesByTerm(term)).thenReturn(List.of());

    when(workProgrammeActivityService.getWorkProgrammeActivitiesByDateRangeFor(term)).thenReturn(List.of());
    when(workProgrammeActivityService.getWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());

    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term)).thenReturn(List.of());

    when(otherScheduleEventService.getScheduleEventsByDateRangeFor(term)).thenReturn(List.of());
    when(otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());
    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of());

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var result = licenceScheduleTimelineService.getEditableLicenceScheduleEventViews(licenceScheduleDetail, form, List.of());

    assertThat(result).hasSize(1);
    var termView = result.getFirst();
    assertThat(termView.showStartDateProgress()).isTrue();
    assertThat(termView.showEndDateProgress()).isTrue();
  }

  @Test
  void getAllowedEventActionsForUser_whenUserHasBothRoles_thenBothActionsReturned() {
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);

    assertThat(licenceScheduleTimelineService.getAllowedEventActionsForUser(userDetail))
        .containsExactlyInAnyOrder(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);
  }

  @Test
  void getAllowedEventActionsForUser_whenUserHasOnlyScheduleAdminRole_thenOnlyEditScheduleEventsReturned() {
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(true);
    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    assertThat(licenceScheduleTimelineService.getAllowedEventActionsForUser(userDetail))
        .containsExactly(ScheduleEventAction.EDIT_SCHEDULE_EVENTS);
  }

  @Test
  void getAllowedEventActionsForUser_whenUserHasOnlyWorkProgrammeAdminRole_thenOnlyEditWorkProgrammeReturned() {
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);
    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(true);

    assertThat(licenceScheduleTimelineService.getAllowedEventActionsForUser(userDetail))
        .containsExactly(ScheduleEventAction.EDIT_WORK_PROGRAMME);
  }

  @Test
  void getAllowedEventActionsForUser_whenUserHasNoRoles_thenNoActionsReturned() {
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);
    when(teamQueryService.userHasAtLeastOneRoleIn(userDetail.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)))
        .thenReturn(false);

    assertThat(licenceScheduleTimelineService.getAllowedEventActionsForUser(userDetail))
        .isEmpty();
  }
}
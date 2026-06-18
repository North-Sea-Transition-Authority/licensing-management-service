package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setEventReference(midPhaseActivity.getEventReference());
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
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
        List.of()
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setEventReference(endOfPhaseActivity.getEventReference());
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
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
        List.of()
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setEventReference(midTerm2Activity.getEventReference());
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
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
        List.of()
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setEventReference(endOfTerm2Activity.getEventReference());
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
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
        List.of()
    );

    var midPhaseEventRef = new EventReference();
    midPhaseEventRef.setId(UUID.randomUUID());
    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));
    midPhaseEvent.setEventReference(midPhaseEventRef);

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
        List.of()
    );

    var endOfPhaseEventRef = new EventReference();
    endOfPhaseEventRef.setId(UUID.randomUUID());
    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");
    endOfPhaseEvent.setEventReference(endOfPhaseEventRef);

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
        List.of()
    );

    var midTerm2EventRef = new EventReference();
    midTerm2EventRef.setId(UUID.randomUUID());
    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));
    midTerm2Event.setEventReference(midTerm2EventRef);

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
        List.of()
    );

    var endOfTerm2EventRef = new EventReference();
    endOfTerm2EventRef.setId(UUID.randomUUID());
    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");
    endOfTerm2Event.setEventReference(endOfTerm2EventRef);

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
        List.of()
    );

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

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
        List.of()
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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

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
        List.of()
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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getEventReference().getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getEventReference().getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getEventReference().getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getEventReference().getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setEventReference(midPhaseActivity.getEventReference());
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.IN_PROGRESS,
        List.of()
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setEventReference(endOfPhaseActivity.getEventReference());
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setEventReference(midTerm2Activity.getEventReference());
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setEventReference(endOfTerm2Activity.getEventReference());
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var midPhaseEventRef = new EventReference();
    midPhaseEventRef.setId(UUID.randomUUID());
    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));
    midPhaseEvent.setEventReference(midPhaseEventRef);

    var midPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        "",
        List.of()
    );

    var endOfPhaseEventRef = new EventReference();
    endOfPhaseEventRef.setId(UUID.randomUUID());
    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");
    endOfPhaseEvent.setEventReference(endOfPhaseEventRef);

    var endOfPhaseEventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        List.of()
    );

    var midTerm2EventRef = new EventReference();
    midTerm2EventRef.setId(UUID.randomUUID());
    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));
    midTerm2Event.setEventReference(midTerm2EventRef);

    var midTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        "",
        List.of()
    );

    var endOfTerm2EventRef = new EventReference();
    endOfTerm2EventRef.setId(UUID.randomUUID());
    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");
    endOfTerm2Event.setEventReference(endOfTerm2EventRef);

    var endOfTerm2EventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        List.of()
    );

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        "",
        "",
        "",
        List.of()
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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        "",
        "",
        "",
        List.of()
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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getEventReference().getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getEventReference().getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getEventReference().getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getEventReference().getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setEventReference(midPhaseActivity.getEventReference());
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
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
        List.of()
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setEventReference(endOfPhaseActivity.getEventReference());
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
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
        List.of()
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setEventReference(midTerm2Activity.getEventReference());
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
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
        List.of()
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setEventReference(endOfTerm2Activity.getEventReference());
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
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
        List.of()
    );

    var midPhaseEventRef = new EventReference();
    midPhaseEventRef.setId(UUID.randomUUID());
    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));
    midPhaseEvent.setEventReference(midPhaseEventRef);

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
        List.of()
    );

    var endOfPhaseEventRef = new EventReference();
    endOfPhaseEventRef.setId(UUID.randomUUID());
    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");
    endOfPhaseEvent.setEventReference(endOfPhaseEventRef);

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
        List.of()
    );

    var midTerm2EventRef = new EventReference();
    midTerm2EventRef.setId(UUID.randomUUID());
    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));
    midTerm2Event.setEventReference(midTerm2EventRef);

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
        List.of()
    );

    var endOfTerm2EventRef = new EventReference();
    endOfTerm2EventRef.setId(UUID.randomUUID());
    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");
    endOfTerm2Event.setEventReference(endOfTerm2EventRef);

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
        List.of()
    );

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(
        ScheduleEventType.WORK_PROGRAMME_ACTIVITY.name(),
        ScheduleEventType.OTHER.name()
    ));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getEventReference().getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getEventReference().getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getEventReference().getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getEventReference().getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setEventReference(midPhaseActivity.getEventReference());
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setEventReference(endOfPhaseActivity.getEventReference());
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setEventReference(midTerm2Activity.getEventReference());
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setEventReference(endOfTerm2Activity.getEventReference());
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midPhaseEventRef = new EventReference();
    midPhaseEventRef.setId(UUID.randomUUID());
    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));
    midPhaseEvent.setEventReference(midPhaseEventRef);

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
        List.of()
    );

    var endOfPhaseEventRef = new EventReference();
    endOfPhaseEventRef.setId(UUID.randomUUID());
    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");
    endOfPhaseEvent.setEventReference(endOfPhaseEventRef);

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
        List.of()
    );

    var midTerm2EventRef = new EventReference();
    midTerm2EventRef.setId(UUID.randomUUID());
    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));
    midTerm2Event.setEventReference(midTerm2EventRef);

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
        List.of()
    );

    var endOfTerm2EventRef = new EventReference();
    endOfTerm2EventRef.setId(UUID.randomUUID());
    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");
    endOfTerm2Event.setEventReference(endOfTerm2EventRef);

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
        List.of()
    );

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

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
        List.of()
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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

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
        List.of()
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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(
        ScheduleEventType.RATE.name(),
        ScheduleEventType.OTHER.name()
    ));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getEventReference().getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getEventReference().getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getEventReference().getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getEventReference().getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setEventReference(midPhaseActivity.getEventReference());
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
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
        List.of()
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setEventReference(endOfPhaseActivity.getEventReference());
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
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
        List.of()
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setEventReference(midTerm2Activity.getEventReference());
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
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
        List.of()
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setEventReference(endOfTerm2Activity.getEventReference());
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
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
        List.of()
    );

    var midPhaseEventRef = new EventReference();
    midPhaseEventRef.setId(UUID.randomUUID());
    var midPhaseEvent = new OtherScheduleEvent();
    midPhaseEvent.setId(UUID.randomUUID());
    midPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midPhaseEvent.setDescription("description");
    midPhaseEvent.setEventDate(LocalDate.of(2025, 2, 1));
    midPhaseEvent.setEventReference(midPhaseEventRef);

    var endOfPhaseEventRef = new EventReference();
    endOfPhaseEventRef.setId(UUID.randomUUID());
    var endOfPhaseEvent = new OtherScheduleEvent();
    endOfPhaseEvent.setId(UUID.randomUUID());
    endOfPhaseEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfPhaseEvent.setDescription("description");
    endOfPhaseEvent.setEventReference(endOfPhaseEventRef);

    var midTerm2EventRef = new EventReference();
    midTerm2EventRef.setId(UUID.randomUUID());
    var midTerm2Event = new OtherScheduleEvent();
    midTerm2Event.setId(UUID.randomUUID());
    midTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    midTerm2Event.setDescription("description");
    midTerm2Event.setEventDate(LocalDate.of(2026, 2, 1));
    midTerm2Event.setEventReference(midTerm2EventRef);

    var endOfTerm2EventRef = new EventReference();
    endOfTerm2EventRef.setId(UUID.randomUUID());
    var endOfTerm2Event = new OtherScheduleEvent();
    endOfTerm2Event.setId(UUID.randomUUID());
    endOfTerm2Event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    endOfTerm2Event.setDescription("description");
    endOfTerm2Event.setEventReference(endOfTerm2EventRef);

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

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
        List.of()
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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

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
        List.of()
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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(List.of(
        ScheduleEventType.RATE.name(),
        ScheduleEventType.WORK_PROGRAMME_ACTIVITY.name()
    ));

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getEventReference().getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getEventReference().getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getEventReference().getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getEventReference().getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(phase)).thenReturn(List.of(midPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term2)).thenReturn(List.of(midTerm2Event));

    when(otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term2, OtherScheduleEventDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Event));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityStatus = new WorkProgrammeActivityStatus();
    midPhaseActivityStatus.setEventReference(midPhaseActivity.getEventReference());
    midPhaseActivityStatus.setStatus(WorkProgrammeStatus.IN_PROGRESS);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(midPhaseActivity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(midPhaseActivity.getEventReference().getId(), null)),
        WorkProgrammeStatus.IN_PROGRESS,
        List.of()
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityStatus = new WorkProgrammeActivityStatus();
    endOfPhaseActivityStatus.setEventReference(endOfPhaseActivity.getEventReference());
    endOfPhaseActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(endOfPhaseActivity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(endOfPhaseActivity.getEventReference().getId(), null)),
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    midTerm2ActivityStatus.setEventReference(midTerm2Activity.getEventReference());
    midTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(midTerm2Activity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(midTerm2Activity.getEventReference().getId(), null)),
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityStatus = new WorkProgrammeActivityStatus();
    endOfTerm2ActivityStatus.setEventReference(endOfTerm2Activity.getEventReference());
    endOfTerm2ActivityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
            .renderStatusUpdatePage(endOfTerm2Activity.getId(), null)),
        ReverseRouter.route(on(EventCommentController.class)
            .renderAddCommentForm(endOfTerm2Activity.getEventReference().getId(), null)),
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        "",
        "",
        "",
        List.of()
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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        "",
        "",
        "",
        List.of()
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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var user = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), Set.of(Role.SCHEDULE_ADMINISTRATOR)))
        .thenReturn(false);
    when(teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR)))
        .thenReturn(true);
    when(teamQueryService.userIsInRegulatorTeam(user.wuaId())).thenReturn(true);

    var activities = List.of(midPhaseActivity, endOfPhaseActivity, midTerm2Activity, endOfTerm2Activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities)).thenReturn(
        Map.of(
            midPhaseActivity.getEventReference().getId(), midPhaseActivityStatus,
            endOfPhaseActivity.getEventReference().getId(), endOfPhaseActivityStatus,
            midTerm2Activity.getEventReference().getId(), midTerm2ActivityStatus,
            endOfTerm2Activity.getEventReference().getId(), endOfTerm2ActivityStatus
        )
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));
    var midPhaseActivityRef = new EventReference();
    midPhaseActivityRef.setId(UUID.randomUUID());
    midPhaseActivity.setEventReference(midPhaseActivityRef);

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        "",
        "",
        "",
        "",
        null,
        List.of()
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");
    var endOfPhaseActivityRef = new EventReference();
    endOfPhaseActivityRef.setId(UUID.randomUUID());
    endOfPhaseActivity.setEventReference(endOfPhaseActivityRef);

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        null,
        List.of()
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));
    var midTerm2ActivityRef = new EventReference();
    midTerm2ActivityRef.setId(UUID.randomUUID());
    midTerm2Activity.setEventReference(midTerm2ActivityRef);

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        "",
        "",
        "",
        "",
        null,
        List.of()
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");
    var endOfTerm2ActivityRef = new EventReference();
    endOfTerm2ActivityRef.setId(UUID.randomUUID());
    endOfTerm2Activity.setEventReference(endOfTerm2ActivityRef);

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
        "description",
        null,
        "",
        "",
        "",
        "",
        "",
        null,
        List.of()
    );

    var phaseRef = new EventReference();
    phaseRef.setId(UUID.randomUUID());
    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setEventReference(phaseRef);

    var phaseRateRef = new EventReference();
    phaseRateRef.setId(UUID.randomUUID());
    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());
    phaseRate.setEventReference(phaseRateRef);

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025 to 31 December 2025",
        "£1.00",
        "",
        "",
        "",
        List.of()
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
        List.of()
    );

    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

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
        List.of()
    );

    var term2Ref = new EventReference();
    term2Ref.setId(UUID.randomUUID());
    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));
    term2.setEventReference(term2Ref);

    var term2RateRef = new EventReference();
    term2RateRef.setId(UUID.randomUUID());
    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());
    term2Rate.setEventReference(term2RateRef);

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026 to 31 December 2026",
        "£2.00",
        "",
        "",
        "",
        List.of()
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
        List.of()
    );

    var form = new TimelineFilterForm();
    form.setEventTypes(ScheduleEventType.getFilterDefaults());

    var user = ServiceUserDetailTestUtil.newBuilder().build();

    when(teamQueryService.userIsInRegulatorTeam(user.wuaId())).thenReturn(false);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(phase)).thenReturn(List.of(midPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term2)).thenReturn(List.of(midTerm2Activity));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByPhaseAndDateOption(phase, WorkProgrammeActivityDateOption.WITHIN_A_PHASE))
        .thenReturn(List.of(endOfPhaseActivity));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term2, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of(endOfTerm2Activity));

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

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
  void getEventsBeyondFinalTerm() {
    var firstTerm = new LicenceScheduleTerm();
    firstTerm.setTermType(TermType.INITIAL);

    var finalTermEndDate = LocalDate.of(2026, 1, 1);

    var finalTerm = new LicenceScheduleTerm();
    finalTerm.setTermType(TermType.SECOND);
    finalTerm.setEndDate(finalTermEndDate);

    var rateRef = new EventReference();
    rateRef.setId(UUID.randomUUID());
    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    rate.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    rate.setRentalRate(new BigDecimal("2.00"));
    rate.setStartDate(finalTermEndDate.plusYears(1));
    rate.setEventReference(rateRef);

    var rateView = new TimelineRateView(
        "Rate",
        rate.getStartDate(),
        "1 January 2027 to 31 December 2027",
        "£2.00",
        ReverseRouter.route(on(LicenceScheduleRateController.class)
            .renderUpdateLicenceScheduleRateForm(rate.getId())),
        ReverseRouter.route(on(LicenceScheduleRateDeletionController.class)
            .renderDeleteRatePage(rate.getId())),
        "",
        List.of()
    );

    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    activity.setDescription("description");
    activity.setDueDate(finalTermEndDate.plusYears(2));
    var activityRef = new EventReference();
    activityRef.setId(UUID.randomUUID());
    activity.setEventReference(activityRef);

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setEventReference(activity.getEventReference());
    activityStatus.setStatus(WorkProgrammeStatus.OPEN);

    var activityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
        "description",
        activity.getDueDate(),
        "1 January 2028",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(activity.getId(), null)),
        "",
        "",
        WorkProgrammeStatus.OPEN,
        List.of()
    );

    var eventRef = new EventReference();
    eventRef.setId(UUID.randomUUID());
    var event = new OtherScheduleEvent();
    event.setId(UUID.randomUUID());
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    event.setDescription("description");
    event.setEventDate(finalTermEndDate.plusYears(3));
    event.setEventReference(eventRef);

    var eventView = new TimelineOtherScheduleEventView(
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        "description",
        event.getEventDate(),
        "1 January 2029",
        ReverseRouter.route(on(OtherScheduleEventController.class)
            .renderUpdateEventForm(event.getId())),
        ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
            .renderDeleteEventPage(event.getId())),
        "",
        List.of()
    );

    var allowedActions = List.of(ScheduleEventAction.EDIT_SCHEDULE_EVENTS, ScheduleEventAction.EDIT_WORK_PROGRAMME);

    var activities = List.of(activity);

    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities))
        .thenReturn(Map.of(activity.getEventReference().getId(), activityStatus));

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(firstTerm, finalTerm));

    when(licenceScheduleRateService.getActiveRatesAfterDate(licenceScheduleDetail, finalTermEndDate))
        .thenReturn(List.of(rate));

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesAfterDate(licenceScheduleDetail, finalTermEndDate))
        .thenReturn(List.of(activity));

    when(otherScheduleEventService.getActiveEventsAfterDate(licenceScheduleDetail, finalTermEndDate))
        .thenReturn(List.of(event));

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(Map.of());

    when(licenceScheduleCalculationService.calculateRateEndDatesForDisplay(licenceScheduleDetail))
        .thenReturn(Map.of(
            rate.getId(), new StartEndDates(rate.getStartDate(), LocalDate.of(2027, 12, 31))
        ));

    assertThat(licenceScheduleTimelineService.getEventsBeyondFinalTerm(licenceScheduleDetail, allowedActions))
        .isEqualTo(List.of(rateView, activityView, eventView));
  }

  @Test
  void getEditableLicenceScheduleEventViews_commentsArePopulatedOnViews() {
    var termRef = new EventReference();
    termRef.setId(UUID.randomUUID());
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));
    term.setEventReference(termRef);

    var wpaRef = new EventReference();
    wpaRef.setId(UUID.randomUUID());
    var wpa = new WorkProgrammeActivity();
    wpa.setId(UUID.randomUUID());
    wpa.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    wpa.setDescription("WPA description");
    wpa.setDueDate(LocalDate.of(2025, 6, 1));
    wpa.setEventReference(wpaRef);

    var wpaStatus = new WorkProgrammeActivityStatus();
    wpaStatus.setEventReference(wpaRef);
    wpaStatus.setStatus(WorkProgrammeStatus.OPEN);

    var rateRef = new EventReference();
    rateRef.setId(UUID.randomUUID());
    var rate = new LicenceScheduleRate();
    rate.setId(UUID.randomUUID());
    rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    rate.setLicenceScheduleTerm(term);
    rate.setRentalRate(new BigDecimal("3.00"));
    rate.setStartDate(LocalDate.of(2025, 1, 1));
    rate.setEventReference(rateRef);

    var otherEventRef = new EventReference();
    otherEventRef.setId(UUID.randomUUID());
    var otherEvent = new OtherScheduleEvent();
    otherEvent.setId(UUID.randomUUID());
    otherEvent.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    otherEvent.setDescription("Event description");
    otherEvent.setEventDate(LocalDate.of(2025, 9, 1));
    otherEvent.setEventReference(otherEventRef);

    var termComment = new EventCommentView("Term note", "Author A", "1 January 2025 12:00:00", "");
    var wpaComment = new EventCommentView("WPA note", "Author B", "2 January 2025 12:00:00", "");
    var rateComment = new EventCommentView("Rate note", "Author C", "3 January 2025 12:00:00", "");
    var eventComment = new EventCommentView("Event note", "Author D", "4 January 2025 12:00:00", "");

    var commentsMap = Map.of(
        termRef.getId(), List.of(termComment),
        wpaRef.getId(), List.of(wpaComment),
        rateRef.getId(), List.of(rateComment),
        otherEventRef.getId(), List.of(eventComment)
    );

    when(eventCommentService.getEventCommentViewsForSchedule(licenceScheduleDetail.getLicenceSchedule()))
        .thenReturn(commentsMap);

    var activities = List.of(wpa);
    when(workProgrammeActivityService.getActiveWorkProgrammeActivities(licenceScheduleDetail)).thenReturn(activities);
    when(workProgrammeActivityStatusService.getLatestStatusesFor(activities))
        .thenReturn(Map.of(wpaRef.getId(), wpaStatus));

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail))
        .thenReturn(List.of(term));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of());

    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByDateRangeFor(term))
        .thenReturn(List.of(wpa));
    when(workProgrammeActivityService.getActiveWorkProgrammeActivitiesByTermAndDateOption(term, WorkProgrammeActivityDateOption.WITHIN_A_TERM))
        .thenReturn(List.of());

    when(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term)).thenReturn(List.of(rate));

    when(otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term)).thenReturn(List.of(otherEvent));
    when(otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.WITHIN_A_TERM))
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
}
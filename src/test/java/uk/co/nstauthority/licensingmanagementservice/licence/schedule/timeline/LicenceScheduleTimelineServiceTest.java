package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

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
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType())).thenReturn(true);

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(
            TimelineSummaryCardView::licenceStartDate,
            TimelineSummaryCardView::showRoundIssuedOn,
            TimelineSummaryCardView::roundIssuedOn,
            TimelineSummaryCardView::status
        )
        .containsExactly(
            DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
            true,
            licence.getRoundIssuedOn(),
            licence.getStatus().getDisplayText()
        );
  }

  @Test
  void getLicenceScheduleTimelineActions() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasRentalRate(licence.getType())).thenReturn(true);

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
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleTimelineActions_actionsDisabled() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(false);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(false);
    when(licenceTypeRulesResolver.hasRentalRate(licence.getType())).thenReturn(false);

    var expectedResult = List.of(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleEventViews() {
    var midPhaseActivity = new WorkProgrammeActivity();
    midPhaseActivity.setId(UUID.randomUUID());
    midPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    midPhaseActivity.setDescription("description");
    midPhaseActivity.setDueDate(LocalDate.of(2025, 2, 1));

    var midPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        "description",
        LocalDate.of(2025, 2, 1),
        "1 February 2025",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midPhaseActivity.getId(), null))
    );

    var endOfPhaseActivity = new WorkProgrammeActivity();
    endOfPhaseActivity.setId(UUID.randomUUID());
    endOfPhaseActivity.setCategory(WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL);
    endOfPhaseActivity.setDescription("description");

    var endOfPhaseActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfPhaseActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfPhaseActivity.getId(), null))
    );

    var midTerm2Activity = new WorkProgrammeActivity();
    midTerm2Activity.setId(UUID.randomUUID());
    midTerm2Activity.setCategory(WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT);
    midTerm2Activity.setDescription("description");
    midTerm2Activity.setDueDate(LocalDate.of(2026, 2, 1));

    var midTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.EARLY_RISK_ASSESSMENT.getDisplayName(),
        "description",
        LocalDate.of(2026, 2, 1),
        "1 February 2026",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(midTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(midTerm2Activity.getId(), null))
    );

    var endOfTerm2Activity = new WorkProgrammeActivity();
    endOfTerm2Activity.setId(UUID.randomUUID());
    endOfTerm2Activity.setCategory(WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA);
    endOfTerm2Activity.setDescription("description");

    var endOfTerm2ActivityView = new TimelineWorkProgrammeActivityView(
        WorkProgrammeActivityCategory.NEW_SHOOT_2_D_SEISMIC_DATA.getDisplayName(),
        "description",
        null,
        "",
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(endOfTerm2Activity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(endOfTerm2Activity.getId(), null))
    );

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseRate = new LicenceScheduleRate();
    phaseRate.setId(UUID.randomUUID());
    phaseRate.setRateDefinitionOption(RateDefinitionOption.PHASE);
    phaseRate.setLicenceSchedulePhase(phase);
    phaseRate.setRentalRate(new BigDecimal("1.00"));
    phaseRate.setStartDate(phase.getStartDate());

    var phaseRateView = new TimelineRateView(
        "Phase A rate",
        phase.getStartDate(),
        "1 January 2025",
        "£1.00",
        "",
        ""
    );

    var phaseView = new TimelinePhaseView(
        List.of(phaseRateView, midPhaseActivityView),
        List.of(endOfPhaseActivityView),
        PhaseType.PHASE_A,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(phase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(phase.getId()))
    );

    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
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
        true
    );

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setLicenceScheduleDetail(licenceScheduleDetail);
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var term2Rate = new LicenceScheduleRate();
    term2Rate.setId(UUID.randomUUID());
    term2Rate.setRateDefinitionOption(RateDefinitionOption.TERM);
    term2Rate.setLicenceScheduleTerm(term2);
    term2Rate.setRentalRate(new BigDecimal("2.00"));
    term2Rate.setStartDate(term2.getStartDate());

    var term2RateView = new TimelineRateView(
        "Second Term rate",
        term2.getStartDate(),
        "1 January 2026",
        "£2.00",
        "",
        ""
    );

    var termView2 = new TimelineTermView(
        List.of(term2RateView, midTerm2ActivityView),
        List.of(endOfTerm2ActivityView),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term2.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term2.getId())),
        false
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

    when(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).thenReturn(List.of(phaseRate));
    when(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term2)).thenReturn(List.of(term2Rate));

    assertThat(licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }
}
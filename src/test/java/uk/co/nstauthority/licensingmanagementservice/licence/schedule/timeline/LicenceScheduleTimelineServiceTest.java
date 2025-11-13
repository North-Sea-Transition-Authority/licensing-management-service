package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

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
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityController;
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

  @InjectMocks
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
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

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(
            TimelineSummaryCardView::licenceStartDate,
            TimelineSummaryCardView::roundIssuedOn,
            TimelineSummaryCardView::status
        )
        .containsExactly(
            DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
            licence.getRoundIssuedOn(),
            licence.getStatus().getDisplayText()
        );
  }

  @Test
  void getLicenceScheduleTimelineActions() {
    when(licenceTypeRulesResolver.arePhasesCaptured(licence.getType())).thenReturn(true);
    when(licenceTypeRulesResolver.hasWorkProgramme(licence.getType())).thenReturn(true);

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
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleEventViews() {
    var term = new LicenceScheduleTerm();
    term.setId(UUID.randomUUID());
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var phase = new LicenceSchedulePhase();
    phase.setId(UUID.randomUUID());
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));

    var phaseView = new TimelinePhaseView(
        List.of(),
        PhaseType.PHASE_A,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderUpdatePhaseForm(phase.getId())),
        ReverseRouter.route(on(LicenceSchedulePhaseDeletionController.class).renderDeletePhasePage(phase.getId()))
    );

    var term2 = new LicenceScheduleTerm();
    term2.setId(UUID.randomUUID());
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var termView = new TimelineTermView(
        List.of(phaseView),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term.getId())),
        true
    );

    var termView2 = new TimelineTermView(
        List.of(),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        ReverseRouter.route(on(LicenceScheduleTermController.class).renderUpdateTermForm(term2.getId())),
        ReverseRouter.route(on(LicenceScheduleTermDeletionController.class).renderDeleteTermPage(term2.getId())),
        false
    );

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));
    when(licenceSchedulePhaseService.getActivePhasesByTerm(term)).thenReturn(List.of(phase));

    assertThat(licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }
}
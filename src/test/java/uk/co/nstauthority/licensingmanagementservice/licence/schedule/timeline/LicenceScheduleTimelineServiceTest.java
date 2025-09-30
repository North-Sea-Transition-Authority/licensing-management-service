package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTimelineServiceTest {

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private LicenceTypeFeatureService licenceTypeFeatureService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @InjectMocks
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  private Licence licence;

  private LicenceScheduleDetail licenceScheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withRoundIssuedOn("1")
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
            TimelineSummaryCardView::roundIssuedOn
        )
        .containsExactly(
            DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate()),
            licence.getRoundIssuedOn()
        );
  }

  @Test
  void getLicenceScheduleTimelineActions() {
    when(licenceTypeFeatureService.arePhasesCaptured(licence.getType())).thenReturn(true);

    var expectedResult = List.of(
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        ),
        new TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_PHASE,
            ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderAddNewPhaseForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }

  @Test
  void getLicenceScheduleEventViews() {
    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    var term2 = new LicenceScheduleTerm();
    term2.setTermType(TermType.SECOND);
    term2.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term2.setStartDate(LocalDate.of(2026, 1, 1));
    term2.setEndDate(LocalDate.of(2026, 12, 31));

    var termView = new TimelineTermView(
        List.of(),
        TermType.INITIAL,
        "1 January 2025 to 31 December 2025 (1 year)",
        "31 December 2025",
        "#",
        "#"
    );

    var termView2 = new TimelineTermView(
        List.of(),
        TermType.SECOND,
        "1 January 2026 to 31 December 2026 (1 year)",
        "31 December 2026",
        "#",
        "#"
    );

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term, term2));

    assertThat(licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(List.of(termView, termView2));
  }
}
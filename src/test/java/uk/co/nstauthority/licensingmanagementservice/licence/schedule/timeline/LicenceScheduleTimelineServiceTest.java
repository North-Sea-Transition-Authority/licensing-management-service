package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeFeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTimelineServiceTest {

  @Mock
  private LicenceStartDateService licenceStartDateService;

  @Mock
  private LicenceTypeFeatureService licenceTypeFeatureService;

  @InjectMocks
  private LicenceScheduleTimelineService licenceScheduleTimelineService;

  @Test
  void getTimelineSummaryCardView() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setStartDate(LocalDate.of(2025, 1, 1));

    when(licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail)).thenReturn(licenceStartDate);

    assertThat(licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .extracting(
            TimelineSummaryCardView::licenceStartDate
        )
        .isEqualTo(
            DateFormatUtil.convertToDisplayText(licenceStartDate.getStartDate())
        );
  }

  @Test
  void getLicenceScheduleTimelineActions() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceTypeFeatureService.arePhasesCaptured(licence.getType())).thenReturn(true);

    var expectedResult = List.of(
        new LicenceScheduleTimelineService.TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_TERM,
            ReverseRouter.route(on(LicenceScheduleTermController.class).renderAddNewTermForm(licenceScheduleDetail.getId(), null))
        ),
        new LicenceScheduleTimelineService.TimelineActionView(
            LicenceScheduleTimelineAction.ADD_A_PHASE,
            ReverseRouter.route(on(LicenceSchedulePhaseController.class).renderAddNewPhaseForm(licenceScheduleDetail.getId(), null))
        )
    );

    assertThat(licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .usingRecursiveComparison()
        .isEqualTo(expectedResult);
  }
}
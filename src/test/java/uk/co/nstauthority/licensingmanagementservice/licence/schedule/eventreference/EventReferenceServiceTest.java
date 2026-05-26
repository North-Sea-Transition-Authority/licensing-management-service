package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@ExtendWith(MockitoExtension.class)
class EventReferenceServiceTest {

  @Mock
  private EventReferenceRepository eventReferenceRepository;

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceScheduleRateService licenceScheduleRateService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @InjectMocks
  private EventReferenceService eventReferenceService;

  @Captor
  private ArgumentCaptor<EventReference> eventReferenceArgumentCaptor;

  private EventReference eventReference;

  private LicenceScheduleDetail scheduleDetail;

  @BeforeEach
  void setUp() {
    var licence = LicenceTestUtil.builder().withId(1).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    eventReference = new EventReference();
    eventReference.setId(UUID.randomUUID());
    eventReference.setLicenceSchedule(licenceSchedule);

    scheduleDetail = new LicenceScheduleDetail();
  }

  @Test
  void createEventReference() {
    var licenceSchedule = new LicenceSchedule();

    eventReferenceService.createEventReference(licenceSchedule);

    verify(eventReferenceRepository).save(eventReferenceArgumentCaptor.capture());

    assertThat(eventReferenceArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }

  @Test
  void getEventReferenceEventCaption_term() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(any(), eq(LicenceScheduleDetailStatus.ACTIVE)))
        .thenReturn(scheduleDetail);
    var term = LicenceScheduleTermTestUtil.builder()
        .withTermType(TermType.INITIAL)
        .build();

    when(licenceScheduleTermService.getTermByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference))
        .thenReturn(term);

    var result = eventReferenceService.getEventReferenceEventCaption(eventReference, ScheduleEventType.TERM);

    assertThat(result).isEqualTo(TermType.INITIAL.getDisplayName());
  }

  @Test
  void getEventReferenceEventCaption_phase() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(any(), eq(LicenceScheduleDetailStatus.ACTIVE)))
        .thenReturn(scheduleDetail);
    var phase = LicenceSchedulePhaseTestUtil.builder()
        .withPhaseType(PhaseType.PHASE_A)
        .build();

    when(licenceSchedulePhaseService.getPhaseByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference))
        .thenReturn(phase);

    var result = eventReferenceService.getEventReferenceEventCaption(eventReference, ScheduleEventType.PHASE);

    assertThat(result).isEqualTo(PhaseType.PHASE_A.getDisplayName());
  }

  @Test
  void getEventReferenceEventCaption_rate() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(any(), eq(LicenceScheduleDetailStatus.ACTIVE)))
        .thenReturn(scheduleDetail);
    var rate = new LicenceScheduleRate();
    rate.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);

    when(licenceScheduleRateService.getRateByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference))
        .thenReturn(rate);

    var result = eventReferenceService.getEventReferenceEventCaption(eventReference, ScheduleEventType.RATE);

    assertThat(result).isEqualTo("Rate");
  }

  @Test
  void getEventReferenceEventCaption_workProgrammeActivity() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(any(), eq(LicenceScheduleDetailStatus.ACTIVE)))
        .thenReturn(scheduleDetail);
    var activity = new WorkProgrammeActivity();
    activity.setCategory(WorkProgrammeActivityCategory.DRILLING_WELL);

    when(workProgrammeActivityService.getWorkProgrammeActivityByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference))
        .thenReturn(activity);

    var result = eventReferenceService.getEventReferenceEventCaption(eventReference, ScheduleEventType.WORK_PROGRAMME_ACTIVITY);

    assertThat(result).isEqualTo(WorkProgrammeActivityCategory.DRILLING_WELL.getDisplayName());
  }

  @Test
  void getEventReferenceEventCaption_other() {
    when(licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(any(), eq(LicenceScheduleDetailStatus.ACTIVE)))
        .thenReturn(scheduleDetail);
    var event = new OtherScheduleEvent();
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);

    when(otherScheduleEventService.getOtherScheduleEventByScheduleDetailAndEventReferenceOrThrow(scheduleDetail, eventReference))
        .thenReturn(event);

    var result = eventReferenceService.getEventReferenceEventCaption(eventReference, ScheduleEventType.OTHER);

    assertThat(result).isEqualTo(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName());
  }
}

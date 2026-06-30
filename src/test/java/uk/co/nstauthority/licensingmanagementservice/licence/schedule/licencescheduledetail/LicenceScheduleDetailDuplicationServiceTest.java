package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationService;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleDetailDuplicationServiceTest {

  @Mock
  private List<DuplicationSource<LicenceScheduleDetail>> duplicationSources;

  @Mock
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

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

  @Mock
  private DuplicationService duplicationService;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService;

  @Captor
  private ArgumentCaptor<LicenceScheduleDetail> licenceScheduleDetailArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceSchedulePhase>> licenceSchedulePhaseArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceScheduleRate>> licenceScheduleRateArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<WorkProgrammeActivity>> workProgrammeActivityArgumentCaptor;

  @Captor
  private ArgumentCaptor<List<OtherScheduleEvent>> otherScheduleEventArgumentCaptor;

  private static final Instant FIXED_INSTANT = Instant.now();

  @Test
  void createNewDraftLicenceScheduleDetailVersion() {
    var schedule = new LicenceSchedule();

    var oldDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(schedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();

    var newDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(schedule)
        .withStatus(LicenceScheduleDetailStatus.DRAFT)
        .withCreatedInstant(FIXED_INSTANT)
        .build();

    when(clock.instant()).thenReturn(FIXED_INSTANT);

    licenceScheduleDetailDuplicationService.createNewDraftLicenceScheduleDetailVersion(oldDetail);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    verify(duplicationService).duplicateChildEntities(eq(oldDetail), any(LicenceScheduleDetail.class), eq(duplicationSources));

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(any(LicenceScheduleDetail.class));

    assertThat(licenceScheduleDetailArgumentCaptor.getValue()).extracting(
        LicenceScheduleDetail::getLicenceSchedule,
        LicenceScheduleDetail::getStatus,
        LicenceScheduleDetail::getCreatedInstant
    ).containsExactly(
        newDetail.getLicenceSchedule(),
        newDetail.getStatus(),
        newDetail.getCreatedInstant()
    );
  }

  @Test
  void relinkTermsAndPhases() {
    var oldDetail = new LicenceScheduleDetail();
    var newDetail = new LicenceScheduleDetail();

    var oldTerm = new LicenceScheduleTerm();
    oldTerm.setLicenceScheduleDetail(oldDetail);
    var oldTermRef = new EventReference();
    oldTermRef.setId(UUID.randomUUID());
    oldTerm.setEventReference(oldTermRef);

    var newTerm = new LicenceScheduleTerm();
    newTerm.setLicenceScheduleDetail(newDetail);
    newTerm.setEventReference(oldTerm.getEventReference());

    var oldPhase = new LicenceSchedulePhase();
    oldPhase.setLicenceScheduleDetail(oldDetail);
    var oldPhaseRef = new EventReference();
    oldPhaseRef.setId(UUID.randomUUID());
    oldPhase.setEventReference(oldPhaseRef);

    var newPhase = new LicenceSchedulePhase();
    newPhase.setLicenceScheduleDetail(newDetail);
    newPhase.setEventReference(oldPhase.getEventReference());
    newPhase.setLicenceScheduleTerm(oldTerm);
    
    var termLinkedRate = new LicenceScheduleRate();
    termLinkedRate.setLicenceScheduleDetail(newDetail);
    termLinkedRate.setLicenceScheduleTerm(oldTerm);

    var phaseLinkedActivity = new WorkProgrammeActivity();
    phaseLinkedActivity.setLicenceScheduleDetail(newDetail);
    phaseLinkedActivity.setLicenceSchedulePhase(oldPhase);

    var termLinkedEvent = new OtherScheduleEvent();
    termLinkedEvent.setLicenceScheduleDetail(newDetail);
    termLinkedEvent.setLicenceScheduleTerm(oldTerm);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(oldDetail)).thenReturn(List.of(oldTerm));
    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(newDetail)).thenReturn(List.of(newTerm));

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(oldDetail)).thenReturn(List.of(oldPhase));
    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(newDetail)).thenReturn(List.of(newPhase));

    when(licenceScheduleRateService.getLicenceScheduleRates(newDetail)).thenReturn(List.of(termLinkedRate));
    when(workProgrammeActivityService.getWorkProgrammeActivities(newDetail)).thenReturn(List.of(phaseLinkedActivity));
    when(otherScheduleEventService.getOtherScheduleEvents(newDetail)).thenReturn(List.of(termLinkedEvent));

    licenceScheduleDetailDuplicationService.relinkTermsAndPhases(oldDetail, newDetail);

    verify(licenceSchedulePhaseService).saveLicenceSchedulePhases(licenceSchedulePhaseArgumentCaptor.capture());
    verify(licenceScheduleRateService).saveLicenceScheduleRates(licenceScheduleRateArgumentCaptor.capture());
    verify(workProgrammeActivityService).saveWorkProgrammeActivities(workProgrammeActivityArgumentCaptor.capture());
    verify(otherScheduleEventService).saveScheduleEvents(otherScheduleEventArgumentCaptor.capture());

    assertThat(licenceSchedulePhaseArgumentCaptor.getValue().getFirst()).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getLicenceScheduleTerm
    ).containsExactly(
        newDetail,
        newTerm
    );

    assertThat(licenceScheduleRateArgumentCaptor.getValue().getFirst()).extracting(
        LicenceScheduleRate::getLicenceScheduleDetail,
        LicenceScheduleRate::getLicenceScheduleTerm
    ).containsExactly(
        newDetail,
        newTerm
    );

    assertThat(workProgrammeActivityArgumentCaptor.getValue().getFirst()).extracting(
        WorkProgrammeActivity::getLicenceScheduleDetail,
        WorkProgrammeActivity::getLicenceSchedulePhase
    ).containsExactly(
        newDetail,
        newPhase
    );

    assertThat(otherScheduleEventArgumentCaptor.getValue().getFirst()).extracting(
        OtherScheduleEvent::getLicenceScheduleDetail,
        OtherScheduleEvent::getLicenceScheduleTerm
    ).containsExactly(
        newDetail,
        newTerm
    );
  }

}
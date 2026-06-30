package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseFormServiceTest {

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @Mock
  private EventReferenceService eventReferenceService;

  @Mock
  private EventCommentService eventCommentService;

  @InjectMocks
  private LicenceSchedulePhaseFormService licenceSchedulePhaseFormService;

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Captor
  private ArgumentCaptor<LicenceSchedulePhase> licenceSchedulePhaseArgumentCaptor;

  @Test
  void savePhaseFromForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceSchedule = new LicenceSchedule();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setYears("1");
    form.getPhaseDuration().setMonths("0");
    form.getPhaseDuration().setDays("0");
    form.setComments("comments");

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));
    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceSchedule, ScheduleEventType.PHASE)).thenReturn(eventReference);

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail, new LicenceSchedulePhase(), USER);

    verify(licenceSchedulePhaseRepository).save(licenceSchedulePhaseArgumentCaptor.capture());

    var result = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getPhaseType,
        LicenceSchedulePhase::getPhaseDuration,
        LicenceSchedulePhase::getLicenceScheduleTerm,
        LicenceSchedulePhase::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        PhaseType.PHASE_A,
        form.getPhaseDuration().toThreeFieldDuration(),
        term,
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void savePhaseFromForm_existingPhase_doesntOverwriteEventReference() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setYears("1");
    form.getPhaseDuration().setMonths("0");
    form.getPhaseDuration().setDays("0");
    form.setComments("comments");

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var phase = new LicenceSchedulePhase();
    var existingEventReference = new EventReference();
    phase.setEventReference(existingEventReference);

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail, phase, USER);

    verify(licenceSchedulePhaseRepository).save(licenceSchedulePhaseArgumentCaptor.capture());

    var result = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getPhaseType,
        LicenceSchedulePhase::getPhaseDuration,
        LicenceSchedulePhase::getLicenceScheduleTerm,
        LicenceSchedulePhase::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        PhaseType.PHASE_A,
        form.getPhaseDuration().toThreeFieldDuration(),
        term,
        existingEventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), existingEventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void getPhaseForm_withPendingComment_populatesComments() {
    var eventReference = new EventReference();
    var phase = new LicenceSchedulePhase();
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setEventReference(eventReference);

    var pendingComment = new EventComment();
    pendingComment.setComment("pending comment text");
    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.of(pendingComment));

    var result = licenceSchedulePhaseFormService.getPhaseForm(phase);

    assertThat(result.getComments()).isEqualTo("pending comment text");
  }

  @Test
  void getPhaseForm_noPendingComment_commentsIsNull() {
    var eventReference = new EventReference();
    var phase = new LicenceSchedulePhase();
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    phase.setEventReference(eventReference);

    when(eventCommentService.findPendingCommentForEventReference(eventReference)).thenReturn(Optional.empty());

    var result = licenceSchedulePhaseFormService.getPhaseForm(phase);

    assertThat(result.getComments()).isNull();
  }

  @Test
  void getPhaseForm_noEventReference_commentsIsNull() {
    var phase = new LicenceSchedulePhase();
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));

    var result = licenceSchedulePhaseFormService.getPhaseForm(phase);

    assertThat(result.getComments()).isNull();
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermFormServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private EventReferenceService eventReferenceService;

  @Mock
  private EventCommentService eventCommentService;

  @InjectMocks
  private LicenceScheduleTermFormService licenceScheduleTermFormService;

  @Captor
  private ArgumentCaptor<LicenceScheduleTerm> licenceScheduleTermArgumentCaptor;

  @Test
  void saveTermFromForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceSchedule = new LicenceSchedule();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.getTermDuration().setYears("1");
    form.getTermDuration().setMonths("0");
    form.getTermDuration().setDays("0");

    var eventReference = new EventReference();
    when(eventReferenceService.createEventReference(licenceSchedule, ScheduleEventType.TERM)).thenReturn(eventReference);

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, new LicenceScheduleTerm(), USER);

    verify(licenceScheduleTermRepository).save(licenceScheduleTermArgumentCaptor.capture());

    var result = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration,
        LicenceScheduleTerm::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        TermType.INITIAL,
        form.getTermDuration().toThreeFieldDuration(),
        eventReference
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), eventReference, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveTermFromForm_existingTerm_doesntOverwriteEventReference() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.getTermDuration().setYears("1");
    form.getTermDuration().setMonths("0");
    form.getTermDuration().setDays("0");

    var term = new LicenceScheduleTerm();
    term.setEventReference(new EventReference());

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, term, USER);

    verify(licenceScheduleTermRepository).save(licenceScheduleTermArgumentCaptor.capture());

    var result = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration,
        LicenceScheduleTerm::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        TermType.INITIAL,
        form.getTermDuration().toThreeFieldDuration(),
        term.getEventReference()
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), term.getEventReference(), USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }
}

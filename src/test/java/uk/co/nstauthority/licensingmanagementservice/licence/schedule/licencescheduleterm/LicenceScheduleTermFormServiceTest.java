package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermFormServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

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

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, new LicenceScheduleTerm(), USER);

    verify(licenceScheduleTermRepository).save(licenceScheduleTermArgumentCaptor.capture());

    var result = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration,
        LicenceScheduleTerm::getLicenceSchedule
    ).containsExactly(
        licenceScheduleDetail,
        TermType.INITIAL,
        form.getTermDuration().toThreeFieldDuration(),
        licenceSchedule
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), result, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void saveTermFromForm_existingTerm_doesntOverwriteLicenceSchedule() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.getTermDuration().setYears("1");
    form.getTermDuration().setMonths("0");
    form.getTermDuration().setDays("0");

    var term = new LicenceScheduleTerm();
    var existingSchedule = new LicenceSchedule();
    term.setLicenceSchedule(existingSchedule);

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, term, USER);

    verify(licenceScheduleTermRepository).save(licenceScheduleTermArgumentCaptor.capture());

    var result = licenceScheduleTermArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceScheduleTerm::getLicenceScheduleDetail,
        LicenceScheduleTerm::getTermType,
        LicenceScheduleTerm::getTermDuration,
        LicenceScheduleTerm::getLicenceSchedule
    ).containsExactly(
        licenceScheduleDetail,
        TermType.INITIAL,
        form.getTermDuration().toThreeFieldDuration(),
        existingSchedule
    );

    verify(eventCommentService).addOrUpdatePendingComment(form.getComments(), term, USER);
    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void getTermForm_mapsTermTypeAndDuration() {
    var term = new LicenceScheduleTerm();
    term.setOriginalEventId(UUID.randomUUID());
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(2, 6, 0));
    term.setLicenceSchedule(new LicenceSchedule());

    when(eventCommentService.findPendingCommentForScheduleEvent(term)).thenReturn(Optional.empty());

    var result = licenceScheduleTermFormService.getTermForm(term);

    assertThat(result.getTermType()).isEqualTo(TermType.INITIAL);
    assertThat(result.getTermDuration().toThreeFieldDuration()).isEqualTo(new ThreeFieldDuration(2, 6, 0));
    assertThat(result.getComments()).isNull();
  }

  @Test
  void getTermForm_whenPendingCommentExists_populatesComments() {
    var term = new LicenceScheduleTerm();
    term.setOriginalEventId(UUID.randomUUID());
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));
    term.setLicenceSchedule(new LicenceSchedule());

    var pendingComment = new EventComment();
    pendingComment.setComment("Pending comment text");

    when(eventCommentService.findPendingCommentForScheduleEvent(term))
        .thenReturn(Optional.of(pendingComment));

    var result = licenceScheduleTermFormService.getTermForm(term);

    assertThat(result.getComments()).isEqualTo("Pending comment text");
  }

  @Test
  void getTermForm_whenLicenceScheduleIsNull_doesNotFetchComment() {
    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);
    term.setTermDuration(new ThreeFieldDuration(1, 0, 0));

    var result = licenceScheduleTermFormService.getTermForm(term);

    verify(eventCommentService, never()).findPendingCommentForScheduleEvent(term);
    assertThat(result.getComments()).isNull();
  }
}

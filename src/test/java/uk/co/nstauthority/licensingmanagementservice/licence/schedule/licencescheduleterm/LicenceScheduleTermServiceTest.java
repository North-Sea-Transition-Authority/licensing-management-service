package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermServiceTest {

  @Mock
  private LicenceScheduleTermRepository licenceScheduleTermRepository;

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceScheduleRateService licenceScheduleRateService;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @InjectMocks
  private LicenceScheduleTermService licenceScheduleTermService;

  @Test
  void getTermsByLicenceScheduleDetail() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceScheduleTermRepository).findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void saveTerms() {
    var terms = List.of(new LicenceScheduleTerm());

    licenceScheduleTermService.saveTerms(terms);

    verify(licenceScheduleTermRepository).saveAll(terms);
  }

  @Test
  void getTermByIdOrThrow() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());

    when(licenceScheduleTermRepository.findById(licenceScheduleTerm.getId())).thenReturn(Optional.of(licenceScheduleTerm));

    assertThat(licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTerm.getId())).isEqualTo(licenceScheduleTerm);
  }

  @Test
  void getTermByIdOrThrow_termNotFound() {
    when(licenceScheduleTermRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceScheduleTermService.getTermByIdOrThrow(UUID.randomUUID()))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getTermsByLicenceScheduleDetailAndTermTypeOrThrow() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermRepository.findByLicenceScheduleDetailAndTermType(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(Optional.of(licenceScheduleTerm));

    assertThat(licenceScheduleTermService.getTermsByLicenceScheduleDetailAndTermTypeOrThrow(
        licenceScheduleDetail, TermType.INITIAL))
        .isEqualTo(licenceScheduleTerm);
  }

  @Test
  void getTermsByLicenceScheduleDetailAndTermTypeOrThrow_termNotFound() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceScheduleTermRepository.findByLicenceScheduleDetailAndTermType(licenceScheduleDetail, TermType.INITIAL))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        licenceScheduleTermService.getTermsByLicenceScheduleDetailAndTermTypeOrThrow(licenceScheduleDetail, TermType.INITIAL))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void deleteTerm_whenCanDeleteTerm_deletesSuccessfully() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of());
    when(licenceScheduleRateService.getAllRatesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(otherScheduleEventService.getAllEventsLinkedTo(licenceScheduleTerm)).thenReturn(List.of());

    licenceScheduleTermService.deleteTerm(licenceScheduleTerm);

    verify(licenceScheduleTermRepository).delete(licenceScheduleTerm);
  }

  @Test
  void deleteTerm_whenCannotDeleteTerm_throwsResponseStatusException() {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of(new LicenceSchedulePhase()));

    assertThatThrownBy(() -> licenceScheduleTermService.deleteTerm(licenceScheduleTerm))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void canDeleteTerm_whenHasPhases_returnsFalse() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of(new LicenceSchedulePhase()));

    assertThat(licenceScheduleTermService.canDeleteTerm(licenceScheduleTerm)).isFalse();
  }

  @Test
  void canDeleteTerm_whenHasLinkedRates_returnsFalse() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of());
    when(licenceScheduleRateService.getAllRatesLinkedTo(licenceScheduleTerm)).thenReturn(List.of(new LicenceScheduleRate()));

    assertThat(licenceScheduleTermService.canDeleteTerm(licenceScheduleTerm)).isFalse();
  }

  @Test
  void canDeleteTerm_whenHasLinkedWorkProgrammeActivities_returnsFalse() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of());
    when(licenceScheduleRateService.getAllRatesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(licenceScheduleTerm)).thenReturn(List.of(new WorkProgrammeActivity()));

    assertThat(licenceScheduleTermService.canDeleteTerm(licenceScheduleTerm)).isFalse();
  }

  @Test
  void canDeleteTerm_whenHasLinkedOtherScheduleEvents_returnsFalse() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of());
    when(licenceScheduleRateService.getAllRatesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(otherScheduleEventService.getAllEventsLinkedTo(licenceScheduleTerm)).thenReturn(List.of(new OtherScheduleEvent()));

    assertThat(licenceScheduleTermService.canDeleteTerm(licenceScheduleTerm)).isFalse();
  }

  @Test
  void canDeleteTerm_whenNoReferences_returnsTrue() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    when(licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm)).thenReturn(List.of());
    when(licenceScheduleRateService.getAllRatesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(workProgrammeActivityService.getAllActivitiesLinkedTo(licenceScheduleTerm)).thenReturn(List.of());
    when(otherScheduleEventService.getAllEventsLinkedTo(licenceScheduleTerm)).thenReturn(List.of());

    assertThat(licenceScheduleTermService.canDeleteTerm(licenceScheduleTerm)).isTrue();
  }
}

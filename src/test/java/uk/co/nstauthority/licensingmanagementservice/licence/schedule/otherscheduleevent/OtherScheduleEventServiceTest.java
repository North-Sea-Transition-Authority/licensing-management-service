package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption.WITHIN_A_PHASE;
import static uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption.WITHIN_A_TERM;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.ScheduleState;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class OtherScheduleEventServiceTest {

  @Mock
  private OtherScheduleEventRepository otherScheduleEventRepository;

  @InjectMocks
  private OtherScheduleEventService otherScheduleEventService;

  @Test
  void getOtherScheduleEventByIdOrThrow() {
    var event = new OtherScheduleEvent();
    event.setId(UUID.randomUUID());

    when(otherScheduleEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

    assertThat(otherScheduleEventService.getOtherScheduleEventByIdOrThrow(event.getId())).isEqualTo(event);
  }

  @Test
  void getOtherScheduleEvents() {
    var detail = new LicenceScheduleDetail();

    otherScheduleEventService.getOtherScheduleEvents(detail);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleDetail(detail);
  }

  @Test
  void saveScheduleEvents() {
    var eventList = List.of(new OtherScheduleEvent());

    otherScheduleEventService.saveScheduleEvents(eventList);

    verify(otherScheduleEventRepository).saveAll(eventList);
  }

  @Test
  void getScheduleEventsByTermAndDateOption() {
    var term = new LicenceScheduleTerm();

    otherScheduleEventService.getScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.RELATIVE_DATE);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleTermAndDateOption(term, OtherScheduleEventDateOption.RELATIVE_DATE);
  }

  @Test
  void getScheduleEventsByPhaseAndDateOption() {
    var phase = new LicenceSchedulePhase();

    otherScheduleEventService.getScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.RELATIVE_DATE);

    verify(otherScheduleEventRepository).findAllByLicenceSchedulePhaseAndDateOption(phase, OtherScheduleEventDateOption.RELATIVE_DATE);
  }

  @Test
  void getScheduleEventsByDateRangeFor_term() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setStartDate(startDate);
    term.setEndDate(endDate);

    otherScheduleEventService.getScheduleEventsByDateRangeFor(term);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleDetailAndEventDateBetween(
        licenceScheduleDetail,
        startDate,
        endDate
    );
  }

  @Test
  void getScheduleEventsByDateRangeFor_phase() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var phase = new LicenceSchedulePhase();
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setStartDate(startDate);
    phase.setEndDate(endDate);

    otherScheduleEventService.getScheduleEventsByDateRangeFor(phase);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleDetailAndEventDateBetween(
        licenceScheduleDetail,
        startDate,
        endDate
    );
  }

  @Test
  void getEventsAfterDate() {
    var detail = new LicenceScheduleDetail();
    var date = LocalDate.of(2026, 1, 1);

    otherScheduleEventService.getEventsAfterDate(detail, date);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleDetailAndEventDateAfter(detail, date);
  }

  @Test
  void getOtherScheduleEventByScheduleDetailAndEventReferenceOrThrow() {
    var detail = new LicenceScheduleDetail();
    var eventReference = new EventReference();
    var event = new OtherScheduleEvent();

    when(otherScheduleEventRepository.findByLicenceScheduleDetailAndEventReference(detail, eventReference))
        .thenReturn(Optional.of(event));

    assertThat(otherScheduleEventService.getOtherScheduleEventByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isEqualTo(event);
  }

  @Test
  void getOtherScheduleEventByScheduleDetailAndEventReferenceOrThrow_notFound() {
    var eventReference = new EventReference();
    eventReference.setId(UUID.randomUUID());

    when(otherScheduleEventRepository.findByLicenceScheduleDetailAndEventReference(any(), any()))
        .thenReturn(Optional.empty());

    var detail = new LicenceScheduleDetail();
    assertThatThrownBy(() -> otherScheduleEventService.getOtherScheduleEventByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void deleteOtherScheduleEvent() {
    var otherScheduleEvent = new OtherScheduleEvent();

    otherScheduleEventService.deleteOtherScheduleEvent(otherScheduleEvent);

    verify(otherScheduleEventRepository).delete(otherScheduleEvent);
  }

  @Test
  void getAllEventsLinkedTo() {
    var term = new LicenceScheduleTerm();

    otherScheduleEventService.getAllEventsLinkedTo(term);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleTerm(term);
  }

  @Test
  void hasEventWithinScheduleWindow_whenWithinTermEventInCurrentOrNextTerm_returnsTrue() {
    var currentTerm = new LicenceScheduleTerm();
    var nextTerm = new LicenceScheduleTerm();
    var state = new ScheduleState(currentTerm, null, nextTerm, null);

    var event = new OtherScheduleEvent();
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);

    when(otherScheduleEventRepository.findAllByLicenceScheduleTermAndDateOption(currentTerm, WITHIN_A_TERM)).thenReturn(List.of(event));

    var detail = new LicenceScheduleDetail();
    assertThat(otherScheduleEventService.hasEventWithinScheduleWindow(detail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).isTrue();
  }

  @Test
  void hasEventWithinScheduleWindow_whenWithinPhaseEventInCurrentOrNextPhase_returnsTrue() {
    var currentPhase = new LicenceSchedulePhase();
    var nextPhase = new LicenceSchedulePhase();
    var state = new ScheduleState(null, currentPhase, null, nextPhase);

    var event = new OtherScheduleEvent();
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);

    when(otherScheduleEventRepository.findAllByLicenceSchedulePhaseAndDateOption(currentPhase, WITHIN_A_PHASE)).thenReturn(List.of(event));

    var detail = new LicenceScheduleDetail();
    assertThat(otherScheduleEventService.hasEventWithinScheduleWindow(detail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state)).isTrue();
  }

  @Test
  void hasEventWithinScheduleWindow_whenEventDatedWithinCurrentToNextTermWindow_returnsTrue() {
    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2026, 1, 1));
    currentTerm.setEndDate(LocalDate.of(2028, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setStartDate(LocalDate.of(2028, 1, 1));
    nextTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var state = new ScheduleState(currentTerm, null, nextTerm, null);

    var event = new OtherScheduleEvent();
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);

    var detail = new LicenceScheduleDetail();
    when(otherScheduleEventRepository.findAllByLicenceScheduleDetailAndEventDateBetween(detail, LocalDate.of(2026, 1, 1), LocalDate.of(2030, 1, 1))).thenReturn(List.of(event));

    assertThat(otherScheduleEventService.hasEventWithinScheduleWindow(
        detail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state))
        .isTrue();
  }

  @Test
  void hasEventWithinScheduleWindow_whenOnlyOtherCategoryEventsInWindow_returnsFalse() {
    var currentTerm = new LicenceScheduleTerm();
    currentTerm.setStartDate(LocalDate.of(2026, 1, 1));

    var nextTerm = new LicenceScheduleTerm();
    nextTerm.setEndDate(LocalDate.of(2030, 1, 1));

    var state = new ScheduleState(currentTerm, null, nextTerm, null);

    var otherCategoryEvent = new OtherScheduleEvent();
    otherCategoryEvent.setCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);

    var detail = new LicenceScheduleDetail();
    when(otherScheduleEventRepository.findAllByLicenceScheduleDetailAndEventDateBetween(detail, LocalDate.of(2026, 1, 1), LocalDate.of(2030, 1, 1))).thenReturn(List.of(otherCategoryEvent));

    assertThat(otherScheduleEventService.hasEventWithinScheduleWindow(
        detail, OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT, state))
        .isFalse();
  }
}

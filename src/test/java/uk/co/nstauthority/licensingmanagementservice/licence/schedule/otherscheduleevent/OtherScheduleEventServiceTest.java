package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  void getActiveScheduleEventsByTermAndDateOption() {
    var term = new LicenceScheduleTerm();

    otherScheduleEventService.getActiveScheduleEventsByTermAndDateOption(term, OtherScheduleEventDateOption.RELATIVE_DATE);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleTermAndDateOption(term, OtherScheduleEventDateOption.RELATIVE_DATE);
  }

  @Test
  void getActiveScheduleEventsByPhaseAndDateOption() {
    var phase = new LicenceSchedulePhase();

    otherScheduleEventService.getActiveScheduleEventsByPhaseAndDateOption(phase, OtherScheduleEventDateOption.RELATIVE_DATE);

    verify(otherScheduleEventRepository).findAllByLicenceSchedulePhaseAndDateOption(phase, OtherScheduleEventDateOption.RELATIVE_DATE);
  }

  @Test
  void getActiveScheduleEventsByDateRangeFor_term() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var term = new LicenceScheduleTerm();
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setStartDate(startDate);
    term.setEndDate(endDate);

    otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(term);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleDetailAndEventDateBetween(
        licenceScheduleDetail,
        startDate,
        endDate
    );
  }

  @Test
  void getActiveScheduleEventsByDateRangeFor_phase() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var startDate = LocalDate.now();
    var endDate = LocalDate.now();

    var phase = new LicenceSchedulePhase();
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setStartDate(startDate);
    phase.setEndDate(endDate);

    otherScheduleEventService.getActiveScheduleEventsByDateRangeFor(phase);

    verify(otherScheduleEventRepository).findAllByLicenceScheduleDetailAndEventDateBetween(
        licenceScheduleDetail,
        startDate,
        endDate
    );
  }

  @Test
  void getActiveEventsAfterDate() {
    var detail = new LicenceScheduleDetail();
    var date = LocalDate.of(2026, 1, 1);

    otherScheduleEventService.getActiveEventsAfterDate(detail, date);

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
}

package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@Service
public class TermOrPhaseEndDeadlineService implements ReminderDeadlineSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(TermOrPhaseEndDeadlineService.class);

  private static final int PREFILTER_BUFFER_DAYS = 2;

  private final Clock clock;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;

  public TermOrPhaseEndDeadlineService(
      Clock clock,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService
  ) {
    this.clock = clock;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
  }

  @Override
  public List<ReminderDeadline> getDeadlinesDueReminder(NoticePeriod noticePeriod) {
    var today = LocalDate.now(clock);
    var latestEndDate = today.plusMonths(noticePeriod.getMonths()).plusDays(PREFILTER_BUFFER_DAYS);

    var candidateTerms = licenceScheduleTermService.getTermsEndingBetweenOnActiveSchedules(today, latestEndDate);

    var candidatePhases = licenceSchedulePhaseService.getPhasesEndingBetweenOnActiveSchedules(today, latestEndDate);

    if (candidateTerms.isEmpty() && candidatePhases.isEmpty()) {
      return List.of();
    }

    var finalTermIds = candidateTerms.isEmpty()
        ? Set.<UUID>of()
        : getFinalTermIds(getScheduleDetails(candidateTerms));

    var deadlines = new ArrayList<ReminderDeadline>();

    candidateTerms.stream()
        .filter(term -> !finalTermIds.contains(term.getId()))
        .filter(term -> isDue(noticePeriod, term.getEndDate(), today, term.getTermType().getDisplayName()))
        .map(term -> toDeadline(term, term.getEndDate(), term.getTermType().getDisplayName()))
        .forEach(deadlines::add);

    candidatePhases.stream()
        .filter(phase -> !endsWithItsTerm(phase))
        .filter(phase -> isDue(noticePeriod, phase.getEndDate(), today, phase.getPhaseType().getDisplayName()))
        .map(phase -> toDeadline(phase, phase.getEndDate(), phase.getPhaseType().getDisplayName()))
        .forEach(deadlines::add);

    return deadlines;
  }

  private Set<LicenceScheduleDetail> getScheduleDetails(List<LicenceScheduleTerm> candidateTerms) {
    return candidateTerms.stream()
        .map(LicenceScheduleTerm::getLicenceScheduleDetail)
        .collect(Collectors.toSet());
  }

  private Set<UUID> getFinalTermIds(Collection<LicenceScheduleDetail> activeScheduleDetails) {
    return licenceScheduleTermService.getTermsByLicenceScheduleDetails(activeScheduleDetails)
        .stream()
        .collect(Collectors.groupingBy(
            LicenceScheduleTerm::getLicenceScheduleDetail,
            Collectors.maxBy(Comparator.comparingInt(term -> term.getTermType().getDisplayOrder()))))
        .values()
        .stream()
        .flatMap(Optional::stream)
        .map(LicenceScheduleTerm::getId)
        .collect(Collectors.toSet());
  }

  private boolean endsWithItsTerm(LicenceSchedulePhase phase) {
    var term = phase.getLicenceScheduleTerm();

    return term != null && term.getEndDate() != null && term.getEndDate().equals(phase.getEndDate());
  }

  private boolean isDue(NoticePeriod noticePeriod, LocalDate endDate, LocalDate today, String displayName) {
    if (endDate == null) {
      LOGGER.warn("Skipping {} for reminders as it has no end date", displayName);
      return false;
    }

    return noticePeriod.isDueBy(endDate, today);
  }

  private ReminderDeadline toDeadline(
      ScheduleEvent scheduleEvent,
      LocalDate deadlineDate,
      String displayName
  ) {
    return new ReminderDeadline(
        scheduleEvent,
        scheduleEvent.getOriginalEventId(),
        scheduleEvent.getLicenceSchedule().getLicence(),
        deadlineDate,
        displayName);
  }
}

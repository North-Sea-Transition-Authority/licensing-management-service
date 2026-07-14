package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationService;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
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
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class LicenceScheduleDetailDuplicationService {

  private final List<DuplicationSource<LicenceScheduleDetail>> duplicationSources;
  private final LicenceScheduleDetailRepository licenceScheduleDetailRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final OtherScheduleEventService otherScheduleEventService;
  private final DuplicationService duplicationService;
  private final Clock clock;

  public LicenceScheduleDetailDuplicationService(
      List<DuplicationSource<LicenceScheduleDetail>> duplicationSources,
      LicenceScheduleDetailRepository licenceScheduleDetailRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceScheduleRateService licenceScheduleRateService,
      WorkProgrammeActivityService workProgrammeActivityService,
      OtherScheduleEventService otherScheduleEventService,
      DuplicationService duplicationService,
      Clock clock
  ) {
    this.duplicationSources = duplicationSources;
    this.licenceScheduleDetailRepository = licenceScheduleDetailRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.otherScheduleEventService = otherScheduleEventService;
    this.duplicationService = duplicationService;
    this.clock = clock;
  }

  @Transactional
  LicenceScheduleDetail createNewDraftLicenceScheduleDetailVersion(LicenceScheduleDetail oldDetail) {
    var newDetail = createNewDetail(oldDetail);
    licenceScheduleDetailRepository.save(newDetail);
    duplicationService.duplicateChildEntities(oldDetail, newDetail, duplicationSources);
    relinkTermsAndPhases(oldDetail, newDetail);
    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(newDetail);
    return newDetail;
  }

  private LicenceScheduleDetail createNewDetail(LicenceScheduleDetail oldDetail) {
    var newDetail = new LicenceScheduleDetail();
    newDetail.setLicenceSchedule(oldDetail.getLicenceSchedule());
    newDetail.setCreatedInstant(Instant.now(clock));
    newDetail.setStatus(LicenceScheduleDetailStatus.DRAFT);

    return newDetail;
  }

  @Transactional
  void relinkTermsAndPhases(LicenceScheduleDetail oldDetail, LicenceScheduleDetail newDetail) {
    var oldNewTermMap = getOldNewTermMap(oldDetail, newDetail);
    var oldNewPhaseMap = getOldNewPhaseMap(oldDetail, newDetail);

    var phases = List.copyOf(oldNewPhaseMap.values());
    for (LicenceSchedulePhase phase : phases) {
      phase.setLicenceScheduleTerm(oldNewTermMap.get(phase.getLicenceScheduleTerm()));
    }

    var rates = licenceScheduleRateService.getLicenceScheduleRates(newDetail);
    for (LicenceScheduleRate rate : rates) {
      if (rate.getLicenceScheduleTerm() != null) {
        rate.setLicenceScheduleTerm(oldNewTermMap.get(rate.getLicenceScheduleTerm()));
      } else {
        rate.setLicenceSchedulePhase(oldNewPhaseMap.get(rate.getLicenceSchedulePhase()));
      }
    }

    var activities = workProgrammeActivityService.getWorkProgrammeActivities(newDetail);
    for (WorkProgrammeActivity activity : activities) {
      if (activity.getLicenceScheduleTerm() != null) {
        activity.setLicenceScheduleTerm(oldNewTermMap.get(activity.getLicenceScheduleTerm()));
      } else {
        activity.setLicenceSchedulePhase(oldNewPhaseMap.get(activity.getLicenceSchedulePhase()));
      }
    }

    var events = otherScheduleEventService.getOtherScheduleEvents(newDetail);
    for (OtherScheduleEvent event : events) {
      if (event.getLicenceScheduleTerm() != null) {
        event.setLicenceScheduleTerm(oldNewTermMap.get(event.getLicenceScheduleTerm()));
      } else {
        event.setLicenceSchedulePhase(oldNewPhaseMap.get(event.getLicenceSchedulePhase()));
      }
    }

    licenceSchedulePhaseService.saveLicenceSchedulePhases(phases);
    licenceScheduleRateService.saveLicenceScheduleRates(rates);
    workProgrammeActivityService.saveWorkProgrammeActivities(activities);
    otherScheduleEventService.saveScheduleEvents(events);
  }

  private Map<LicenceScheduleTerm, LicenceScheduleTerm> getOldNewTermMap(
      LicenceScheduleDetail oldDetail,
      LicenceScheduleDetail newDetail
  ) {
    var oldDetailTerms = licenceScheduleTermService.getTermsByLicenceScheduleDetail(oldDetail);
    var newDetailTermsRefMap = licenceScheduleTermService.getTermsByLicenceScheduleDetail(newDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(
            LicenceScheduleTerm::getTermType,
            Function.identity()
        ));

    return oldDetailTerms.stream()
        .collect(StreamUtil.toLinkedHashMap(
            Function.identity(),
            term -> newDetailTermsRefMap.get(term.getTermType())
        ));
  }

  private Map<LicenceSchedulePhase, LicenceSchedulePhase> getOldNewPhaseMap(
      LicenceScheduleDetail oldDetail,
      LicenceScheduleDetail newDetail
  ) {
    var oldDetailPhases = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(oldDetail);
    var newDetailPhasesRefMap = licenceSchedulePhaseService.getPhasesByLicenceScheduleDetail(newDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(
            LicenceSchedulePhase::getPhaseType,
            Function.identity()
        ));

    return oldDetailPhases.stream()
        .collect(StreamUtil.toLinkedHashMap(
            Function.identity(),
            phase -> newDetailPhasesRefMap.get(phase.getPhaseType())
        ));
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@Service
public class LicenceScheduleTermService {

  private final LicenceScheduleTermRepository licenceScheduleTermRepository;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final OtherScheduleEventService otherScheduleEventService;

  public LicenceScheduleTermService(LicenceScheduleTermRepository licenceScheduleTermRepository,
                                    LicenceSchedulePhaseService licenceSchedulePhaseService,
                                    LicenceScheduleRateService licenceScheduleRateService,
                                    WorkProgrammeActivityService workProgrammeActivityService,
                                    OtherScheduleEventService otherScheduleEventService
  ) {
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  public List<LicenceScheduleTerm> getTermsByLicenceScheduleDetail(LicenceScheduleDetail scheduleDetail) {
    return licenceScheduleTermRepository.findAllByLicenceScheduleDetail(scheduleDetail);
  }

  @Transactional
  public Iterable<LicenceScheduleTerm> saveTerms(List<LicenceScheduleTerm> licenceScheduleTerms) {
    return licenceScheduleTermRepository.saveAll(licenceScheduleTerms);
  }

  public LicenceScheduleTerm getTermByIdOrThrow(UUID id) {
    return licenceScheduleTermRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceScheduleTerm not found", id.toString()));
  }

  public LicenceScheduleTerm getTermsByLicenceScheduleDetailAndTermTypeOrThrow(
      LicenceScheduleDetail licenceScheduleDetail,
      TermType termType
  ) {
    return licenceScheduleTermRepository.findByLicenceScheduleDetailAndTermType(licenceScheduleDetail, termType)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "LicenceScheduleTerm not found for schedule detail id: %s and term type %s"
                .formatted(licenceScheduleDetail.getId(), termType.getDisplayName())
        ));
  }

  @Transactional
  void deleteTerm(LicenceScheduleTerm licenceScheduleTerm) {
    if (canDeleteTerm(licenceScheduleTerm)) {
      licenceScheduleTermRepository.delete(licenceScheduleTerm);
    } else {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Cannot delete term id: %s as it is referenced by other schedule events".formatted(licenceScheduleTerm.getId())
      );
    }
  }

  boolean canDeleteTerm(LicenceScheduleTerm licenceScheduleTerm) {
    if (!licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm).isEmpty()) {
      return false;
    }

    if (!licenceScheduleRateService.getAllRatesLinkedTo(licenceScheduleTerm).isEmpty()) {
      return false;
    }

    if (!workProgrammeActivityService.getAllActivitiesLinkedTo(licenceScheduleTerm).isEmpty()) {
      return false;
    }

    return otherScheduleEventService.getAllEventsLinkedTo(licenceScheduleTerm).isEmpty();
  }
}

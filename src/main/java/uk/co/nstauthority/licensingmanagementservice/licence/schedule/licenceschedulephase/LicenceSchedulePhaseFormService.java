package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Service
public class LicenceSchedulePhaseFormService {

  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final EventReferenceService eventReferenceService;
  private final EventCommentService eventCommentService;

  public LicenceSchedulePhaseFormService(
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      LicenceScheduleTermService licenceScheduleTermService,
      EventReferenceService eventReferenceService,
      EventCommentService eventCommentService
  ) {
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.eventReferenceService = eventReferenceService;
    this.eventCommentService = eventCommentService;
  }

  @Transactional
  public void savePhaseFromForm(
      LicenceSchedulePhaseForm licenceSchedulePhaseForm,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceSchedulePhase licenceSchedulePhase,
      ServiceUserDetail serviceUserDetail
  ) {
    licenceSchedulePhase.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceSchedulePhase.setPhaseType(licenceSchedulePhaseForm.getPhaseType());
    licenceSchedulePhase.setPhaseDuration(licenceSchedulePhaseForm.getPhaseDuration().toThreeFieldDuration());
    licenceSchedulePhase.setLicenceScheduleTerm(getRelatedTerm(licenceScheduleDetail, licenceSchedulePhaseForm.getPhaseType()));

    if (licenceSchedulePhase.getEventReference() == null) {
      licenceSchedulePhase.setEventReference(
          eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.PHASE)
      );
    }

    licenceSchedulePhaseRepository.save(licenceSchedulePhase);

    eventCommentService.addOrUpdatePendingComment(
        licenceSchedulePhaseForm.getComments(),
        licenceSchedulePhase.getEventReference(),
        serviceUserDetail
    );

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  public LicenceSchedulePhaseForm getPhaseForm(LicenceSchedulePhase phase) {
    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(phase.getPhaseType());
    form.getPhaseDuration().setFromThreeFieldDuration(phase.getPhaseDuration());

    if (phase.getEventReference() != null) {
      eventCommentService.findPendingCommentForEventReference(phase.getEventReference())
          .ifPresent(comment -> form.setComments(comment.getComment()));
    }

    return form;
  }

  private LicenceScheduleTerm getRelatedTerm(
      LicenceScheduleDetail licenceScheduleDetail,
      PhaseType phaseType
  ) {
    return licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .filter(term -> term.getTermType().equals(phaseType.getTermType()))
        .findFirst()
        .orElseThrow(
            () -> new LmsEntityNotFoundException("Could not find related term for phase type: %s".formatted(phaseType.name()))
        );
  }
}

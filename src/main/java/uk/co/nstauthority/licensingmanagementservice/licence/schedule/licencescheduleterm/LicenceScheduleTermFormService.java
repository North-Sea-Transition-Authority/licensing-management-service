package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Service
public class LicenceScheduleTermFormService {

  private final LicenceScheduleTermRepository licenceScheduleTermRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final EventReferenceService eventReferenceService;
  private final EventCommentService eventCommentService;

  public LicenceScheduleTermFormService(
      LicenceScheduleTermRepository licenceScheduleTermRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      EventReferenceService eventReferenceService,
      EventCommentService eventCommentService
  ) {
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.eventReferenceService = eventReferenceService;
    this.eventCommentService = eventCommentService;
  }

  @Transactional
  public void saveTermFromForm(
      LicenceScheduleTermForm licenceScheduleTermForm,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm licenceScheduleTerm,
      ServiceUserDetail serviceUserDetail
  ) {
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setTermType(licenceScheduleTermForm.getTermType());
    licenceScheduleTerm.setTermDuration(licenceScheduleTermForm.getTermDuration().toThreeFieldDuration());

    if (licenceScheduleTerm.getEventReference() == null) {
      licenceScheduleTerm.setEventReference(
          eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.TERM)
      );
    }

    licenceScheduleTermRepository.save(licenceScheduleTerm);
    eventCommentService.addOrUpdatePendingComment(
        licenceScheduleTermForm.getComments(),
        licenceScheduleTerm.getEventReference(),
        serviceUserDetail
    );

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  public LicenceScheduleTermForm getTermForm(LicenceScheduleTerm term) {
    var form = new LicenceScheduleTermForm();
    form.setTermType(term.getTermType());
    form.getTermDuration().setFromThreeFieldDuration(term.getTermDuration());
    if (term.getEventReference() != null) {
      eventCommentService.findPendingCommentForEventReference(term.getEventReference())
          .ifPresent(comment -> form.setComments(comment.getComment()));
    }

    return form;
  }
}

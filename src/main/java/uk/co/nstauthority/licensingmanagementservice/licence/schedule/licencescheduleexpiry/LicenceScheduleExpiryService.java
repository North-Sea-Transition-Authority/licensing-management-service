package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReferenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Service
public class LicenceScheduleExpiryService {

  private final LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;
  private final EventReferenceService eventReferenceService;

  public LicenceScheduleExpiryService(
      LicenceScheduleExpiryRepository licenceScheduleExpiryRepository,
      EventReferenceService eventReferenceService
  ) {
    this.licenceScheduleExpiryRepository = licenceScheduleExpiryRepository;
    this.eventReferenceService = eventReferenceService;
  }

  public Optional<LicenceScheduleExpiry> getExpiryForLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleExpiryRepository.findByLicenceScheduleDetail(licenceScheduleDetail);
  }

  public LicenceScheduleExpiry getOrCreateExpiry(LicenceScheduleDetail licenceScheduleDetail) {
    return getExpiryForLicenceScheduleDetail(licenceScheduleDetail).orElseGet(LicenceScheduleExpiry::new);
  }

  @Transactional
  public void saveExpiryFromForm(
      LicenceScheduleExpiryForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleExpiry licenceScheduleExpiry
  ) {
    licenceScheduleExpiry.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleExpiry.setExpiryDate(form.getExpiryDate().getAsLocalDate().orElse(null));
    licenceScheduleExpiry.setComments(form.getComments());

    if (licenceScheduleExpiry.getEventReference() == null) {
      licenceScheduleExpiry.setEventReference(
          eventReferenceService.createEventReference(licenceScheduleDetail.getLicenceSchedule(), ScheduleEventType.EXPIRY)
      );
    }

    licenceScheduleExpiryRepository.save(licenceScheduleExpiry);
  }

  public LicenceScheduleExpiryForm getExpiryForm(LicenceScheduleExpiry expiry) {
    var form = new LicenceScheduleExpiryForm();
    if (expiry.getExpiryDate() != null) {
      form.getExpiryDate().setDate(expiry.getExpiryDate());
    }

    form.setComments(expiry.getComments());

    return form;
  }
}

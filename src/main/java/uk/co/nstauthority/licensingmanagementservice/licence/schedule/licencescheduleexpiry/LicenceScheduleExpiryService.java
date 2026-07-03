package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleExpiryService {

  private final LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  public LicenceScheduleExpiryService(
      LicenceScheduleExpiryRepository licenceScheduleExpiryRepository
  ) {
    this.licenceScheduleExpiryRepository = licenceScheduleExpiryRepository;
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

    if (licenceScheduleExpiry.getLicenceSchedule() == null) {
      licenceScheduleExpiry.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
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

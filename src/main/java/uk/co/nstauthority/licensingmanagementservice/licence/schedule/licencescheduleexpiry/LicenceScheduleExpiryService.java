package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class LicenceScheduleExpiryService {

  private final LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  public LicenceScheduleExpiryService(LicenceScheduleExpiryRepository licenceScheduleExpiryRepository) {
    this.licenceScheduleExpiryRepository = licenceScheduleExpiryRepository;
  }

  public LicenceScheduleExpiry getExpiryByIdOrThrow(UUID licenceScheduleExpiryId) {
    return licenceScheduleExpiryRepository.findById(licenceScheduleExpiryId)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceScheduleExpiry not found", licenceScheduleExpiryId.toString()));
  }

  public List<LicenceScheduleExpiry> getAllActiveExpiryDatesByLicenceScheduleDetail(
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return licenceScheduleExpiryRepository.findAllByLicenceScheduleDetailAndStatus(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<LicenceScheduleExpiry> getAllActiveExpiryDatesByDateRange(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate startDate,
      LocalDate endDate
  ) {
    return licenceScheduleExpiryRepository.findAllByLicenceScheduleDetailAndStatusAndExpiryDateBetween(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE,
        startDate,
        endDate
    );
  }

  public List<LicenceScheduleExpiry> getAllActiveExpiryDatesByDateRangeFor(LicenceScheduleTerm licenceScheduleTerm) {
    return getAllActiveExpiryDatesByDateRange(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<LicenceScheduleExpiry> getAllActiveExpiryDatesByDateRangeFor(LicenceSchedulePhase licenceSchedulePhase) {
    return getAllActiveExpiryDatesByDateRange(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    );
  }

  @Transactional
  public void saveExpiryFromForm(
      LicenceScheduleExpiryForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleExpiry licenceScheduleExpiry
  ) {
    licenceScheduleExpiry.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleExpiry.setStatus(LicenceScheduleEventStatus.ACTIVE);
    form.getExpiryDate().getAsLocalDate().ifPresent(licenceScheduleExpiry::setExpiryDate);
    licenceScheduleExpiry.setComments(form.getComments());

    licenceScheduleExpiryRepository.save(licenceScheduleExpiry);
  }

  public LicenceScheduleExpiryForm getExpiryForm(LicenceScheduleExpiry expiry) {
    var form = new LicenceScheduleExpiryForm();
    form.getExpiryDate().setDate(expiry.getExpiryDate());
    form.setComments(expiry.getComments());

    return form;
  }
}

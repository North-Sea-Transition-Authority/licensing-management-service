package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleExtensionFormService {
  private final Clock clock;
  private final LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;
  private final LicenceScheduleTermRepository licenceScheduleTermRepository;
  private final LicenceScheduleExtensionService licenceScheduleExtensionService;
  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  public LicenceScheduleExtensionFormService(
      Clock clock, LicenceScheduleExtensionRepository licenceScheduleExtensionRepository,
      LicenceScheduleTermRepository licenceScheduleTermRepository,
      LicenceScheduleExtensionService licenceScheduleExtensionService,
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository) {
    this.clock = clock;
    this.licenceScheduleExtensionRepository = licenceScheduleExtensionRepository;
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
  }

  public LicenceScheduleTerm getCurrentTerm(LicenceScheduleDetail licenceScheduleDetail) {

    List<LicenceScheduleTerm> licenceScheduleTerms = licenceScheduleTermRepository.findByLicenceScheduleDetail(
        licenceScheduleDetail);

    return licenceScheduleTerms.stream().filter(
        term -> isCurrentlyActive(term.getStartDate(), term.getEndDate())).findFirst().orElse(null);
  }

  public LicenceSchedulePhase getCurrentPhase(LicenceScheduleDetail licenceScheduleDetail) {


    List<LicenceSchedulePhase> licenceSchedulePhases = licenceSchedulePhaseRepository.findByLicenceScheduleDetail(
        licenceScheduleDetail);

    return licenceSchedulePhases.stream().filter(
        phase -> isCurrentlyActive(phase.getStartDate(), phase.getEndDate())).findFirst().orElse(null);
  }

  boolean isCurrentlyActive(LocalDate startDate, LocalDate endDate) {
    LocalDate today = LocalDate.now(clock);

    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }

    boolean hasStarted = startDate.isBefore(today) || startDate.isEqual(today);
    boolean hasNotEnded = endDate.isAfter(today);
    return hasStarted && hasNotEnded;
  }

  @Transactional
  public void saveExtensionForm(
      LicenceScheduleExtensionForm licenceScheduleExtensionForm,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var licenceScheduleExtension = licenceScheduleExtensionRepository.findByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail).orElse(new LicenceScheduleExtensionRequest());
    licenceScheduleExtension.setScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
    licenceScheduleExtension.setExtensionDuration(
        licenceScheduleExtensionForm.getExtensionDuration().toThreeFieldDuration());
    licenceScheduleExtension.setExplanation(licenceScheduleExtensionForm.getExplanation());
    licenceScheduleExtensionRepository.save(licenceScheduleExtension);
  }

  private LicenceScheduleExtensionForm licenceScheduleExtensionToForm(
      LicenceScheduleExtensionRequest licenceScheduleExtensionRequest) {

    var form = new LicenceScheduleExtensionForm();
    var formExtensionDuration = form.getExtensionDuration();
    var requestExtensionDuration = licenceScheduleExtensionRequest.getExtensionDuration();

    formExtensionDuration.setDays(
        requestExtensionDuration.days().toString());
    formExtensionDuration.setMonths(
        requestExtensionDuration.months().toString());
    formExtensionDuration.setYears(
        requestExtensionDuration.years().toString());
    form.setExplanation(licenceScheduleExtensionRequest.getExplanation());
    return form;

  }

  public LicenceScheduleExtensionForm getLicenceScheduleExtensionForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionService.getExtensionRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail).map(
        this::licenceScheduleExtensionToForm).orElse(new LicenceScheduleExtensionForm());
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;

@Service
public class LicenceScheduleSupportingInformationService {

  private final LicenceScheduleSupportingInformationRepository licenceScheduleSupportingInformationRepository;
  private LicenceScheduleExtensionService licenceScheduleExtensionService;
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public LicenceScheduleSupportingInformationService(
      LicenceScheduleSupportingInformationRepository licenceScheduleSupportingInformationRepository,
      LicenceScheduleExtensionService licenceScheduleExtensionService,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService
  ) {
    this.licenceScheduleSupportingInformationRepository = licenceScheduleSupportingInformationRepository;
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  public Optional<LicenceScheduleSupportingInformation> getRequestByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }

  @Transactional
  public void saveRequestForm(
      LicenceScheduleSupportingInformationForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var licenceScheduleRequest = licenceScheduleSupportingInformationRepository
        .findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail)
        .orElse(new LicenceScheduleSupportingInformation());
    licenceScheduleRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceScheduleRequest.setLicenceProgress(form.getLicenceProgress());
    licenceScheduleRequest.setReasonForAmendment(form.getReasonForAmendment());
    licenceScheduleRequest.setImpactOnDeliverables(form.getImpactOnDeliverables());
    licenceScheduleRequest.setPlanDuringExtension(form.getPlanDuringExtension());
    licenceScheduleRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceScheduleSupportingInformationRepository.save(licenceScheduleRequest);
  }

  private LicenceScheduleSupportingInformationForm licenceScheduleRequestForm(
      LicenceScheduleSupportingInformation licenceScheduleExtensionRequest
  ) {

    var form = new LicenceScheduleSupportingInformationForm();
    form.setLicenceProgress(licenceScheduleExtensionRequest.getLicenceProgress());
    form.setImpactOnDeliverables(licenceScheduleExtensionRequest.getImpactOnDeliverables());
    form.setPlanDuringExtension(licenceScheduleExtensionRequest.getPlanDuringExtension());
    form.setReasonForAmendment(licenceScheduleExtensionRequest.getReasonForAmendment());

    return form;
  }

  public LicenceScheduleSupportingInformationForm getLicenceScheduleRequestForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail).map(
        this::licenceScheduleRequestForm).orElse(new LicenceScheduleSupportingInformationForm());
  }

  public boolean isExtensionOrAmendment(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)
        || licenceWorkProgrammeAmendmentService.isAmendmentRequested(scheduleWorkProgrammeApplicationDetail);
  }

}
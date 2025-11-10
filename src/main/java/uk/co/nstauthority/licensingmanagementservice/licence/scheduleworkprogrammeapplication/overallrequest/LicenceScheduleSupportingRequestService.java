package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;

@Service
public class LicenceScheduleSupportingRequestService {

  private final LicenceScheduleSupportingRequestRepository licenceScheduleSupportingRequestRepository;
  private LicenceScheduleExtensionService licenceScheduleExtensionService;
  private LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;

  public LicenceScheduleSupportingRequestService(
      LicenceScheduleSupportingRequestRepository licenceScheduleSupportingRequestRepository,
      LicenceScheduleExtensionService licenceScheduleExtensionService,
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService
  ) {
    this.licenceScheduleSupportingRequestRepository = licenceScheduleSupportingRequestRepository;
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
  }

  public Optional<LicenceScheduleSupportingRequest> getRequestByScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleSupportingRequestRepository.findByScheduleWorkProgrammeApplicationDetails(
        scheduleWorkProgrammeApplicationDetail);
  }

  @Transactional
  public void saveRequestForm(
      LicenceScheduleSupportingRequestForm form,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var licenceScheduleRequest = licenceScheduleSupportingRequestRepository
        .findByScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail)
        .orElse(new LicenceScheduleSupportingRequest());
    licenceScheduleRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceScheduleRequest.setLicenceProgress(form.getLicenceProgress());
    licenceScheduleRequest.setReasonForAmendment(form.getReasonForAmendment());
    licenceScheduleRequest.setImpactOnDeliverables(form.getImpactOnDeliverables());
    licenceScheduleRequest.setPlanDuringExtension(form.getPlanDuringExtension());
    licenceScheduleRequest.setScheduleWorkProgrammeApplicationDetails(scheduleWorkProgrammeApplicationDetail);
    licenceScheduleSupportingRequestRepository.save(licenceScheduleRequest);
  }

  private LicenceScheduleSupportingRequestForm licenceScheduleRequestForm(
      LicenceScheduleSupportingRequest licenceScheduleExtensionRequest
  ) {

    var form = new LicenceScheduleSupportingRequestForm();
    form.setLicenceProgress(licenceScheduleExtensionRequest.getLicenceProgress());
    form.setImpactOnDeliverables(licenceScheduleExtensionRequest.getImpactOnDeliverables());
    form.setPlanDuringExtension(licenceScheduleExtensionRequest.getPlanDuringExtension());
    form.setReasonForAmendment(licenceScheduleExtensionRequest.getReasonForAmendment());

    return form;
  }

  public LicenceScheduleSupportingRequestForm getLicenceScheduleRequestForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return getRequestByScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplicationDetail).map(
        this::licenceScheduleRequestForm).orElse(new LicenceScheduleSupportingRequestForm());
  }

  public boolean isExtensionOrAmendment(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    return licenceScheduleExtensionService.isExtensionRequested(scheduleWorkProgrammeApplicationDetail)
        || licenceWorkProgrammeAmendmentService.isAmendmentRequested(scheduleWorkProgrammeApplicationDetail);
  }

}
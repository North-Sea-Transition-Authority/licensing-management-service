package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceScheduleSupportingInformationService {

  private final LicenceScheduleSupportingInformationRepository licenceScheduleSupportingInformationRepository;
  private final ApplicationFileService licenceScheduleApplicationFileService;
  private final LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService;

  public LicenceScheduleSupportingInformationService(
      LicenceScheduleSupportingInformationRepository licenceScheduleSupportingInformationRepository,
      ApplicationFileService licenceScheduleApplicationFileService,
      LicenceScheduleSupportingInformationHelperService licenceScheduleSupportingInformationHelperService
  ) {
    this.licenceScheduleSupportingInformationRepository = licenceScheduleSupportingInformationRepository;
    this.licenceScheduleApplicationFileService = licenceScheduleApplicationFileService;
    this.licenceScheduleSupportingInformationHelperService = licenceScheduleSupportingInformationHelperService;
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

    licenceScheduleApplicationFileService.saveDocuments(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail),
        form.getDocuments()
    );
  }

  private LicenceScheduleSupportingInformationForm licenceScheduleRequestForm(
      LicenceScheduleSupportingInformation licenceScheduleExtensionRequest,
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    var uploadedFileFormList = licenceScheduleApplicationFileService.getUploadedFiles(
        LicenceScheduleSupportingInformationFileUsages.fromApplication(scheduleWorkProgrammeApplicationDetail))
                                                                    .stream()
                                                                    .map(FileUploadLibraryUtils::asForm)
                                                                    .toList();

    var form = new LicenceScheduleSupportingInformationForm();
    form.setLicenceProgress(licenceScheduleExtensionRequest.getLicenceProgress());
    form.setImpactOnDeliverables(licenceScheduleExtensionRequest.getImpactOnDeliverables());
    form.setPlanDuringExtension(licenceScheduleExtensionRequest.getPlanDuringExtension());
    form.setReasonForAmendment(licenceScheduleExtensionRequest.getReasonForAmendment());
    form.setDocuments(uploadedFileFormList);
    return form;
  }

  public LicenceScheduleSupportingInformationForm getLicenceScheduleRequestForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    return getRequestByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        .map((LicenceScheduleSupportingInformation licenceScheduleExtensionRequest) -> licenceScheduleRequestForm(
                licenceScheduleExtensionRequest,
                scheduleWorkProgrammeApplicationDetail
            ))
        .orElse(new LicenceScheduleSupportingInformationForm());
  }

  @Transactional
  public void handleSupportingInformationExtensionRemoval(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    if (!licenceScheduleSupportingInformationHelperService.isExtensionOrAmendment(scheduleWorkProgrammeApplicationDetail)) {
      getRequestByScheduleWorkProgrammeApplicationDetail(
          scheduleWorkProgrammeApplicationDetail)
          .ifPresent(request -> {
            request.setPlanDuringExtension(null);
            licenceScheduleSupportingInformationRepository.save(request);
          });
    }
  }
}
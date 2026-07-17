package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationSupportingInformationService {

  private final LicenceContinuationSupportingInformationRepository licenceContinuationSupportingInformationRepository;
  private final ApplicationFileService applicationFileService;

  public LicenceContinuationSupportingInformationService(
      LicenceContinuationSupportingInformationRepository licenceContinuationSupportingInformationRepository,
      ApplicationFileService applicationFileService
  ) {
    this.licenceContinuationSupportingInformationRepository = licenceContinuationSupportingInformationRepository;
    this.applicationFileService = applicationFileService;
  }

  public Optional<LicenceContinuationSupportingInformation> getSupportingInformation(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return licenceContinuationSupportingInformationRepository.findByLicenceContinuationApplicationDetail(
        licenceContinuationApplicationDetail
    );
  }

  @Transactional
  public void saveSupportingInformationForm(
      LicenceContinuationSupportingInformationForm form,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var supportingInformation = licenceContinuationSupportingInformationRepository
        .findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail)
        .orElse(new LicenceContinuationSupportingInformation());

    supportingInformation.setLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail);
    supportingInformation.setHasAdditionalSupportingInformation(form.getHasAdditionalSupportingInformation());
    licenceContinuationSupportingInformationRepository.save(supportingInformation);

    var fileUsages = LicenceContinuationSupportingInformationFileUsages
        .fromApplication(licenceContinuationApplicationDetail);

    if (BooleanUtils.isTrue(form.getHasAdditionalSupportingInformation())) {
      applicationFileService.saveDocuments(fileUsages, form.getDocuments());
    } else {
      applicationFileService.deleteFiles(fileUsages);
    }
  }

  public LicenceContinuationSupportingInformationForm getSupportingInformationForm(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var form = new LicenceContinuationSupportingInformationForm();

    getSupportingInformation(licenceContinuationApplicationDetail).ifPresent(supportingInformation ->
        form.setHasAdditionalSupportingInformation(supportingInformation.getHasAdditionalSupportingInformation()));

    var uploadedFileForms = applicationFileService.getUploadedFiles(
            LicenceContinuationSupportingInformationFileUsages.fromApplication(licenceContinuationApplicationDetail))
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();
    form.setDocuments(uploadedFileForms);

    return form;
  }
}

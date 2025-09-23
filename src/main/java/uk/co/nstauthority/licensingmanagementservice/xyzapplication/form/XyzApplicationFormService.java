package uk.co.nstauthority.licensingmanagementservice.xyzapplication.form;

import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.SupportingDocumentApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplicationService;

@Service
public class XyzApplicationFormService {

  private final SearchSelectorService searchSelectorService;
  private final XyzApplicationService xyzApplicationService;
  private final ApplicationFileService applicationFileService;

  public XyzApplicationFormService(SearchSelectorService searchSelectorService,
                                   XyzApplicationService xyzApplicationService,
                                   ApplicationFileService applicationFileService) {
    this.searchSelectorService = searchSelectorService;
    this.xyzApplicationService = xyzApplicationService;
    this.applicationFileService = applicationFileService;
  }

  public Map<String, String> getPreselectedApplication(String applicationId) {
    if (StringUtils.isBlank(applicationId)) {
      return Map.of();
    }
    var applicationOptional = xyzApplicationService.findXyzApplicationById(UUID.fromString(applicationId));
    if (applicationOptional.isEmpty()) {
      return Map.of();
    }
    var application = applicationOptional.get();
    return searchSelectorService.buildPrePopulatedSelections(
        Collections.singletonList(applicationId),
        Map.of(applicationId, application.getReference()));
  }

  public XyzApplicationForm getApplicationForm(XyzApplication xyzApplication) {
    var applicationForm = new XyzApplicationForm();
    var uploadedFileForm = applicationFileService.getUploadedFiles(
        SupportingDocumentApplicationFileUsage.fromApplication(xyzApplication)
        )
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();

    applicationForm.setDocuments(uploadedFileForm);
    return applicationForm;
  }

  @Transactional
  public void saveApplicationForm(XyzApplicationForm xyzApplicationForm, XyzApplication xyzApplication) {
    // save xyz form entity for example:
    // applicationEntity.setXyzApplicationName(xyzApplicationForm.getXyzApplicationName())
    // ...
    // applicationEntityRepo.save(applicationEntity)

    // save any associated documents from form
    applicationFileService.saveDocuments(
        SupportingDocumentApplicationFileUsage.fromApplication(xyzApplication),
        xyzApplicationForm.getDocuments()
    );
  }
}

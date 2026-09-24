package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;

@Service
public class UploadDspService {

  private final ScheduleWorkProgrammeApplicationDetailRepository detailRepository;
  private final ApplicationFileService applicationFileService;
  private final ApplicationLetterService applicationLetterService;

  public UploadDspService(
      ScheduleWorkProgrammeApplicationDetailRepository detailRepository,
      ApplicationFileService applicationFileService,
      ApplicationLetterService applicationLetterService
  ) {
    this.detailRepository = detailRepository;
    this.applicationFileService = applicationFileService;
    this.applicationLetterService = applicationLetterService;
  }

  public UploadDspForm getFormForApplication(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new UploadDspForm();

    if (applicationDetail.getDecisionDate() != null) {
      form.getDecisionDate().setDate(applicationDetail.getDecisionDate());
    }

    var uploadedFiles = applicationFileService.getUploadedFiles(
        UploadDspFileUsage.fromApplication(applicationDetail))
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();
    form.setFinalDecisionSupportPapers(uploadedFiles);

    return form;
  }

  @Transactional
  public void uploadDsp(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      UploadDspForm form) {
    applicationLetterService.createDocumentInstance(applicationDetail.getScheduleWorkProgrammeApplication());
    form.getDecisionDate().getAsLocalDate().ifPresent(applicationDetail::setDecisionDate);
    applicationDetail.setStatus(ApplicationStatus.DSP_UPLOADED);
    detailRepository.save(applicationDetail);

    applicationFileService.saveDocuments(
        UploadDspFileUsage.fromApplication(applicationDetail),
        form.getFinalDecisionSupportPapers()
    );
  }
}

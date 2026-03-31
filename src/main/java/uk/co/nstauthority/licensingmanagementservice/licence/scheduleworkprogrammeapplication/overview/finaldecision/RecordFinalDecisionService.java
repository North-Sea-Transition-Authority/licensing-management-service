package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;

@Service
public class RecordFinalDecisionService {

  private final ScheduleWorkProgrammeApplicationDetailRepository detailRepository;
  private final ApplicationFileService applicationFileService;
  private final ApplicationLetterService applicationLetterService;

  public RecordFinalDecisionService(
      ScheduleWorkProgrammeApplicationDetailRepository detailRepository,
      ApplicationFileService applicationFileService,
      ApplicationLetterService applicationLetterService
  ) {
    this.detailRepository = detailRepository;
    this.applicationFileService = applicationFileService;
    this.applicationLetterService = applicationLetterService;
  }

  public RecordFinalDecisionForm getFormForApplication(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new RecordFinalDecisionForm();

    if (applicationDetail.getDecisionDate() != null) {
      form.getDecisionDate().setDate(applicationDetail.getDecisionDate());
    }

    var uploadedFiles = applicationFileService.getUploadedFiles(
        RecordFinalDecisionFileUsage.fromApplication(applicationDetail))
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();
    form.setFinalDecisionSupportPapers(uploadedFiles);

    return form;
  }

  @Transactional
  public void recordDecision(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      RecordFinalDecisionForm form) {
    applicationLetterService.createDocumentInstance(applicationDetail.getScheduleWorkProgrammeApplication());
    form.getDecisionDate().getAsLocalDate().ifPresent(applicationDetail::setDecisionDate);
    applicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.ISSUE_DECISION);
    detailRepository.save(applicationDetail);

    applicationFileService.saveDocuments(
        RecordFinalDecisionFileUsage.fromApplication(applicationDetail),
        form.getFinalDecisionSupportPapers()
    );
  }
}

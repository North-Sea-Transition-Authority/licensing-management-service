package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationService;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationFileUsages;

@Service
public class ScheduleWorkProgrammeApplicationDetailDuplicationService {

  private final List<DuplicationSource<ScheduleWorkProgrammeApplicationDetail>> duplicationSources;
  private final ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;
  private final DuplicationService duplicationService;
  private final Clock clock;
  private final ApplicationFileService applicationFileService;

  public ScheduleWorkProgrammeApplicationDetailDuplicationService(
      List<DuplicationSource<ScheduleWorkProgrammeApplicationDetail>> duplicationSources,
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository,
      DuplicationService duplicationService,
      Clock clock,
      ApplicationFileService applicationFileService
  ) {
    this.duplicationSources = duplicationSources;
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
    this.duplicationService = duplicationService;
    this.clock = clock;
    this.applicationFileService = applicationFileService;
  }

  @Transactional
  public ScheduleWorkProgrammeApplicationDetail createNewDraftScheduleWorkProgrammeApplicationDetailVersion(
      ScheduleWorkProgrammeApplicationDetail oldDetail,
      ServiceUserDetail user
  ) {
    var newDetail = createNewDetail(oldDetail, user);
    scheduleWorkProgrammeApplicationDetailRepository.save(newDetail);
    duplicationService.duplicateChildEntities(oldDetail, newDetail, duplicationSources);
    duplicateSupportingDocumentsForScheduleWorkProgrammeApplication(oldDetail, newDetail);
    return newDetail;
  }

  private void duplicateSupportingDocumentsForScheduleWorkProgrammeApplication(
      ScheduleWorkProgrammeApplicationDetail oldDetail,
      ScheduleWorkProgrammeApplicationDetail newDetail
  ) {
    List<UploadedFile> files =
        applicationFileService.getUploadedFiles(LicenceScheduleSupportingInformationFileUsages.fromApplication(oldDetail));

    applicationFileService.duplicateFiles(files, LicenceScheduleSupportingInformationFileUsages.fromApplication(newDetail));
  }

  private ScheduleWorkProgrammeApplicationDetail createNewDetail(
      ScheduleWorkProgrammeApplicationDetail oldDetail,
      ServiceUserDetail user
  ) {
    var newDetail = new ScheduleWorkProgrammeApplicationDetail();
    newDetail.setScheduleWorkProgrammeApplication(oldDetail.getScheduleWorkProgrammeApplication());
    newDetail.setVersionNumber(oldDetail.getVersionNumber() + 1);
    newDetail.setStatus(ApplicationStatus.DRAFT);
    newDetail.setCreatedDatetime(Instant.now(clock));
    newDetail.setCreatedByWuaId(user.wuaId());
    newDetail.setAllLicenseesPermissionConfirmed(oldDetail.getAllLicenseesPermissionConfirmed());
    newDetail.setResponsibleOrganisationUnitId(oldDetail.getResponsibleOrganisationUnitId());
    return newDetail;
  }
}

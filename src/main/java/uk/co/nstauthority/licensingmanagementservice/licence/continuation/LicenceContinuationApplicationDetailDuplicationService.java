package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

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
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileUsage;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationFileUsages;

@Service
public class LicenceContinuationApplicationDetailDuplicationService {

  private final List<DuplicationSource<LicenceContinuationApplicationDetail>> duplicationSources;
  private final LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;
  private final DuplicationService duplicationService;
  private final Clock clock;
  private final ApplicationFileService applicationFileService;

  public LicenceContinuationApplicationDetailDuplicationService(
      List<DuplicationSource<LicenceContinuationApplicationDetail>> duplicationSources,
      LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository,
      DuplicationService duplicationService,
      Clock clock,
      ApplicationFileService applicationFileService
  ) {
    this.duplicationSources = duplicationSources;
    this.licenceContinuationApplicationDetailRepository = licenceContinuationApplicationDetailRepository;
    this.duplicationService = duplicationService;
    this.clock = clock;
    this.applicationFileService = applicationFileService;
  }

  @Transactional
  public LicenceContinuationApplicationDetail createNewDraftLicenceContinuationApplicationDetailVersion(
      LicenceContinuationApplicationDetail oldDetail,
      ServiceUserDetail user
  ) {
    var newDetail = createNewDetail(oldDetail, user);
    licenceContinuationApplicationDetailRepository.save(newDetail);
    duplicationService.duplicateChildEntities(oldDetail, newDetail, duplicationSources);
    duplicateDocuments(
        LicenceContinuationSupportingInformationFileUsages.fromApplication(oldDetail),
        LicenceContinuationSupportingInformationFileUsages.fromApplication(newDetail)
    );
    duplicateDocuments(
        LicenceContinuationOtherRequirementFileUsages.fromApplication(oldDetail),
        LicenceContinuationOtherRequirementFileUsages.fromApplication(newDetail)
    );
    return newDetail;
  }

  private void duplicateDocuments(ApplicationFileUsage oldUsage, ApplicationFileUsage newUsage) {
    List<UploadedFile> files = applicationFileService.getUploadedFiles(oldUsage);
    applicationFileService.duplicateFiles(files, newUsage);
  }

  private LicenceContinuationApplicationDetail createNewDetail(
      LicenceContinuationApplicationDetail oldDetail,
      ServiceUserDetail user
  ) {
    var newDetail = new LicenceContinuationApplicationDetail();
    newDetail.setLicenceContinuationApplication(oldDetail.getLicenceContinuationApplication());
    newDetail.setVersionNumber(oldDetail.getVersionNumber() + 1);
    newDetail.setStatus(ApplicationStatus.DRAFT);
    newDetail.setCreatedDateTime(Instant.now(clock));
    newDetail.setCreatedByWuaId(user.wuaId());
    newDetail.setResponsibleOrganisationUnitId(oldDetail.getResponsibleOrganisationUnitId());
    newDetail.setCurrentTerm(oldDetail.getCurrentTerm());
    newDetail.setCurrentPhase(oldDetail.getCurrentPhase());
    newDetail.setNextTerm(oldDetail.getNextTerm());
    newDetail.setNextPhase(oldDetail.getNextPhase());
    return newDetail;
  }
}

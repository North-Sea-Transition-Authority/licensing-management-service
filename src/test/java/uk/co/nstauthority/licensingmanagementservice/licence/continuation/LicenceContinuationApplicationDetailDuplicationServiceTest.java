package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationService;
import uk.co.nstauthority.licensingmanagementservice.duplication.DuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney.LicenceContinuationOtherRequirementFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation.LicenceContinuationSupportingInformationFileUsages;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationDetailDuplicationServiceTest {

  private static final Instant FIXED_INSTANT = Instant.now();
  private static final UUID NEW_DETAIL_ID = UUID.randomUUID();

  @Mock
  private List<DuplicationSource<LicenceContinuationApplicationDetail>> duplicationSources;

  @Mock
  private LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  @Mock
  private DuplicationService duplicationService;

  @Mock
  private Clock clock;

  @Mock
  private ApplicationFileService applicationFileService;

  @InjectMocks
  private LicenceContinuationApplicationDetailDuplicationService duplicationServiceUnderTest;

  @Captor
  private ArgumentCaptor<LicenceContinuationApplicationDetail> detailCaptor;

  @Test
  void createNewDraftLicenceContinuationApplicationDetailVersion() {
    var user = new ServiceUserDetail(20L, 30L, "Forename", "Surname", "email@example.com", null, null);
    var application = new LicenceContinuationApplication();
    var currentTerm = new LicenceScheduleTerm();
    var currentPhase = new LicenceSchedulePhase();
    var nextTerm = new LicenceScheduleTerm();
    var nextPhase = new LicenceSchedulePhase();

    var oldDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    oldDetail.setLicenceContinuationApplication(application);
    oldDetail.setVersionNumber(1);
    oldDetail.setStatus(ApplicationStatus.SUBMITTED);
    oldDetail.setResponsibleOrganisationUnitId(40);
    oldDetail.setCurrentTerm(currentTerm);
    oldDetail.setCurrentPhase(currentPhase);
    oldDetail.setNextTerm(nextTerm);
    oldDetail.setNextPhase(nextPhase);
    oldDetail.setSubmittedByWuaId(10L);
    oldDetail.setSubmittedDatetime(FIXED_INSTANT.minusSeconds(60));

    var expected = new LicenceContinuationApplicationDetail(NEW_DETAIL_ID);
    expected.setLicenceContinuationApplication(application);
    expected.setVersionNumber(2);
    expected.setStatus(ApplicationStatus.DRAFT);
    expected.setCreatedDateTime(FIXED_INSTANT);
    expected.setCreatedByWuaId(user.wuaId());
    expected.setResponsibleOrganisationUnitId(40);
    expected.setCurrentTerm(currentTerm);
    expected.setCurrentPhase(currentPhase);
    expected.setNextTerm(nextTerm);
    expected.setNextPhase(nextPhase);

    when(clock.instant()).thenReturn(FIXED_INSTANT);
    givenSavedDetailIsAssignedAnId();

    var result = duplicationServiceUnderTest
        .createNewDraftLicenceContinuationApplicationDetailVersion(oldDetail, user);

    verify(licenceContinuationApplicationDetailRepository).save(detailCaptor.capture());
    verify(duplicationService).duplicateChildEntities(oldDetail, detailCaptor.getValue(), duplicationSources);

    assertThat(result).isSameAs(detailCaptor.getValue());
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void createNewDraftLicenceContinuationApplicationDetailVersion_duplicatesSupportingAndOtherRequirementDocuments() {
    var user = new ServiceUserDetail(20L, 30L, "Forename", "Surname", "email@example.com", null, null);
    var oldDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    oldDetail.setLicenceContinuationApplication(new LicenceContinuationApplication());
    oldDetail.setVersionNumber(1);

    var oldSupportingUsage = LicenceContinuationSupportingInformationFileUsages.fromApplication(oldDetail);
    var oldOtherRequirementUsage = LicenceContinuationOtherRequirementFileUsages.fromApplication(oldDetail);
    var supportingFiles = List.of(new UploadedFile());
    var otherRequirementFiles = List.of(new UploadedFile(), new UploadedFile());

    when(clock.instant()).thenReturn(FIXED_INSTANT);
    givenSavedDetailIsAssignedAnId();
    when(applicationFileService.getUploadedFiles(oldSupportingUsage)).thenReturn(supportingFiles);
    when(applicationFileService.getUploadedFiles(oldOtherRequirementUsage)).thenReturn(otherRequirementFiles);

    var result = duplicationServiceUnderTest
        .createNewDraftLicenceContinuationApplicationDetailVersion(oldDetail, user);

    verify(applicationFileService).duplicateFiles(
        supportingFiles,
        LicenceContinuationSupportingInformationFileUsages.fromApplication(result)
    );
    verify(applicationFileService).duplicateFiles(
        otherRequirementFiles,
        LicenceContinuationOtherRequirementFileUsages.fromApplication(result)
    );
  }

  private void givenSavedDetailIsAssignedAnId() {
    when(licenceContinuationApplicationDetailRepository.save(any(LicenceContinuationApplicationDetail.class)))
        .thenAnswer(invocation -> {
          LicenceContinuationApplicationDetail detail = invocation.getArgument(0);
          detail.setId(NEW_DETAIL_ID);
          return detail;
        });
  }
}

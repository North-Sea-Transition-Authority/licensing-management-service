package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationFileUsages;

@ExtendWith(MockitoExtension.class)
class ScheduleWorkProgrammeApplicationDetailDuplicationServiceTest {

  private static final Instant FIXED_INSTANT = Instant.now();
  private static final UUID NEW_DETAIL_ID = UUID.randomUUID();

  @Mock
  private List<DuplicationSource<ScheduleWorkProgrammeApplicationDetail>> duplicationSources;

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;

  @Mock
  private DuplicationService duplicationService;

  @Mock
  private Clock clock;

  @Mock
  private ApplicationFileService applicationFileService;

  @InjectMocks
  private ScheduleWorkProgrammeApplicationDetailDuplicationService duplicationServiceUnderTest;

  @Captor
  private ArgumentCaptor<ScheduleWorkProgrammeApplicationDetail> detailCaptor;

  @Test
  void createNewDraftScheduleWorkProgrammeApplicationDetailVersion() {
    var user = new ServiceUserDetail(20L, 30L, "Forename", "Surname", "email@example.com", null, null);
    var application = new ScheduleWorkProgrammeApplication();

    var oldDetail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());
    oldDetail.setScheduleWorkProgrammeApplication(application);
    oldDetail.setVersionNumber(1);
    oldDetail.setStatus(ApplicationStatus.SUBMITTED);
    oldDetail.setAllLicenseesPermissionConfirmed(true);
    oldDetail.setResponsibleOrganisationUnitId(40);
    oldDetail.setSubmittedByWuaId(10L);
    oldDetail.setSubmittedDatetime(FIXED_INSTANT.minusSeconds(60));

    var expected = new ScheduleWorkProgrammeApplicationDetail(NEW_DETAIL_ID);
    expected.setScheduleWorkProgrammeApplication(application);
    expected.setVersionNumber(2);
    expected.setStatus(ApplicationStatus.DRAFT);
    expected.setCreatedDatetime(FIXED_INSTANT);
    expected.setCreatedByWuaId(user.wuaId());
    expected.setAllLicenseesPermissionConfirmed(true);
    expected.setResponsibleOrganisationUnitId(40);

    when(clock.instant()).thenReturn(FIXED_INSTANT);
    givenSavedDetailIsAssignedAnId();

    var result = duplicationServiceUnderTest
        .createNewDraftScheduleWorkProgrammeApplicationDetailVersion(oldDetail, user);

    verify(scheduleWorkProgrammeApplicationDetailRepository).save(detailCaptor.capture());
    verify(duplicationService).duplicateChildEntities(oldDetail, detailCaptor.getValue(), duplicationSources);

    assertThat(result).isSameAs(detailCaptor.getValue());
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void createNewDraftScheduleWorkProgrammeApplicationDetailVersion_duplicatesSupportingDocuments() {
    var user = new ServiceUserDetail(20L, 30L, "Forename", "Surname", "email@example.com", null, null);
    var oldDetail = new ScheduleWorkProgrammeApplicationDetail(UUID.randomUUID());
    oldDetail.setScheduleWorkProgrammeApplication(new ScheduleWorkProgrammeApplication());
    oldDetail.setVersionNumber(1);

    var supportingFiles = List.of(new UploadedFile(), new UploadedFile());

    when(clock.instant()).thenReturn(FIXED_INSTANT);
    givenSavedDetailIsAssignedAnId();
    when(applicationFileService.getUploadedFiles(LicenceScheduleSupportingInformationFileUsages.fromApplication(oldDetail)))
        .thenReturn(supportingFiles);

    var result = duplicationServiceUnderTest
        .createNewDraftScheduleWorkProgrammeApplicationDetailVersion(oldDetail, user);

    verify(applicationFileService).duplicateFiles(
        supportingFiles,
        LicenceScheduleSupportingInformationFileUsages.fromApplication(result)
    );
  }

  private void givenSavedDetailIsAssignedAnId() {
    when(scheduleWorkProgrammeApplicationDetailRepository.save(any(ScheduleWorkProgrammeApplicationDetail.class)))
        .thenAnswer(invocation -> {
          ScheduleWorkProgrammeApplicationDetail detail = invocation.getArgument(0);
          detail.setId(NEW_DETAIL_ID);
          return detail;
        });
  }
}

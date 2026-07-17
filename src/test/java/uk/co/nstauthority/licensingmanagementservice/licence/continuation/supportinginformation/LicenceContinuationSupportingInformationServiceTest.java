package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationSupportingInformationServiceTest {

  @Mock
  private LicenceContinuationSupportingInformationRepository repository;

  @Mock
  private ApplicationFileService applicationFileService;

  @Captor
  private ArgumentCaptor<LicenceContinuationSupportingInformation> supportingInformationCaptor;

  @InjectMocks
  private LicenceContinuationSupportingInformationService service;

  @Test
  void saveSupportingInformationForm_whenYes_savesAnswerAndDocuments() {
    var licenceContinuationApplicationDetail = detailWithId();
    var form = new LicenceContinuationSupportingInformationForm();
    form.setHasAdditionalSupportingInformation(true);
    form.setDocuments(List.of(new UploadedFileForm()));

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.empty());

    service.saveSupportingInformationForm(form, licenceContinuationApplicationDetail);

    verify(repository).save(supportingInformationCaptor.capture());
    var saved = supportingInformationCaptor.getValue();
    assertThat(saved.getHasAdditionalSupportingInformation()).isTrue();
    assertThat(saved.getLicenceContinuationApplicationDetail()).isEqualTo(licenceContinuationApplicationDetail);

    verify(applicationFileService).saveDocuments(
        LicenceContinuationSupportingInformationFileUsages.fromApplication(licenceContinuationApplicationDetail),
        form.getDocuments()
    );
  }

  @Test
  void saveSupportingInformationForm_whenNo_savesAnswerAndDeletesDocuments() {
    var licenceContinuationApplicationDetail = detailWithId();
    var form = new LicenceContinuationSupportingInformationForm();
    form.setHasAdditionalSupportingInformation(false);

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(new LicenceContinuationSupportingInformation()));

    service.saveSupportingInformationForm(form, licenceContinuationApplicationDetail);

    verify(repository).save(supportingInformationCaptor.capture());
    assertThat(supportingInformationCaptor.getValue().getHasAdditionalSupportingInformation()).isFalse();

    var fileUsages = LicenceContinuationSupportingInformationFileUsages
        .fromApplication(licenceContinuationApplicationDetail);
    verify(applicationFileService).deleteFiles(fileUsages);
    verify(applicationFileService, never()).saveDocuments(fileUsages, form.getDocuments());
  }

  @Test
  void getSupportingInformationForm_whenExists_returnsMappedFormWithDocuments() {
    var licenceContinuationApplicationDetail = detailWithId();
    var supportingInformation = new LicenceContinuationSupportingInformation();
    supportingInformation.setHasAdditionalSupportingInformation(true);

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.of(supportingInformation));

    var form = service.getSupportingInformationForm(licenceContinuationApplicationDetail);

    assertThat(form.getHasAdditionalSupportingInformation()).isTrue();
    assertThat(form.getDocuments()).isEmpty();
  }

  @Test
  void getSupportingInformationForm_whenNotExists_returnsEmptyForm() {
    var licenceContinuationApplicationDetail = detailWithId();

    when(repository.findByLicenceContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(Optional.empty());

    var form = service.getSupportingInformationForm(licenceContinuationApplicationDetail);

    assertThat(form.getHasAdditionalSupportingInformation()).isNull();
    assertThat(form.getDocuments()).isEmpty();
  }

  private static LicenceContinuationApplicationDetail detailWithId() {
    var detail = new LicenceContinuationApplicationDetail();
    detail.setId(UUID.randomUUID());
    return detail;
  }
}

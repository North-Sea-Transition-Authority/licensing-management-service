package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleDetailServiceTest {

  @Mock
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Captor
  private ArgumentCaptor<LicenceScheduleDetail> licenceScheduleDetailArgumentCaptor;

  @Test
  void getByIdOrThrow() {
    var licenceScheduleDetailId = UUID.randomUUID();

    var licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceScheduleDetailRepository.findById(licenceScheduleDetailId)).thenReturn(Optional.of(licenceScheduleDetail));

    assertThat(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetailId)).isEqualTo(licenceScheduleDetail);
  }

  @Test
  void getByIdOrThrow_licenceScheduleDetailNotFound() {
    var licenceScheduleDetailId = UUID.randomUUID();

    when(licenceScheduleDetailRepository.findById(licenceScheduleDetailId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetailId)).isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void createNewLicenceScheduleEntitiesForLicence_noExistingEntities() {
    var licence = new Licence();
    var licenceSchedule = new LicenceSchedule();

    when(licenceScheduleService.getOrCreateNewLicenceScheduleForLicence(licence)).thenReturn(licenceSchedule);

    licenceScheduleDetailService.createNewLicenceScheduleEntitiesForLicence(licence);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }

  @Test
  void createNewDraftLicenceScheduleDetail() {
    var licenceSchedule = new LicenceSchedule();

    licenceScheduleDetailService.createNewDraftLicenceScheduleDetail(licenceSchedule);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }

  @Test
  void draftScheduleExistsForLicence() {
    var licence = new Licence();

    when(licenceScheduleDetailRepository.existsByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.DRAFT))
        .thenReturn(true);

    assertThat(licenceScheduleDetailService.draftScheduleExistsForLicence(licence)).isTrue();
  }

  @Test
  void searchByLicenceReferenceLicenceTypeAndStatus() {
    var searchTerm = "searchTerm";

    licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        LicenceType.CARBON_STORAGE,
        LicenceScheduleDetailStatus.ACTIVE
    );

    verify(licenceScheduleDetailRepository).searchByLicenceReferenceLicenceTypeAndStatus(
      searchTerm,
      LicenceType.CARBON_STORAGE,
      LicenceScheduleDetailStatus.ACTIVE
    );
  }
}
package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class LicenceServiceTest {

  @Mock
  private LicenceRepository licenceRepository;

  @InjectMocks
  private LicenceService licenceService;


  @Test
  void getAllLicences() {
    licenceService.getAllLicences();

    verify(licenceRepository).findAll();
  }

  @Test
  void findLicenceByIdOrThrow() {
    var licence = new Licence();

    when(licenceRepository.findById(1)).thenReturn(Optional.of(licence));

    assertThat(licenceService.findLicenceByIdOrThrow(1)).isEqualTo(licence);
  }

  @Test
  void findLicenceByIdOrThrow_licenceNotFound() {
    when(licenceRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceService.findLicenceByIdOrThrow(1)).isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void licenceNumberExistsForType() {
    licenceService.licenceNumberExistsForType(LicenceType.CARBON_STORAGE, "001");

    verify(licenceRepository).existsByTypeAndLicenceNumber(LicenceType.CARBON_STORAGE, "001");
  }

  @Test
  void findLicenceByReference_returnsLicence_whenFound() {
    Licence licence = new Licence();
    licence.setLicenceReference("CS123");
    when(licenceRepository.findByLicenceReference("CS123"))
        .thenReturn(Optional.of(licence));

    Optional<Licence> result = licenceService.findLicenceByReference("CS123");

    assertTrue(result.isPresent());
    assertEquals("CS123", result.get().getLicenceReference());
  }

  @Test
  void findLicenceByReference_returnsEmpty_whenNotFound() {
    when(licenceRepository.findByLicenceReference("NOT_FOUND"))
        .thenReturn(Optional.empty());

    Optional<Licence> result = licenceService.findLicenceByReference("NOT_FOUND");

    assertFalse(result.isPresent());
  }

  @Test
  void findLicenceByReference_returnsEmpty_whenReferenceIsNull() {
    when(licenceRepository.findByLicenceReference(null))
        .thenReturn(Optional.empty());

    Optional<Licence> result = licenceService.findLicenceByReference(null);

    assertFalse(result.isPresent());
  }

  @Test
  void saveLicences() {
    var licences = List.of(new Licence());

    licenceService.saveLicences(licences);

    verify(licenceRepository).saveAll(licences);
  }

  @Test
  void getNextLicenceId_noLicences() {
    when(licenceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

    assertThat(licenceService.getNextLicenceId()).isEqualTo(10000);
  }

  @Test
  void getNextLicenceId_maxIdBelow10000() {
    var licence = new Licence();
    licence.setId(1);

    when(licenceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licence));

    assertThat(licenceService.getNextLicenceId()).isEqualTo(10000);
  }

  @Test
  void getNextLicenceId_maxIdAbove10000() {
    var licence = new Licence();
    licence.setId(10000);

    when(licenceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licence));

    assertThat(licenceService.getNextLicenceId()).isEqualTo(10001);
  }

  @Test
  void getLicencePageCaption() {
    var licenceId = 1;
    var licenceType = LicenceType.CARBON_STORAGE;
    var licenceRef = "P100";
    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(licenceType)
        .withLicenceReference(licenceRef)
        .build();
    when(licenceRepository.findById(licenceId)).thenReturn(Optional.of(licence));
    assertThat(licenceService.getLicencePageCaption(licence)).isEqualTo(licenceType.getDisplayName() + " - " + licenceRef);
  }

  @Test
  void isCarbonStorageLicence_whenCarbonStorage_returnsTrue() {
    var licence = new Licence();
    licence.setType(LicenceType.CARBON_STORAGE);

    assertThat(licenceService.isCarbonStorageLicence(licence)).isTrue();
  }

  @Test
  void isCarbonStorageLicence_whenNotCarbonStorage_returnsFalse() {
    var licence = new Licence();
    licence.setType(LicenceType.SEAWARD_PRODUCTION);

    assertThat(licenceService.isCarbonStorageLicence(licence)).isFalse();
  }

  @Test
  void searchLicencesByReferenceAndTypes() {
    var searchTerm = "term";
    var licenceTypes = List.of(LicenceType.SEAWARD_PRODUCTION);

    licenceService.searchLicencesByReferenceAndTypes(searchTerm, licenceTypes);

    verify(licenceRepository).findAllByLicenceReferenceContainingIgnoreCaseAndTypeIn(searchTerm, licenceTypes);
  }
}
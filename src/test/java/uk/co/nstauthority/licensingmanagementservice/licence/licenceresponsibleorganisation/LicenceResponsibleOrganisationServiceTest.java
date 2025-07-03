package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@ExtendWith(MockitoExtension.class)
class LicenceResponsibleOrganisationServiceTest {

  @Captor
  private ArgumentCaptor<List<LicenceResponsibleOrganisation>> organisationCaptor;

  @Mock
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @InjectMocks
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Test
  void getAllByLicence() {
    var licence = new Licence();

    licenceResponsibleOrganisationService.getAllByLicence(licence);

    verify(licenceResponsibleOrganisationRepository).findAllByLicence(licence);
  }

  @Test
  void refreshPearsResponsibleOrganisations() {
    var licence = new Licence();
    licence.setId(1);

    var licence2 = new Licence();
    licence2.setId(2);

    var licenceList = List.of(licence, licence2);

    var licenceMap = Map.of(
        1, List.of(1, 2),
        2, List.of(1, 3)
    );

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setResponsibleOrganisationId(1);
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setManagedByLms(false);

    var responsibleOrganisation2 = new LicenceResponsibleOrganisation();
    responsibleOrganisation2.setResponsibleOrganisationId(2);
    responsibleOrganisation2.setLicence(licence);
    responsibleOrganisation2.setManagedByLms(false);

    var responsibleOrganisation3 = new LicenceResponsibleOrganisation();
    responsibleOrganisation3.setResponsibleOrganisationId(1);
    responsibleOrganisation3.setLicence(licence2);
    responsibleOrganisation3.setManagedByLms(false);

    var responsibleOrganisation4 = new LicenceResponsibleOrganisation();
    responsibleOrganisation4.setResponsibleOrganisationId(3);
    responsibleOrganisation4.setLicence(licence2);
    responsibleOrganisation4.setManagedByLms(false);

    var oldOrgs = List.of(responsibleOrganisation, responsibleOrganisation2);

    when(licenceResponsibleOrganisationRepository.findAllByManagedByLmsIsFalse()).thenReturn(oldOrgs);

    licenceResponsibleOrganisationService.refreshPearsResponsibleOrganisations(licenceList, licenceMap);

    verify(licenceResponsibleOrganisationRepository).deleteAll(oldOrgs);

    verify(licenceResponsibleOrganisationRepository).saveAll(organisationCaptor.capture());

    assertThat(organisationCaptor.getValue())
        .containsExactlyInAnyOrder(
            responsibleOrganisation,
            responsibleOrganisation2,
            responsibleOrganisation3,
            responsibleOrganisation4);
  }

  @Test
  void saveLicenseesFromForm() {
    var licence = new Licence();

    var orgs = List.of("1", "2");

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setResponsibleOrganisationId(1);
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setManagedByLms(true);

    var responsibleOrganisation2 = new LicenceResponsibleOrganisation();
    responsibleOrganisation2.setResponsibleOrganisationId(2);
    responsibleOrganisation2.setLicence(licence);
    responsibleOrganisation2.setManagedByLms(true);

    var expectedResult = List.of(responsibleOrganisation, responsibleOrganisation2);

    licenceResponsibleOrganisationService.saveLicenseesFromForm(licence, orgs);

    verify(licenceResponsibleOrganisationRepository).saveAll(organisationCaptor.capture());

    assertThat(organisationCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(expectedResult);
  }
}
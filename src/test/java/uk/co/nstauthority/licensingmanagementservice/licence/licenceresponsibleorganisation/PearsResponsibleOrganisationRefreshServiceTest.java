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
class PearsResponsibleOrganisationRefreshServiceTest {

  @Mock
  private LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  @InjectMocks
  private PearsResponsibleOrganisationRefreshService pearsResponsibleOrganisationRefreshService;

  @Captor
  private ArgumentCaptor<List<LicenceResponsibleOrganisation>> responsibleOrganisationCaptor;

  @Test
  void saveResponsibleOrganisationsForLicences() {
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

    pearsResponsibleOrganisationRefreshService.saveResponsibleOrganisationsForLicences(licenceList, licenceMap);

    verify(licenceResponsibleOrganisationRepository).saveAll(responsibleOrganisationCaptor.capture());

    assertThat(responsibleOrganisationCaptor.getValue())
        .containsExactlyInAnyOrder(
            responsibleOrganisation,
            responsibleOrganisation2,
            responsibleOrganisation3,
            responsibleOrganisation4);
  }

  @Test
  void deleteRemovedResponsibleOrganisationsForLicences() {
    var licenceMap = Map.of(
        1, List.of(1),
        2, List.of(3)
    );

    var licence1 = new Licence();
    licence1.setId(1);

    var licence2 = new Licence();
    licence2.setId(2);

    var existingOrganisation1 = new LicenceResponsibleOrganisation();
    existingOrganisation1.setLicence(licence1);
    existingOrganisation1.setResponsibleOrganisationId(1);
    existingOrganisation1.setManagedByLms(false);

    var existingOrganisation2 = new LicenceResponsibleOrganisation();
    existingOrganisation2.setLicence(licence2);
    existingOrganisation2.setResponsibleOrganisationId(3);
    existingOrganisation2.setManagedByLms(false);

    var existingOrganisation3 = new LicenceResponsibleOrganisation();
    existingOrganisation3.setLicence(licence2);
    existingOrganisation3.setResponsibleOrganisationId(4);
    existingOrganisation3.setManagedByLms(false);

    when(licenceResponsibleOrganisationRepository.findAllByManagedByLmsIsFalse())
        .thenReturn(List.of(existingOrganisation1, existingOrganisation2, existingOrganisation3));

    pearsResponsibleOrganisationRefreshService.deleteRemovedResponsibleOrganisationsForLicences(licenceMap);

    verify(licenceResponsibleOrganisationRepository).deleteAll(List.of(existingOrganisation3));
  }
}

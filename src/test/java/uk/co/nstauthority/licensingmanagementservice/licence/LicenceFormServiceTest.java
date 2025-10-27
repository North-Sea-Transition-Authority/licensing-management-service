package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class LicenceFormServiceTest {

  @Mock
  private LicenceRepository licenceRepository;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicenceService licenceService;

  @InjectMocks
  private LicenceFormService licenceFormService;

  @Captor
  private ArgumentCaptor<Licence> licenceCaptor;

  @Test
  void saveNewLicenceFromForm() {
    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.CARBON_STORAGE);
    form.setLicenceNumber("001");
    form.setOrganisationUnitIds(List.of("1", "2"));

    when(licenceService.getNextLicenceId()).thenReturn(10000);

    var licence = new Licence();
    licence.setId(10000);
    licence.setType(LicenceType.CARBON_STORAGE);
    licence.setPrefix("CS");
    licence.setLicenceNumber("001");
    licence.setLicenceReference("CS001");
    licence.setStatus(LicenceStatus.EXTANT);

    licenceFormService.saveNewLicenceFromForm(form);

    verify(licenceRepository).save(licenceCaptor.capture());

    assertThat(licenceCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(licence);

    verify(licenceResponsibleOrganisationService).saveLicenseesFromForm(any(), eq(form.getOrganisationUnitIds()));
  }

  @Test
  void getPreselectedOrganisationUnits() {
    var orgUnitJson = new OrganisationUnitJson(
        1,
        "org name"
    );

    var orgUnitJson2 = new OrganisationUnitJson(
        2,
        "org name 2"
    );

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1,2))).thenReturn(List.of(orgUnitJson, orgUnitJson2));

    assertThat(licenceFormService.getPreselectedOrganisationUnits(List.of("1", "2"))).isEqualTo(List.of(orgUnitJson, orgUnitJson2));
  }

  @Test
  void getPreselectedOrganisationUnits_noneSelected() {
    assertThat(licenceFormService.getPreselectedOrganisationUnits(List.of())).isEmpty();

    verifyNoInteractions(organisationUnitQueryService);
  }

  @Test
  void getSavedOrganisationUnits() {
    var licence = new Licence();

    var licenceResponsibleOrganisation = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation.setLicence(licence);
    licenceResponsibleOrganisation.setResponsibleOrganisationId(1);

    var licenceResponsibleOrganisation2 = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation2.setLicence(licence);
    licenceResponsibleOrganisation2.setResponsibleOrganisationId(2);

    when(licenceResponsibleOrganisationService.getAllByLicence(licence)).thenReturn(List.of(licenceResponsibleOrganisation, licenceResponsibleOrganisation2));

    var orgUnitJson = new OrganisationUnitJson(
        1,
        "org name"
    );

    var orgUnitJson2 = new OrganisationUnitJson(
        2,
        "org name 2"
    );

    when(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1,2))).thenReturn(List.of(orgUnitJson, orgUnitJson2));

    assertThat(licenceFormService.getSavedOrganisationUnits(licence)).isEqualTo(List.of(orgUnitJson, orgUnitJson2));
  }
}
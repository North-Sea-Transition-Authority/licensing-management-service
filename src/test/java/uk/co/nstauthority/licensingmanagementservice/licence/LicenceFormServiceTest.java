package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

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

  @Mock
  private LicenceStatusService licenceStatusService;

  @InjectMocks
  private LicenceFormService licenceFormService;

  @Captor
  private ArgumentCaptor<Licence> licenceCaptor;

  @Test
  void saveNewLicenceFromForm() {
    var statusDate = LocalDate.of(2024, Month.JUNE, 15);

    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.SEAWARD_PRODUCTION);
    form.setLicenceNumber("001");
    form.setLicenceStatus(LicenceStatusType.REVOKED);
    form.getLicenceStatusDate().setDate(statusDate);
    form.setOrganisationUnitIds(List.of("1", "2"));

    when(licenceService.getNextLicenceId()).thenReturn(10000);

    var licence = new Licence();
    licence.setId(10000);
    licence.setType(LicenceType.SEAWARD_PRODUCTION);
    licence.setPrefix("P");
    licence.setLicenceNumber("001");
    licence.setLicenceReference("P001");

    licenceFormService.saveNewLicenceFromForm(form);

    verify(licenceRepository).save(licenceCaptor.capture());

    assertThat(licenceCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(licence);

    verify(licenceStatusService).recordLicenceStatus(any(), eq(LicenceStatusType.REVOKED), eq(statusDate));
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

  @Test
  void getEditLicenceDetailsForm() {
    var licence = new Licence();
    var statusDate = LocalDate.of(2024, Month.JUNE, 15);

    var latestLicenceStatus = new LicenceStatus();
    latestLicenceStatus.setLicence(licence);
    latestLicenceStatus.setStatus(LicenceStatusType.EXTANT);
    latestLicenceStatus.setStatusDate(statusDate);

    when(licenceStatusService.getLatestLicenceStatus(licence)).thenReturn(Optional.of(latestLicenceStatus));

    var form = licenceFormService.getEditLicenceDetailsForm(licence);

    assertThat(form.getLicenceStatus()).isEqualTo(LicenceStatusType.EXTANT);
    assertThat(form.getLicenceStatusDate().getAsLocalDate()).contains(statusDate);
  }

  @Test
  void getEditLicenceDetailsForm_noExistingStatus() {
    var licence = new Licence();

    when(licenceStatusService.getLatestLicenceStatus(licence)).thenReturn(Optional.empty());

    var form = licenceFormService.getEditLicenceDetailsForm(licence);

    assertThat(form.getLicenceStatus()).isNull();
    assertThat(form.getLicenceStatusDate().getAsLocalDate()).isEmpty();
  }

  @Test
  void saveEditLicenceDetailsFromForm_whenStatusChanged_thenStatusIsRecorded() {
    var licence = new Licence();
    var statusDate = LocalDate.of(2024, Month.JUNE, 15);

    var form = new EditLicenceDetailsForm();
    form.setLicenceStatus(LicenceStatusType.REVOKED);
    form.getLicenceStatusDate().setDate(statusDate);
    form.setOrganisationUnitIds(List.of("1"));

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    licenceFormService.saveEditLicenceDetailsFromForm(licence, form);

    verify(licenceStatusService).recordLicenceStatus(licence, LicenceStatusType.REVOKED, statusDate);
    verify(licenceResponsibleOrganisationService).saveLicenseesFromForm(licence, form.getOrganisationUnitIds());
  }

  @Test
  void saveEditLicenceDetailsFromForm_whenStatusUnchanged_thenStatusIsNotRecorded() {
    var licence = new Licence();
    var statusDate = LocalDate.of(2024, Month.JUNE, 15);

    var form = new EditLicenceDetailsForm();
    form.setLicenceStatus(LicenceStatusType.EXTANT);
    form.getLicenceStatusDate().setDate(statusDate);
    form.setOrganisationUnitIds(List.of("1"));

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);

    licenceFormService.saveEditLicenceDetailsFromForm(licence, form);

    verify(licenceStatusService, never()).recordLicenceStatus(any(), any(), any());
    verify(licenceResponsibleOrganisationService).saveLicenseesFromForm(licence, form.getOrganisationUnitIds());
  }
}
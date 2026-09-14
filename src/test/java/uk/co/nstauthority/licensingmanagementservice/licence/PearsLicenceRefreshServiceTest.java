package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.EpaLicenceDataDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.LicenceQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@ExtendWith(MockitoExtension.class)
class PearsLicenceRefreshServiceTest {

  private static final Integer LICENCE_ID = 1;
  private static final List<Integer> ORG_UNIT_IDS = List.of(1, 2);

  @Mock
  private LicenceQueryService licenceQueryService;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceStatusService licenceStatusService;

  @InjectMocks
  private PearsLicenceRefreshService pearsLicenceRefreshService;

  @Test
  void refreshAllLicences_whenLicenceIsNew_thenStatusIsRecorded() {
    var licence = licence();
    var licences = List.of(licence);
    var licenceIdOrgIdMap = Map.of(LICENCE_ID, ORG_UNIT_IDS);

    when(licenceQueryService.getEpaLicenceData())
        .thenReturn(new EpaLicenceDataDto(licences, licenceIdOrgIdMap, Map.of(LICENCE_ID, LicenceStatusType.EXTANT)));
    when(licenceService.getExistingLicenceIds(List.of(LICENCE_ID))).thenReturn(Set.of());
    when(licenceService.saveLicences(licences)).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences)).thenReturn(Map.of());

    pearsLicenceRefreshService.refreshAllLicences();

    verify(licenceStatusService).recordLicenceStatus(licence, LicenceStatusType.EXTANT);
    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licences, licenceIdOrgIdMap);
  }

  @Test
  void refreshAllLicences_whenLicenceAlreadyExisted_andStatusUnchanged_thenStatusIsNotRecorded() {
    var licence = licence();
    var licences = List.of(licence);
    var licenceIdOrgIdMap = Map.of(LICENCE_ID, ORG_UNIT_IDS);

    when(licenceQueryService.getEpaLicenceData())
        .thenReturn(new EpaLicenceDataDto(licences, licenceIdOrgIdMap, Map.of(LICENCE_ID, LicenceStatusType.EXTANT)));
    when(licenceService.getExistingLicenceIds(List.of(LICENCE_ID))).thenReturn(Set.of(LICENCE_ID));
    when(licenceService.saveLicences(licences)).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences))
        .thenReturn(Map.of(LICENCE_ID, LicenceStatusType.EXTANT));

    pearsLicenceRefreshService.refreshAllLicences();

    verify(licenceStatusService, never()).recordLicenceStatus(any(), any());
    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licences, licenceIdOrgIdMap);
  }

  @Test
  void refreshAllLicences_whenLicenceAlreadyExisted_andStatusChanged_thenStatusIsRecorded() {
    var licence = licence();
    var licences = List.of(licence);
    var licenceIdOrgIdMap = Map.of(LICENCE_ID, ORG_UNIT_IDS);

    when(licenceQueryService.getEpaLicenceData())
        .thenReturn(new EpaLicenceDataDto(licences, licenceIdOrgIdMap, Map.of(LICENCE_ID, LicenceStatusType.REVOKED)));
    when(licenceService.getExistingLicenceIds(List.of(LICENCE_ID))).thenReturn(Set.of(LICENCE_ID));
    when(licenceService.saveLicences(licences)).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences))
        .thenReturn(Map.of(LICENCE_ID, LicenceStatusType.EXTANT));

    pearsLicenceRefreshService.refreshAllLicences();

    verify(licenceStatusService).recordLicenceStatus(licence, LicenceStatusType.REVOKED);
    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licences, licenceIdOrgIdMap);
  }

  @Test
  void refreshLicence_whenRefreshingSingleLicence_thenOnlyThatLicencesResponsibleOrganisationsAreRefreshed() {
    var licence = licence();
    var licences = List.of(licence);

    when(licenceQueryService.getEpaLicenceData(List.of(LICENCE_ID))).thenReturn(new EpaLicenceDataDto(
        licences, Map.of(LICENCE_ID, ORG_UNIT_IDS), Map.of(LICENCE_ID, LicenceStatusType.EXTANT)));
    when(licenceService.getExistingLicenceIds(List.of(LICENCE_ID))).thenReturn(Set.of(LICENCE_ID));
    when(licenceService.saveLicences(licences)).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences))
        .thenReturn(Map.of(LICENCE_ID, LicenceStatusType.EXTANT));

    var result = pearsLicenceRefreshService.refreshLicence(LICENCE_ID);

    assertThat(result).contains(licence);
    verify(licenceResponsibleOrganisationService)
        .refreshPearsResponsibleOrganisationsForLicence(licence, ORG_UNIT_IDS);
  }

  @Test
  void refreshLicence_whenLicenceHasNoLicensees_thenRefreshedWithNoOrganisations() {
    var licence = licence();
    var licences = List.of(licence);

    when(licenceQueryService.getEpaLicenceData(List.of(LICENCE_ID))).thenReturn(new EpaLicenceDataDto(
        licences, Map.of(), Map.of(LICENCE_ID, LicenceStatusType.EXTANT)));
    when(licenceService.getExistingLicenceIds(List.of(LICENCE_ID))).thenReturn(Set.of(LICENCE_ID));
    when(licenceService.saveLicences(licences)).thenReturn(licences);
    when(licenceStatusService.getCurrentStatusesByLicenceId(licences))
        .thenReturn(Map.of(LICENCE_ID, LicenceStatusType.EXTANT));

    pearsLicenceRefreshService.refreshLicence(LICENCE_ID);

    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisationsForLicence(licence, List.of());
  }

  @Test
  void refreshLicence_whenEnergyPortalDoesNotHoldLicence_thenNothingChanged() {
    when(licenceQueryService.getEpaLicenceData(List.of(LICENCE_ID)))
        .thenReturn(new EpaLicenceDataDto(List.of(), Map.of(), Map.of()));

    var result = pearsLicenceRefreshService.refreshLicence(LICENCE_ID);

    assertThat(result).isEmpty();
    verifyNoInteractions(licenceService, licenceStatusService, licenceResponsibleOrganisationService);
  }

  private Licence licence() {
    return LicenceTestUtil.builder().withId(LICENCE_ID).build();
  }
}

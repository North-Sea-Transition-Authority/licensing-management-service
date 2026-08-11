package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class LicenceScheduledJobServiceTest {

  @Mock
  private LicenceQueryService licenceQueryService;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceStatusService licenceStatusService;

  @InjectMocks
  private LicenceScheduledJobService licenceScheduledJobService;

  @Test
  void retrieveAndSavePearsLicences_whenLicenceIsNew_thenStatusIsRecorded() {
    var licence = new Licence();
    licence.setId(1);

    var licenceList = List.of(licence);
    var licenceIdOrgIdMap = Map.of(1, List.of(1,2));
    var licenceIdStatusMap = Map.of(1, LicenceStatusType.EXTANT);

    var epaData = new EpaLicenceDataDto(licenceList, licenceIdOrgIdMap, licenceIdStatusMap);

    when(licenceQueryService.getEpaLicenceData()).thenReturn(epaData);

    when(licenceService.getExistingLicenceIds(List.of(1))).thenReturn(Set.of());

    when(licenceService.saveLicences(licenceList)).thenReturn(licenceList);

    when(licenceStatusService.getCurrentStatusesByLicenceId(licenceList)).thenReturn(Map.of());

    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(licenceStatusService).recordLicenceStatus(licence, LicenceStatusType.EXTANT);
    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licenceList, licenceIdOrgIdMap);
  }

  @Test
  void retrieveAndSavePearsLicences_whenLicenceAlreadyExisted_andStatusUnchanged_thenStatusIsNotRecorded() {
    var licence = new Licence();
    licence.setId(1);

    var licenceList = List.of(licence);
    var licenceIdOrgIdMap = Map.of(1, List.of(1,2));
    var licenceIdStatusMap = Map.of(1, LicenceStatusType.EXTANT);

    var epaData = new EpaLicenceDataDto(licenceList, licenceIdOrgIdMap, licenceIdStatusMap);

    when(licenceQueryService.getEpaLicenceData()).thenReturn(epaData);

    when(licenceService.getExistingLicenceIds(List.of(1))).thenReturn(Set.of(1));

    when(licenceService.saveLicences(licenceList)).thenReturn(licenceList);

    when(licenceStatusService.getCurrentStatusesByLicenceId(licenceList)).thenReturn(Map.of(1, LicenceStatusType.EXTANT));

    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(licenceStatusService, never()).recordLicenceStatus(any(), any());
    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licenceList, licenceIdOrgIdMap);
  }

  @Test
  void retrieveAndSavePearsLicences_whenLicenceAlreadyExisted_andStatusChanged_thenStatusIsRecorded() {
    var licence = new Licence();
    licence.setId(1);

    var licenceList = List.of(licence);
    var licenceIdOrgIdMap = Map.of(1, List.of(1,2));
    var licenceIdStatusMap = Map.of(1, LicenceStatusType.REVOKED);

    var epaData = new EpaLicenceDataDto(licenceList, licenceIdOrgIdMap, licenceIdStatusMap);

    when(licenceQueryService.getEpaLicenceData()).thenReturn(epaData);

    when(licenceService.getExistingLicenceIds(List.of(1))).thenReturn(Set.of(1));

    when(licenceService.saveLicences(licenceList)).thenReturn(licenceList);

    when(licenceStatusService.getCurrentStatusesByLicenceId(licenceList)).thenReturn(Map.of(1, LicenceStatusType.EXTANT));

    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(licenceStatusService).recordLicenceStatus(licence, LicenceStatusType.REVOKED);
    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licenceList, licenceIdOrgIdMap);
  }

}
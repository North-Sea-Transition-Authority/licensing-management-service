package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.EpaLicenceDataDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.LicenceQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduledJobServiceTest {

  @Mock
  private LicenceQueryService licenceQueryService;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @InjectMocks
  private LicenceScheduledJobService licenceScheduledJobService;

  @Test
  void retrieveAndSavePearsLicences() {
    var licenceList = List.of(new Licence());
    var licenceIdOrgIdMap = Map.of(1, List.of(1,2));

    var epaData = new EpaLicenceDataDto(licenceList, licenceIdOrgIdMap);

    when(licenceQueryService.getEpaLicenceData()).thenReturn(epaData);

    when(licenceService.saveLicences(licenceList)).thenReturn(licenceList);

    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(licenceResponsibleOrganisationService).refreshPearsResponsibleOrganisations(licenceList, licenceIdOrgIdMap);
  }

}
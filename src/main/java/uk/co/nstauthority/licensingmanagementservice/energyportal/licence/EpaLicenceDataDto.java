package uk.co.nstauthority.licensingmanagementservice.energyportal.licence;

import java.util.List;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;

public record EpaLicenceDataDto(
    List<Licence> licences,
    Map<Integer, List<Integer>> licenceIdOrgIdMap,
    Map<Integer, LicenceStatusType> licenceIdStatusMap
) {}

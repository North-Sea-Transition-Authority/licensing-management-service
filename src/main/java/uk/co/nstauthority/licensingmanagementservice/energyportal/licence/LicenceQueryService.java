package uk.co.nstauthority.licensingmanagementservice.energyportal.licence;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceApi;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceSearchFilter;
import uk.co.fivium.energyportalapi.generated.client.LicencesProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.LicenceStatus;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSubtype;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class LicenceQueryService {

  public static final LicencesProjectionRoot LICENCE_PROJECTION_ROOT =
      new LicencesProjectionRoot().id().licenceType().licenceSubType().licenceNo().licensees().organisationUnitId().root();

  private final LicenceApi licenceApi;

  public LicenceQueryService(LicenceApi licenceApi) {
    this.licenceApi = licenceApi;
  }

  public Map<Licence, List<OrganisationUnit>> getLicencesLicenseesMap() {
    var searchFilter =  LicenceSearchFilter.builder().withStatuses(Arrays.stream(LicenceStatus.values()).toList()).build();

    return licenceApi.searchLicences(
        searchFilter,
        LICENCE_PROJECTION_ROOT,
        new RequestPurpose("Get all licences"),
        CorrelationIdUtil.getLogCorrelationId()
    ).stream()
        .collect(
            StreamUtil.toLinkedHashMap(
                this::convertFromEpaLicense,
                uk.co.fivium.energyportalapi.generated.types.Licence::getLicensees
            )
        );
  }

  private Licence convertFromEpaLicense(uk.co.fivium.energyportalapi.generated.types.Licence portalLicence) {
    var subType = portalLicence.getLicenceSubType() != null
        ? LicenceSubtype.fromEpaLicenceSubtype(portalLicence.getLicenceSubType())
        : null;

    var licence = new Licence();
    licence.setId(portalLicence.getId());
    licence.setType(LicenceType.getFromPrefix(portalLicence.getLicenceType()));
    licence.setSubtype(subType);
    licence.setLicenceNumber(portalLicence.getLicenceNo().toString());
    licence.setPrefix(portalLicence.getLicenceType());

    return licence;
  }

}

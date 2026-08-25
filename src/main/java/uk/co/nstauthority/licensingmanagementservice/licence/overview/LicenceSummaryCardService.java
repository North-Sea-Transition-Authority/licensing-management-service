package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenseeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@Service
public class LicenceSummaryCardService {

  private final LicenceStatusService licenceStatusService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public LicenceSummaryCardService(
      LicenceStatusService licenceStatusService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceTypeRulesResolver licenceTypeRulesResolver
  ) {
    this.licenceStatusService = licenceStatusService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  public LicenceSummaryCardView getLicenceSummaryCardView(Licence licence) {
    var status = licenceStatusService.getCurrentStatus(licence);

    return new LicenceSummaryCardView(
        status != null ? status.getDisplayName() : null,
        LicenseeUtil.getLicenseeNames(licence, licenceResponsibleOrganisationService),
        licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType()),
        licence.getRoundIssuedOn()
    );
  }
}

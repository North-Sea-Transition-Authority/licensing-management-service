package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;

public record LicencePositionStateView(
    AdministratorStateView administratorStateView,
    LicenseeStateView licenseeStateView,
    List<BeneficialInterestView> beneficialInterests,
    List<OrganisationNameHistoryView> organisationNameHistories
) {
}

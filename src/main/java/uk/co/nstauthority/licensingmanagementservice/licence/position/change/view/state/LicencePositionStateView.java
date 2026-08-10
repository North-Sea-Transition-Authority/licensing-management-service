package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;

public record LicencePositionStateView(
    AdministratorStateView administratorStateView,
    List<BeneficialInterestView> beneficialInterests
) {
}
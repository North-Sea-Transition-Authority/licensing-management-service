package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;

public record SubareaBeneficialInterestView(
    String subareaLabel,
    List<BeneficialInterestView> beneficialInterests
) {
}
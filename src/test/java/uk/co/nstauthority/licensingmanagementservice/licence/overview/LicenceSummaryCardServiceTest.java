package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@ExtendWith(MockitoExtension.class)
class LicenceSummaryCardServiceTest {

  @Mock
  private LicenceStatusService licenceStatusService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @InjectMocks
  private LicenceSummaryCardService licenceSummaryCardService;

  @Test
  void getLicenceSummaryCardView_assertFullView() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withRoundIssuedOn("Round 1")
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(LicenceStatusType.EXTANT);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(licence)))
        .thenReturn(Map.of(licence, List.of(
            new OrganisationUnit(2, "SHELL U.K. LIMITED"),
            new OrganisationUnit(1, "BP EXPLORATION (ALPHA) LIMITED")
        )));
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(LicenceType.CARBON_STORAGE)).thenReturn(true);

    var result = licenceSummaryCardService.getLicenceSummaryCardView(licence);

    assertThat(result).usingRecursiveComparison().isEqualTo(new LicenceSummaryCardView(
        LicenceStatusType.EXTANT.getDisplayName(),
        List.of("BP EXPLORATION (ALPHA) LIMITED", "SHELL U.K. LIMITED"),
        true,
        "Round 1"
    ));
  }

  @Test
  void getLicenceSummaryCardView_whenNoStatusRecorded_assertNullStatus() {
    var licence = LicenceTestUtil.builder()
        .withLicenceType(LicenceType.GAS_STORAGE)
        .build();

    when(licenceStatusService.getCurrentStatus(licence)).thenReturn(null);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(List.of(licence)))
        .thenReturn(Map.of());
    when(licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(LicenceType.GAS_STORAGE)).thenReturn(false);

    var result = licenceSummaryCardService.getLicenceSummaryCardView(licence);

    assertThat(result).usingRecursiveComparison().isEqualTo(new LicenceSummaryCardView(
        null,
        List.of(),
        false,
        null
    ));
  }
}

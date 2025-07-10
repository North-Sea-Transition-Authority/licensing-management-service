package uk.co.nstauthority.licensingmanagementservice.licence.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

@ExtendWith(MockitoExtension.class)
class LicenceTypeFeatureServiceTest {

  @InjectMocks
  private LicenceTypeFeatureService underTest;

  @Test
  void hasTerms_WhenLicenceTypeHasTerms_ReturnsTrue() {
    var result = underTest.hasTerms(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void hasTerms_WhenLicenceTypeHasNoTerms_ReturnsFalse() {
    var result = underTest.hasTerms(LicenceType.A);
    assertThat(result).isFalse();
  }

  @Test
  void getTerms_WhenLicenceTypeHasTerms_ReturnsNonEmptySet() {
    var terms = underTest.getTerms(LicenceType.SEAWARD_PRODUCTION);
    assertThat(terms).isNotEmpty();
  }

  @Test
  void getTerms_WhenLicenceTypeHasNoTerms_ReturnsEmptySet() {
    var terms = underTest.getTerms(LicenceType.A);
    assertThat(terms).isEmpty();
  }

  @Test
  void hasPhases_WhenLicenceTypeHasPhases_ReturnsTrue() {
    var result = underTest.hasPhases(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void hasPhases_WhenLicenceTypeHasNoPhases_ReturnsFalse() {
    var result = underTest.hasPhases(LicenceType.A);
    assertThat(result).isFalse();
  }

  @Test
  void getPhases_WhenLicenceTypeHasPhases_ReturnsNonEmptySet() {
    var phases = underTest.getPhases(LicenceType.SEAWARD_PRODUCTION, TermType.INITIAL);
    assertThat(phases).isNotEmpty();
  }

  @Test
  void getPhases_WhenLicenceTypeHasNoPhases_ReturnsEmptySet() {
    var phases = underTest.getPhases(LicenceType.A, TermType.INITIAL);
    assertThat(phases).isEmpty();
  }

  @Test
  void arePhasesCaptured_WhenPhasesAreCaptured_ReturnsTrue() {
    var result = underTest.arePhasesCaptured(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void arePhasesCaptured_WhenPhasesAreNotCaptured_ReturnsFalse() {
    var result = underTest.arePhasesCaptured(LicenceType.CARBON_STORAGE);
    assertThat(result).isFalse();
  }

  @Test
  void hasRentalRate_WhenLicenceTypeHasRentalRate_ReturnsTrue() {
    var result = underTest.hasRentalRate(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void hasRentalRate_WhenLicenceTypeHasNoRentalRate_ReturnsFalse() {
    var result = underTest.hasRentalRate(LicenceType.A);
    assertThat(result).isFalse();
  }

  @Test
  void isRentalRatesFlat_WhenLicenceTypeHasFlatRentalRates_ReturnsTrue() {
    var result = underTest.isRentalRatesFlat(LicenceType.SEAWARD_EXPLORATION);
    assertThat(result).isTrue();
  }

  @Test
  void isRentalRatesFlat_WhenLicenceTypeHasNonFlatRentalRates_ReturnsFalse() {
    var result = underTest.isRentalRatesFlat(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isFalse();
  }

  @Test
  void isRentalRatesExponential_WhenLicenceTypeHasExponentialRentalRates_ReturnsTrue() {
    var result = underTest.isRentalRatesExponential(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void isRentalRatesExponential_WhenLicenceTypeHasNonExponentialRentalRates_ReturnsFalse() {
    var result = underTest.isRentalRatesExponential(LicenceType.SEAWARD_EXPLORATION);
    assertThat(result).isFalse();
  }

  @Test
  void hasWorkProgramme_WhenLicenceTypeHasWorkProgramme_ReturnsTrue() {
    var result = underTest.hasWorkProgramme(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void hasWorkProgramme_WhenLicenceTypeHasNoWorkProgramme_ReturnsFalse() {
    var result = underTest.hasWorkProgramme(LicenceType.A);
    assertThat(result).isFalse();
  }

  @Test
  void isWorkProgrammesTermTied_WhenLicenceTypeHasTermTiedWorkProgrammeAndAppraisalTerm_ReturnsTrue() {
    var result = underTest.isWorkProgrammesTermTied(LicenceType.CARBON_STORAGE, TermType.APPRAISAL);
    assertThat(result).isTrue();
  }

  @Test
  void isWorkProgrammesTermTied_WhenLicenceTypeHasWorkProgrammeAndAppraisalTerm_ReturnsFalse() {
    var result = underTest.isWorkProgrammesTermTied(LicenceType.SEAWARD_PRODUCTION, TermType.APPRAISAL);
    assertThat(result).isFalse();
  }

  @Test
  void isWorkProgrammesPhaseTied_WhenLicenceTypeHasWorkProgramme_ReturnsTrue() {
    var result = underTest.isWorkProgrammesPhaseTied(LicenceType.SEAWARD_PRODUCTION);
    assertThat(result).isTrue();
  }

  @Test
  void isWorkProgrammesPhaseTied_WhenLicenceTypeHasNonPhaseTiedWorkProgramme_ReturnsFalse() {
    var result = underTest.isWorkProgrammesPhaseTied(LicenceType.SEAWARD_EXPLORATION);
    assertThat(result).isFalse();
  }

  @Test
  void getSupportedEvents_WhenLicenceTypeHasSupportedEvents_ReturnsNonEmptySet() {
    var events = underTest.getSupportedEvents(LicenceType.SEAWARD_PRODUCTION);
    assertThat(events).isNotEmpty();
  }

  @Test
  void getSupportedEvents_WhenLicenceTypeHasNoSupportedEvents_ReturnsEmptySet() {
    var events = underTest.getSupportedEvents(LicenceType.A);
    assertThat(events).isEmpty();
  }
}
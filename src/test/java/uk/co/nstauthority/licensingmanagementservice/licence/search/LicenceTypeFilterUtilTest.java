package uk.co.nstauthority.licensingmanagementservice.licence.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

class LicenceTypeFilterUtilTest {

  private static final List<String> UNKNOWN_LICENCE_TYPE_NAMES = List.of(
      LicenceType.A.name(),
      LicenceType.AL.name(),
      LicenceType.B.name(),
      LicenceType.CE.name(),
      LicenceType.DL.name(),
      LicenceType.NA.name(),
      LicenceType.XL.name()
  );

  @Test
  void getOptions_thenDisplayableTypesInDisplayOrderFollowedByOther() {
    var result = LicenceTypeFilterUtil.getOptions();

    var expectedOptions = new LinkedHashMap<String, String>();
    expectedOptions.put(LicenceType.CARBON_STORAGE.name(), "Carbon storage");
    expectedOptions.put(LicenceType.GAS_STORAGE.name(), "Gas storage");
    expectedOptions.put(LicenceType.LANDWARD_EXPLORATION.name(), "Landward exploration");
    expectedOptions.put(LicenceType.LANDWARD_PRODUCTION.name(), "Landward production");
    expectedOptions.put(LicenceType.METHANE_DRAINAGE.name(), "Methane drainage");
    expectedOptions.put(LicenceType.SEAWARD_EXPLORATION.name(), "Seaward exploration");
    expectedOptions.put(LicenceType.SEAWARD_PRODUCTION.name(), "Seaward production");
    expectedOptions.put("OTHER", "Other");
    assertThat(result).containsExactlyEntriesOf(expectedOptions);
  }

  @Test
  void toLicenceTypeNames_whenNothingSelected_thenSelectionIsUnchanged() {
    var result = LicenceTypeFilterUtil.toLicenceTypeNames(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void toLicenceTypeNames_whenNullSelection_thenSelectionIsUnchanged() {
    var result = LicenceTypeFilterUtil.toLicenceTypeNames(null);

    assertThat(result).isNull();
  }

  @Test
  void toLicenceTypeNames_whenOtherNotSelected_thenSelectionIsUnchanged() {
    var selectedOptions = List.of(LicenceType.CARBON_STORAGE.name(), LicenceType.GAS_STORAGE.name());

    var result = LicenceTypeFilterUtil.toLicenceTypeNames(selectedOptions);

    assertThat(result).containsExactlyElementsOf(selectedOptions);
  }

  @Test
  void toLicenceTypeNames_whenOtherSelected_thenOtherIsReplacedByUnknownLicenceTypes() {
    var result = LicenceTypeFilterUtil.toLicenceTypeNames(List.of(LicenceType.CARBON_STORAGE.name(), "OTHER"));

    assertThat(result).containsExactlyInAnyOrderElementsOf(
        Stream.concat(Stream.of(LicenceType.CARBON_STORAGE.name()), UNKNOWN_LICENCE_TYPE_NAMES.stream()).toList());
  }

  @Test
  void toLicenceTypeNames_whenOnlyOtherSelected_thenOnlyUnknownLicenceTypesAreSelected() {
    var result = LicenceTypeFilterUtil.toLicenceTypeNames(List.of("OTHER"));

    assertThat(result).containsExactlyInAnyOrderElementsOf(UNKNOWN_LICENCE_TYPE_NAMES);
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

/**
 * Licence type filter options, being the displayable licence types plus an "Other" option which covers
 * every licence type with no known display name.
 */
public class LicenceTypeFilterUtil {

  static final String OTHER_OPTION = "OTHER";

  private static final String OTHER_OPTION_DISPLAY_NAME = "Other";

  private LicenceTypeFilterUtil() {
    throw new IllegalUtilClassInstantiationException(LicenceTypeFilterUtil.class);
  }

  public static Map<String, String> getOptions() {
    var options = new LinkedHashMap<>(DisplayableEnumOptionUtil.getDisplayableOptions(LicenceType.getDisplayableTypes()));
    options.put(OTHER_OPTION, OTHER_OPTION_DISPLAY_NAME);
    return options;
  }

  /**
   * Resolves the selected filter options to licence type names, replacing the "Other" option with the
   * licence types it covers.
   */
  public static List<String> toLicenceTypeNames(List<String> selectedOptions) {
    if (CollectionUtils.isEmpty(selectedOptions) || !selectedOptions.contains(OTHER_OPTION)) {
      return selectedOptions;
    }

    return Stream.concat(
        selectedOptions.stream().filter(option -> !OTHER_OPTION.equals(option)),
        LicenceType.getNonDisplayableTypes().stream().map(Enum::name)
    ).toList();
  }
}

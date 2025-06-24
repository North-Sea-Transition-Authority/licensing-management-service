package uk.co.nstauthority.licensingmanagementservice.util.enumutil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

public class DisplayableEnumOptionUtil {

  private DisplayableEnumOptionUtil() {
    throw new IllegalStateException("DisplayableEnumOptionUtil is a util class and should not be instantiated");
  }

  public static Map<String, String> getDisplayableOptions(
      Class<? extends Displayable> displayableOptionEnum
  ) {
    return Arrays.stream((Displayable[]) displayableOptionEnum.getEnumConstants())
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(Displayable::getEnumName, Displayable::getDisplayName));
  }

  public static Map<String, String> getDisplayableOptions(
      Collection<? extends Displayable> displayableOptionEnumConstants
  ) {
    return displayableOptionEnumConstants
        .stream()
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(
            Displayable::getEnumName,
            Displayable::getDisplayName
        ));
  }

  public static Map<String, String> getDisplayableOptionsFromStream(Stream<? extends Displayable> displayableStream) {
    return displayableStream
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(Displayable::getEnumName, Displayable::getDisplayName));
  }

  public static Map<String, String> getDisplayableOptionsWithDescription(
      Class<? extends DisplayableEnumWithDescription> displayableOptionEnum
  ) {
    return Arrays.stream((DisplayableEnumWithDescription[]) displayableOptionEnum.getEnumConstants())
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(
            Displayable::getEnumName,
            opt -> "%s (%s)".formatted(opt.getDescription(), opt.getDisplayName())
        ));
  }
}

package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.Map;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

final class DirectionLetterMockupData {

  static final Map<String, String> SIGNATORY_OPTIONS = Stream.of(
          Map.entry("1", "Morgan Reid (Authorised signatory)"),
          Map.entry("2", "Ellis Grant (Authorised signatory)"),
          Map.entry("3", "Robin Walsh (Authorised signatory)"))
      .collect(StreamUtil.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));

  private DirectionLetterMockupData() {
  }
}

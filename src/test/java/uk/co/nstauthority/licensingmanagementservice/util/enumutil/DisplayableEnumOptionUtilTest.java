package uk.co.nstauthority.licensingmanagementservice.util.enumutil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DisplayableEnumOptionUtilTest {

  @Test
  void getDisplayableOptionsWithSeparateNameAndDescription_returnsNameAndDescriptionInDisplayOrder() {
    var result = DisplayableEnumOptionUtil.getDisplayableOptionsWithSeparateNameAndDescription(TestOption.class);

    assertThat(result).containsExactly(
        Map.entry("SECOND", Map.of("Second option", "Second hint")),
        Map.entry("FIRST", Map.of("First option", "First hint"))
    );
  }

  private enum TestOption implements DisplayableEnumWithDescription {
    FIRST("First option", "First hint", 20),
    SECOND("Second option", "Second hint", 10);

    private final String displayName;
    private final String description;
    private final int displayOrder;

    TestOption(String displayName, String description, int displayOrder) {
      this.displayName = displayName;
      this.description = description;
      this.displayOrder = displayOrder;
    }

    @Override
    public String getDisplayName() {
      return displayName;
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public int getDisplayOrder() {
      return displayOrder;
    }
  }
}

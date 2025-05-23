package uk.co.nstauthority.licensingmanagementservice.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryValueType.FILE_VALUE;
import static uk.co.nstauthority.licensingmanagementservice.summary.SummaryValueType.STRING_VALUE;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryDataViewTest {

  private static final String KEY_1 = "key1";
  private static final String VALUE_1 = "value1";
  private static final String KEY_2 = "key 2";
  private static final List<String> VALUE_2 = List.of("value2a", "value2b", "value2c", "value2d", "value2e");
  private static final String KEY_3 = "key3";
  private static final UploadedFileView VALUE_3 = new UploadedFileView(
          "file id",
          "file name",
          "file size",
          "file desc",
          Instant.parse("2025-04-10T10:25:00Z"),
          "https://www.fivium.co.uk");
  private static final String KEY_4 = "key5";
  private static final List<UploadedFileView> VALUE_4 = List.of(
      new UploadedFileView(
          "file id2a",
          "file name2a",
          "file size2a",
          "file desc2a",
          Instant.parse("2025-04-10T10:35:00Z"),
          "https://www.fivium.co.uk"),
      new UploadedFileView(
          "file id2b",
          "file name2b",
          "file size2b",
          "file desc2b",
          Instant.parse("2025-04-10T10:45:00Z"),
          "https://www.fivium.co.uk")
  );

  @Test
  void newBuilder_addStringValue_build() {
    assertThat(SummaryDataView.newBuilder()
        .addStringValue(KEY_1, VALUE_1)
        .build())
        .isEqualTo(new SummaryDataView(
            List.of(
                new SummaryKeyValue(KEY_1, STRING_VALUE, List.of(VALUE_1)))
            )
        );
  }

  @Test
  void newBuilder_addStringValue_listOfStrings_build() {
    assertThat(SummaryDataView.newBuilder()
        .addStringValue(KEY_2, VALUE_2)
        .build())
        .isEqualTo(new SummaryDataView(
                List.of(
                    new SummaryKeyValue(KEY_2, STRING_VALUE, VALUE_2))
            )
        );
  }


  @Test
  void newBuilder_addFileValue_build() {
    assertThat(SummaryDataView.newBuilder()
        .addFileValue(KEY_3, VALUE_3)
        .build())
        .isEqualTo(new SummaryDataView(
                List.of(
                    new SummaryKeyValue(KEY_3, FILE_VALUE, List.of(VALUE_3)))
            )
        );
  }

  @Test
  void newBuilder_addFileValue_listOfFiles_build() {
    assertThat(SummaryDataView.newBuilder()
        .addFileValue(KEY_4, VALUE_4)
        .build())
        .isEqualTo(new SummaryDataView(
                List.of(
                    new SummaryKeyValue(KEY_4, FILE_VALUE, VALUE_4))
            )
        );
  }

  @Test
  void addStringValue_formatError() {
    var summaryData = SummaryDataView.
        newBuilder();
    assertThatThrownBy(() -> summaryData.addStringValue(KEY_1, 1L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Unexpected value class type: %s".formatted(Long.class.getName()));
  }
}

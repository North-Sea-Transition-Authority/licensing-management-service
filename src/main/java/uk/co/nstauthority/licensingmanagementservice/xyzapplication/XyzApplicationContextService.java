package uk.co.nstauthority.licensingmanagementservice.xyzapplication;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.UploadedFileView;

@Service
public class XyzApplicationContextService {

  //Mock-up for the purposes of creating something in work-area
  public XyzApplicationContext getContextForApplication(XyzApplication xyzApplication) {
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("key1", "value1")
        .addStringValue("key2", "value2")
        .addStringValue("key3", List.of("value3a", "value3b", "value3c"))
        .addFileValue(
            "dummy summary question",
            new UploadedFileView(
                "dummy-file-id",
                "dummy-file-name",
                "999 Bytes",
                "dummy-file-description",
                Instant.now(),
                "https://www.google.com")
        )
        .build();
    var summaryDataViewList = List.of(summaryDataView);
    if ("Double stacked application".equals(xyzApplication.getType())) {
      var summaryDataView2 = SummaryDataView.newBuilder()
          .addStringValue("key5", "value5")
          .addStringValue("key6", "value6")
          .addStringValue("key7", "value7")
          .addStringValue("key8", "value8")
          .build();
      summaryDataViewList = List.of(summaryDataView, summaryDataView2);
    }
    return new XyzApplicationContext(
        xyzApplication.getReference(),
        xyzApplication.getType(),
        summaryDataViewList
    );
  }

  public record XyzApplicationContext(
      String reference,
      String type,
      List<SummaryDataView> summaryDataView
  ) {}
}

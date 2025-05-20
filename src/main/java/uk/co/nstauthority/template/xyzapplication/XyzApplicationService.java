package uk.co.nstauthority.template.xyzapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.summary.SummaryCard;
import uk.co.nstauthority.template.summary.SummaryDataView;
import uk.co.nstauthority.template.summary.SummaryItem;
import uk.co.nstauthority.template.summary.UploadedFileView;

@Service
public class XyzApplicationService {

  public List<XyzApplication> finalAllMockedApplications() {
    var xyzApplication1 = new XyzApplication();
    xyzApplication1.setId(UUID.fromString("1cc7509d-63c4-4e59-b196-d10351b10cc8"));
    xyzApplication1.setReference("Reference1 without tag");
    xyzApplication1.setType("Application without tag");
    xyzApplication1.setStatus(XyzApplicationStatus.DRAFT);

    var xyzApplication2 = new XyzApplication();
    xyzApplication2.setId(UUID.fromString("c0d13e02-0e9b-49dd-8ba6-711566ac8c3d"));
    xyzApplication2.setReference("Reference2 with Green tag");
    xyzApplication2.setType("Application with Green tag");
    xyzApplication2.setStatus(XyzApplicationStatus.SUBMITTED);

    var xyzApplication3 = new XyzApplication();
    xyzApplication3.setId(UUID.fromString("5887cb7e-9b94-40e2-a3a3-394c2e1a82d4"));
    xyzApplication3.setReference("Reference3 with Yellow tag");
    xyzApplication3.setType("Application with Yellow tag");
    xyzApplication3.setStatus(XyzApplicationStatus.SUBMITTED);

    var xyzApplication4 = new XyzApplication();
    xyzApplication4.setId(UUID.fromString("29dd296a-e194-467f-95b1-a302bed78b89"));
    xyzApplication4.setReference("Reference4 without tag");
    xyzApplication4.setType("Double stacked application");
    xyzApplication4.setStatus(XyzApplicationStatus.APPROVED);

    return List.of(
        xyzApplication1,
        xyzApplication2,
        xyzApplication3,
        xyzApplication4
    );
  }

  public XyzApplication getXyzApplicationById(UUID applicationId) {
    return findXyzApplicationById(applicationId)
        .orElseThrow(() ->
            new EntityNotFoundException("XyzApplication not found with id %s".formatted(applicationId))
        );
  }

  public Optional<XyzApplication> findXyzApplicationById(UUID applicationId) {
    return finalAllMockedApplications().stream()
        .filter(application -> application.getId().equals(applicationId))
        .findFirst();
  }

  public List<XyzApplication> getXyzApplicationsByReference(String reference) {
    return finalAllMockedApplications()
        .stream()
        .filter(xyzApplication -> StringUtils.containsIgnoreCase(xyzApplication.getReference(), reference))
        .toList();
  }

  public SummaryItem getXyzApplicationSpecificSummaryItem(XyzApplication xyzApplication, String sectionName) {
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Application Reference", xyzApplication.getReference())
        .addStringValue("Id", xyzApplication.getId().toString())
        .addStringValue("Status", xyzApplication.getStatus())
        .addStringValue("Type", xyzApplication.getType())
        .build();

    var summaryCard = SummaryCard.simpleSummaryCardWithHeading("Specific xyzApplication", summaryDataView);

    return SummaryItem.withCard(sectionName, summaryCard);
  }

  public SummaryItem getXyzApplicationGenericSummaryItem(XyzApplication xyzApplication, String sectionName) {
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Application Reference", xyzApplication.getReference())
        .addStringValue("Second key", "singular value")
        .addStringValue("Fourth key", List.of("list value1", "list value2", "list value3"))
        .build();

    var summaryCard = SummaryCard.simpleSummaryCardWithHeading("Generic xyzApplication", summaryDataView);

    return SummaryItem.withCard(sectionName, summaryCard);
  }

  public SummaryItem getXyzApplicationSummaryItemWithFile(XyzApplication xyzApplication, String sectionName) {
    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("File download for: ", xyzApplication.getReference())
        .addFileValue(
            "First Question expecting 2 Files",
            List.of(
                new UploadedFileView(
                    "11",
                    "FirstQuestionFirstFileName.png",
                    "100 KB",
                    "The First file wanted for the First Question.",
                    Instant.parse("2025-04-10T10:25:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                ),
                new UploadedFileView(
                    "12",
                    "FirstQuestionSecondFileName.png",
                    "100 MB",
                    "The Second file wanted for the First Question.",
                    Instant.parse("2025-04-10T10:35:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                )
            )
        )
        .addStringValue("First key that goes above the files", "First value that goes above the files")
        .addFileValue(
            "Second Question expecting 1 File",
            new UploadedFileView(
                "20",
                "SecondQuestionOnlyFileName.png",
                "100 GB",
                "The Only file wanted for the First Question.",
                Instant.parse("2025-04-10T10:45:00Z"),
                ReverseRouter.route(on(XyzApplicationProcessingController.class)
                    .getApplicationProcessing(xyzApplication, null))
            )
        )
        .addFileValue(
            "Third Question expecting 0 Files",
            List.of()
        )
        .addFileValue(
            "Fourth Question expecting 5 Files",
            List.of(
                new UploadedFileView(
                    "41",
                    "FourthQuestionFirstFileName.png",
                    "100 TB",
                    "The first file wanted for the fourth question.",
                    Instant.parse("2025-04-10T11:15:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                ),
                new UploadedFileView(
                    "42",
                    "FourthQuestionSecondFileName.png",
                    "100 PB",
                    "The second file wanted for the fourth question.",
                    Instant.parse("2025-04-10T11:20:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                ),
                new UploadedFileView(
                    "43",
                    "FourthQuestionThirdFileName.png",
                    "100 XB",
                    "The third file wanted for the fourth question.",
                    Instant.parse("2025-04-10T11:25:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                ),
                new UploadedFileView(
                    "44",
                    "FourthQuestionFourthFileName.png",
                    "100 ZB",
                    "The fourth file wanted for the fourth question.",
                    Instant.parse("2025-04-10T11:30:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                ),
                new UploadedFileView(
                    "45",
                    "FourthQuestionFifthFileName.png",
                    "100 YB",
                    "The fifth file wanted for the fourth question.",
                    Instant.parse("2025-04-10T11:35:00Z"),
                    ReverseRouter.route(on(XyzApplicationProcessingController.class)
                        .getApplicationProcessing(xyzApplication, null))
                )
            )
        )
        .addStringValue("Second key that goes above the files", "Second value that goes above the files")
        .build();

    var summaryCard = SummaryCard.simpleSummaryCardWithHeading("Generic xyzApplication", summaryDataView);

    return SummaryItem.withCard(sectionName, summaryCard);
  }
}

package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

// Public so Freemarker can read the records. Built from the journey, so it follows what was recorded in the session
final class DpeaSummary {

  // Links point nowhere: the mockup has no real uploaded file to download
  public record Row(String label, List<String> values, boolean links) {

    Row(String label, List<String> values) {
      this(label, values, false);
    }
  }

  public record Section(String heading, List<Row> rows) {
  }

  private static final String LICENCE = "P1234 ('Licence')";
  private static final String DECISION =
      "Grant an extension to the Second Term of the Licence from the current date of expiry (30 June 2026) " +
          "for a period of 24 months to 30 June 2028, subject to the following milestone:";
  private static final String MILESTONE = "\"By 31 December 2027, the Licensee shall submit a Field Development Plan.\"";

  private DpeaSummary() {
  }

  static List<Section> build(UploadDspMockupJourney journey) {
    var details = journey.decisionDetails();
    var sections = new ArrayList<Section>();

    sections.add(new Section(null, List.of(
        row("Document", documentName(journey.recordedDocumentType())),
        row("Parties", "NSTA, " + UploadDspMockupJourney.LICENSEES.stream()
            .map(UploadDspMockupJourney.Licensee::name).collect(Collectors.joining(", "))),
        row("Licence", LICENCE)
    )));

    var decision = new ArrayList<Row>();
    decision.add(new Row("Decision", List.of(DECISION, MILESTONE)));
    details.ifPresent(form -> {
      decision.add(row("Decision maker", DecisionDetailsMockupData.decisionMakerName(form.getDecisionMaker())));
      if ("YES".equals(form.getDelegated())) {
        decision.add(row("Delegated to", DecisionDetailsMockupData.decisionMakerName(form.getDelegatedTo())));
      }
    });
    decision.add(row("Date", DateFormatUtil.convertToDisplayText(journey.decisionDate())));
    sections.add(new Section("Decision", decision));

    var dsp = new ArrayList<Row>();
    dsp.add(new Row("Final DSP", List.of(journey.currentDspFileName()), true));
    if (!journey.addendumFileNames().isEmpty()) {
      dsp.add(new Row("DSP addendums", journey.addendumFileNames(), true));
    }
    journey.dspUploadedAt().ifPresent(at -> dsp.add(row("Date", DateUtil.formatLongDate(at))));
    sections.add(new Section("DSP", dsp));

    sections.add(new Section("Document Preparation Assurance", List.of(
        row("Licensee agreement", journey.latestEvent(UploadDspMockupJourney.DECISION_ACCEPTED)
            .map(event -> "Yes: " + DateUtil.formatLongDate(event.at()))
            .orElse("No")),
        row("Legal approval", legalApproval(details.map(DecisionDetailsMockupForm::getLegalApproval).orElse(null))),
        row("Ancillary documents required", "None")
    )));

    var execution = new ArrayList<Row>();
    execution.add(new Row("RACI consultees", details
        .map(form -> form.getRaciConsulteeIds().stream().map(DecisionDetailsMockupData::consulteeName).toList())
        .orElse(List.of("None recorded"))));
    execution.add(row("Did all consultees respond?", "Yes"));
    execution.add(row("Decision maker consistent with Delegation Framework?", "Yes"));
    sections.add(new Section("Document Execution Assurance", execution));

    return sections;
  }

  private static Row row(String label, String value) {
    return new Row(label, List.of(value));
  }

  private static String documentName(String documentType) {
    if (documentType == null) {
      return "Not recorded";
    }
    return "DOV".equals(documentType) ? "Deed of Variation" : "Direction letter";
  }

  static String legalApproval(String legalApproval) {
    return switch (String.valueOf(legalApproval)) {
      case "YES" -> "Yes";
      case "NO" -> "No";
      default -> "N/A (no obligations/milestones are included)";
    };
  }

}

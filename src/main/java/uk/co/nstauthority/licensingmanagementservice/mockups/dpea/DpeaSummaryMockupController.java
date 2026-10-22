package uk.co.nstauthority.licensingmanagementservice.mockups.dpea;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationContext;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryTableView;

@Controller
@RequestMapping("/mockups/dpea-summary")
@Profile("mockups")
public class DpeaSummaryMockupController {

  @GetMapping
  public ModelAndView renderMockup() {
    var applicationContext = new ScheduleWorkProgrammeApplicationContext(
        "P1234/5678",
        "P1234 - Traditional",
        List.of(SummaryDataView.newBuilder()
            .addStringValue("Submitted by", "John Doe")
            .addStringValue("Submission date", "12 Apr 2026 14:30")
            .addStringValue("Steward", "Jane Smith")
            .build())
    );

    var licenceSection = new SummarySection(10, List.of(
        SummaryItem.withCard("Licence",
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Licence", "P1234")
                .addStringValue("Round", "32nd")
                .addStringValue("Blocks", "21/1a, 21/1b")
                .addStringValue("Prospect / discovery / field", "Brent")
                .addStringValue("Licensees", "Shell UK limited, BP Exploration Operating Company Limited")
                .build()))
    ));

    var scheduleSection = new SummarySection(20, List.of(
        SummaryItem.withCard("Schedule",
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Start date", "01 Jan 2021")
                .addStringValue("Initial term expiry", "31 Dec 2026")
                .addStringValue("Current phase/term expiry", "Phase C - 31 Dec 2026")
                .addStringValue("Phase/term durations", List.of(
                    "Phase A: 2 years",
                    "Phase B: 2 years",
                    "Phase C: 2 years",
                    "Second term: 4 years",
                    "Third term: 18 years"))
                .build()))
    ));

    var workProgrammeSection = new SummarySection(30, List.of(
        SummaryItem.withCard("Work programme",
            SummaryCard.tableSummaryCard(SummaryTableView.newWithHeading("Description", "Term/phase", "Status")
                .addRow("Seismic reprocessing and geological study.", "Phase A", WorkProgrammeStatus.COMPLETE.getDisplayName())
                .addRow("Acquisition of new broadband 3D seismic data.", "Phase B", WorkProgrammeStatus.COMPLETE.getDisplayName())
                .addRow("Drill exploration well 21/1a-4.", "Phase C", WorkProgrammeStatus.IN_PROGRESS.getDisplayName())
                .addRow("Drill one appraisal well.", "Second term", WorkProgrammeStatus.OPEN.getDisplayName())
                .addRow("FDP submission and First Oil.", "Third term", WorkProgrammeStatus.OPEN.getDisplayName())))
    ));

    var requestSummarySection = new SummarySection(40, List.of(
        SummaryItem.withCard("Request summary",
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Type of request", List.of("Extension", "Work programme amendment"))
                .addStringValue("Date of request", "10 Apr 2026")
                .addStringValue("Requesting licensee", "Shell UK limited")
                .addStringValue("Made on behalf of all licensees", "Yes")
                .addStringValue("Reason for request", "Technical delays due to equipment failure")
                .addStringValue("Previous extensions or amendments", "None")
                .build()))
    ));

    var extensionRequestSection = new SummarySection(50, List.of(
        SummaryItem.withCard("Extension details",
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Initial term extension duration", "1 year, 0 months, 0 days")
                .build()))
    ));

    var workProgrammeRequestSection = new SummarySection(60, List.of(
        SummaryItem.withCards("Work programme amendments", List.of(
            SummaryCard.simpleSummaryCardWithHeading("Drill exploration well 21/1a-4 to 3,500m", SummaryDataView.newBuilder()
                .addStringValue("Completion date change requested", "Yes")
                .addStringValue("Requested extension to completion date", "1 year")
                .addStringValue("Work programme content change requested", "No")
                .build())
        ))
    ));

    var supportingInformationSection = new SummarySection(70, List.of(
        SummaryItem.withCard("Supporting information",
            SummaryCard.simpleSummaryCard(SummaryDataView.newBuilder()
                .addStringValue("Reason for request", "Technical delays due to equipment failure")
                .addStringValue("Licence progress", "Phase A & B completed. Well 21/1a-4 spudded Mar 2026.")
                .addStringValue("Plan during extension", "Complete well 21/1a-4 drilling and testing.")
                .addStringValue("Impact on deliverables", "12-month deferral of Phase C completion.")
                .build()))
    ));

    var summarySections = List.of(
        licenceSection,
        scheduleSection,
        workProgrammeSection,
        requestSummarySection,
        extensionRequestSection,
        workProgrammeRequestSection,
        supportingInformationSection
    );

    var dpeaSummary = Map.ofEntries(
        entry("document", "Deed of Variation"),
        entry("parties", "NSTA, Shell UK limited ('Shell'), BP Exploration Operating Company Limited ('BP')"),
        entry("licence", "P1234 ('Licence')"),
        entry("decision", "Grant an extension to the Second Term of the Licence from the current date of " +
            "expiry (30 June 2026) for a period of 24 months to 30 June 2028, subject to the following " +
            "milestone:<br><br>\"By 31 December 2027, the Licensee shall submit a Field Development Plan.\""),
        entry("decisionDate", "12 Apr 2026"),
        entry("dspTitle", "Operations Directorate - Decision Support Paper: P1234 Second Term Extension"),
        entry("dspDate", "10 Apr 2026"),
        entry("licenseeAgreement", "Yes: 20 Apr 2026"),
        entry("legalApproval", "N/A (no obligations/milestones are included)"),
        entry("ancillaryDocuments", "None"),
        entry("raciConsultees", "Licensing Coordination Manager<br>Head of Licensing & Consents<br>" +
            "Business Manager (Area Team)<br>Senior Exploration Geologist<br>Legal"),
        entry("allConsulteesResponded", "Yes"),
        entry("delegationFrameworkConsistent", "Yes")
    );

    return new ModelAndView("lms/mockups/dpea/dpeaSummary")
        .addObject("applicationContext", applicationContext)
        .addObject("summarySections", summarySections)
        .addObject("dpeaSummary", dpeaSummary)
        .addObject("accordionId", UUID.randomUUID().toString())
        .addObject("applicationActions", List.of());
  }
}

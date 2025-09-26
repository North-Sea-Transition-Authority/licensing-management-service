package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class MockWorkAmendment {
  final UUID id;
  final String label;
  final String description;
  final LocalDate startDate;
  final LocalDate endDate;

  public MockWorkAmendment(UUID id, String label, String description, LocalDate startDate, LocalDate endDate) {
    this.id = id;
    this.label = label;
    this.description = description;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public static List<MockWorkAmendment> getMockWorkAmendments() {
    return List.of(
        new MockWorkAmendment(UUID.fromString("696ebc6b-f475-46e7-9ee6-c7a2bd95a8da"),
            "Reprocess 90 sq kms 3D seismic data to depth",
            "The Licensee shall reprocess 90km2 of 3D seismic data to PSTM and PreSDM",
            LocalDate.now(),
            LocalDate.now().plusYears(2)),
        new MockWorkAmendment(UUID.fromString("696ebc6b-f475-46e7-9ee6-c7a2bd95a8db"),
            "Drill or drop one well to 2900m or 100m into the Carboniferous WITS",
            """
                Drill-or-drop commitment
                    "The Licensee shall either:
                    "(a) drill one well to 2900m or 100m into the Carboniferous, whichever is the shallower, or:
                    "(b) elect to allow the licence to automatically cease and determine pursuant to...
                """,
            LocalDate.now(), LocalDate.now().plusYears(3))
    );

  }


  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }
}
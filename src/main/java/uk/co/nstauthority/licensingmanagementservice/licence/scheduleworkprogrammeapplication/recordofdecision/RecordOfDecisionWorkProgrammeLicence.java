package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Audited
@Entity(name = "swp_record_of_decision_work_programme_licence")
public class RecordOfDecisionWorkProgrammeLicence {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "record_of_decision_work_programme_id")
  private RecordOfDecisionWorkProgramme recordOfDecisionWorkProgramme;

  @ManyToOne
  @JoinColumn(name = "licence_id")
  private Licence licence;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RecordOfDecisionWorkProgramme getRecordOfDecisionWorkProgramme() {
    return recordOfDecisionWorkProgramme;
  }

  public void setRecordOfDecisionWorkProgramme(RecordOfDecisionWorkProgramme recordOfDecisionWorkProgramme) {
    this.recordOfDecisionWorkProgramme = recordOfDecisionWorkProgramme;
  }

  public Licence getLicence() {
    return licence;
  }

  public void setLicence(Licence licence) {
    this.licence = licence;
  }
}

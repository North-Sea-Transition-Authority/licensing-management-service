package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity(name = "swp_record_of_decision_work_programme")
public class RecordOfDecisionWorkProgramme {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_detail_id")
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @ManyToOne
  @JoinColumn(name = "work_programme_activity_id")
  private WorkProgrammeActivity workProgrammeActivity;

  @Enumerated(EnumType.STRING)
  @Column
  private WorkProgrammeAmendmentDecision decision;

  private Boolean amendDuration;

  private Boolean amendText;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "amended_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "amended_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "amended_duration_years"))
  private ThreeFieldDuration amendedDuration;

  private String amendedText;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetail() {
    return scheduleWorkProgrammeApplicationDetail;
  }

  public void setScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    this.scheduleWorkProgrammeApplicationDetail = scheduleWorkProgrammeApplicationDetail;
  }

  public WorkProgrammeActivity getWorkProgrammeActivity() {
    return workProgrammeActivity;
  }

  public void setWorkProgrammeActivity(WorkProgrammeActivity workProgrammeActivity) {
    this.workProgrammeActivity = workProgrammeActivity;
  }

  public WorkProgrammeAmendmentDecision getDecision() {
    return decision;
  }

  public void setDecision(WorkProgrammeAmendmentDecision decision) {
    this.decision = decision;
  }

  public Boolean getAmendDuration() {
    return amendDuration;
  }

  public void setAmendDuration(Boolean amendDuration) {
    this.amendDuration = amendDuration;
  }

  public Boolean getAmendText() {
    return amendText;
  }

  public void setAmendText(Boolean amendText) {
    this.amendText = amendText;
  }

  public ThreeFieldDuration getAmendedDuration() {
    return amendedDuration;
  }

  public void setAmendedDuration(ThreeFieldDuration amendedDuration) {
    this.amendedDuration = amendedDuration;
  }

  public String getAmendedText() {
    return amendedText;
  }

  public void setAmendedText(String amendedText) {
    this.amendedText = amendedText;
  }
}

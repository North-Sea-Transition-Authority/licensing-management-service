package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import org.hibernate.envers.Audited;

/** A row exists only once one of its values has been set. */
@Audited
@Entity(name = "schedule_work_programme_applications")
public class ScheduleWorkProgrammeApplicationData {

  @Id
  private UUID id;

  @MapsId
  @OneToOne
  @JoinColumn(name = "id")
  private ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication;

  @Column
  private Long stewardWuaId;

  protected ScheduleWorkProgrammeApplicationData() {
  }

  ScheduleWorkProgrammeApplicationData(ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
    this.scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplication;
  }

  public UUID getId() {
    return id;
  }

  public ScheduleWorkProgrammeApplication getScheduleWorkProgrammeApplication() {
    return scheduleWorkProgrammeApplication;
  }

  public Long getStewardWuaId() {
    return stewardWuaId;
  }

  void setStewardWuaId(Long stewardWuaId) {
    this.stewardWuaId = stewardWuaId;
  }
}

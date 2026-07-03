package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.duplication.LinkedToDuplicationParent;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Audited
@Entity
@Table(name = "work_programme_activities")
@DiscriminatorValue("WORK_PROGRAMME_ACTIVITY")
public class WorkProgrammeActivity extends ScheduleEvent implements LinkedToDuplicationParent<LicenceScheduleDetail> {

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  @Enumerated(EnumType.STRING)
  private WorkProgrammeActivityCategory category;

  private String otherCategoryName;

  private String description;

  @Enumerated(EnumType.STRING)
  private WorkProgrammeActivityCommitment commitment;

  @Enumerated(EnumType.STRING)
  private WorkProgrammeActivityDateOption dateOption;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_term_id")
  private LicenceScheduleTerm licenceScheduleTerm;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_phase_id")
  private LicenceSchedulePhase licenceSchedulePhase;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "relative_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "relative_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "relative_duration_years"))
  private ThreeFieldDuration relativeDuration;

  private LocalDate dueDate;

  public LicenceScheduleDetail getLicenceScheduleDetail() {
    return licenceScheduleDetail;
  }

  public void setLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    this.licenceScheduleDetail = licenceScheduleDetail;
  }

  @Override
  public void setDuplicationParent(LicenceScheduleDetail licenceScheduleDetail) {
    setLicenceScheduleDetail(licenceScheduleDetail);
  }

  public WorkProgrammeActivityCategory getCategory() {
    return category;
  }

  public void setCategory(WorkProgrammeActivityCategory category) {
    this.category = category;
  }

  public String getOtherCategoryName() {
    return otherCategoryName;
  }

  public void setOtherCategoryName(String otherCategoryName) {
    this.otherCategoryName = otherCategoryName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WorkProgrammeActivityCommitment getCommitment() {
    return commitment;
  }

  public void setCommitment(WorkProgrammeActivityCommitment commitment) {
    this.commitment = commitment;
  }

  public WorkProgrammeActivityDateOption getDateOption() {
    return dateOption;
  }

  public void setDateOption(WorkProgrammeActivityDateOption dateOption) {
    this.dateOption = dateOption;
  }

  public LicenceScheduleTerm getLicenceScheduleTerm() {
    return licenceScheduleTerm;
  }

  public void setLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm) {
    this.licenceScheduleTerm = licenceScheduleTerm;
  }

  public LicenceSchedulePhase getLicenceSchedulePhase() {
    return licenceSchedulePhase;
  }

  public void setLicenceSchedulePhase(LicenceSchedulePhase licenceSchedulePhase) {
    this.licenceSchedulePhase = licenceSchedulePhase;
  }

  public ThreeFieldDuration getRelativeDuration() {
    return relativeDuration;
  }

  public void setRelativeDuration(ThreeFieldDuration relativeDuration) {
    this.relativeDuration = relativeDuration;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public String getCategoryString() {
    return category.equals(WorkProgrammeActivityCategory.OTHER_ACTIVITY)
        ? otherCategoryName
        : category.getDisplayName();
  }

  public Licence getLicence() {
    return this.getLicenceScheduleDetail()
        .getLicenceSchedule()
        .getLicence();
  }

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.WORK_PROGRAMME_ACTIVITY;
  }

  @Override
  public String getEventCaption() {
    return getCategoryString();
  }
}

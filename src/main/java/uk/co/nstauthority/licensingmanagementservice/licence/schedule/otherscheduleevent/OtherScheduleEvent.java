package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.duplication.LinkedToDuplicationParent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Audited
@Entity(name = "other_schedule_events")
public class OtherScheduleEvent implements LinkedToDuplicationParent<LicenceScheduleDetail> {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  @Enumerated(EnumType.STRING)
  private OtherScheduleEventCategory category;

  private String otherCategoryName;

  private String description;

  @Enumerated(EnumType.STRING)
  private OtherScheduleEventDateOption dateOption;

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

  private LocalDate eventDate;

  private String comments;

  @Enumerated(EnumType.STRING)
  private LicenceScheduleEventStatus status;

  private UUID eventReference;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

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

  public OtherScheduleEventCategory getCategory() {
    return category;
  }

  public void setCategory(OtherScheduleEventCategory category) {
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

  public OtherScheduleEventDateOption getDateOption() {
    return dateOption;
  }

  public void setDateOption(OtherScheduleEventDateOption dateOption) {
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

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate dueDate) {
    this.eventDate = dueDate;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public LicenceScheduleEventStatus getStatus() {
    return status;
  }

  public void setStatus(LicenceScheduleEventStatus status) {
    this.status = status;
  }

  public String getCategoryString() {
    return category.equals(OtherScheduleEventCategory.OTHER_ACTIVITY)
        ? otherCategoryName
        : category.getDisplayName();
  }

  public UUID getEventReference() {
    return eventReference;
  }

  public void setEventReference(UUID eventReference) {
    this.eventReference = eventReference;
  }
}

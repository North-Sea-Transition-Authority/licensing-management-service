package uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.duplication.LinkedToDuplicationParent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

/**
 * A retention, development or pre-producing area event ({@code RD} onshore, {@code PA} offshore) on a schedule version,
 * held as PEARS holds it. LMS doesn't manage these areas yet, so the events are kept as they are rather than as
 * schedule events.
 */
@Entity
@Table(name = "licence_schedule_area_events")
@Audited
public class LicenceScheduleAreaEvent implements LinkedToDuplicationParent<LicenceScheduleDetail> {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  private String eventMnem;

  private String subtype;

  private LocalDate eventDate;

  private String comments;

  private String areaName;

  private String areaShortName;

  private String areaType;

  private String areaVersion;

  private String pearsEventId;

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

  public String getEventMnem() {
    return eventMnem;
  }

  public void setEventMnem(String eventMnem) {
    this.eventMnem = eventMnem;
  }

  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public String getAreaName() {
    return areaName;
  }

  public void setAreaName(String areaName) {
    this.areaName = areaName;
  }

  public String getAreaShortName() {
    return areaShortName;
  }

  public void setAreaShortName(String areaShortName) {
    this.areaShortName = areaShortName;
  }

  public String getAreaType() {
    return areaType;
  }

  public void setAreaType(String areaType) {
    this.areaType = areaType;
  }

  public String getAreaVersion() {
    return areaVersion;
  }

  public void setAreaVersion(String areaVersion) {
    this.areaVersion = areaVersion;
  }

  public String getPearsEventId() {
    return pearsEventId;
  }

  public void setPearsEventId(String pearsEventId) {
    this.pearsEventId = pearsEventId;
  }
}

package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Audited
@Entity(name = "licence_positions")
@NamedEntityGraph(
    name = "licencePosition",
    attributeNodes = {
        @NamedAttributeNode(value = "licence"),
        @NamedAttributeNode(value = "licenceTransaction")
    }
)
public class LicencePosition {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_id")
  private Licence licence;

  @ManyToOne
  private LicenceTransaction licenceTransaction;

  private LocalDate positionDate;

  private int positionDateOrder;

  private boolean isExecuted;

  /**
   * The gis framework features (blocks, subareas) this position holds. Held as ids rather than as a
   * relationship to {@code Feature}: those tables belong to the gis framework's own flyway instance,
   * which runs after this application's, so this schema cannot reference them.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  private Set<UUID> featureIds;

  public LicencePosition() {

  }

  LicencePosition(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public Licence getLicence() {
    return licence;
  }

  public void setLicence(Licence licence) {
    this.licence = licence;
  }

  public LicenceTransaction getLicenceTransaction() {
    return licenceTransaction;
  }

  public void setLicenceTransaction(LicenceTransaction licenceTransaction) {
    this.licenceTransaction = licenceTransaction;
  }

  public LocalDate getPositionDate() {
    return positionDate;
  }

  public void setPositionDate(LocalDate positionDate) {
    this.positionDate = positionDate;
  }

  public int getPositionDateOrder() {
    return positionDateOrder;
  }

  public void setPositionDateOrder(int positionDateOrder) {
    this.positionDateOrder = positionDateOrder;
  }

  public String getFormattedPositionDate() {
    return DateUtil.formatLongDate(this.positionDate);
  }

  public boolean isExecuted() {
    return isExecuted;
  }

  public void setExecuted(boolean executed) {
    isExecuted = executed;
  }

  public Set<UUID> getFeatureIds() {
    return featureIds;
  }

  public void setFeatureIds(Set<UUID> featureIds) {
    this.featureIds = featureIds;
  }
}

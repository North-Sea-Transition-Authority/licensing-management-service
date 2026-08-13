package uk.co.fivium.gisframework.feature;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Entity
@Table(name = "gis_framework_features")
@Audited
public class Feature {

  @Id
  @UuidGenerator
  private UUID id;

  private String featureName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, String> attributes;

  @Enumerated(EnumType.STRING)
  private CoordinateSystem coordinateSystem;

  private BigDecimal featureArea;

  @ManyToOne
  @JoinColumn(name = "parent_feature_id")
  private Feature parentFeature;

  private Integer legacyId;

  private LocalDate startDate;

  private LocalDate endDate;

  @VisibleForTesting
  Feature(UUID id) {
    this.id = id;
  }

  public Feature() {
  }

  public UUID getId() {
    return id;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public CoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }

  public void setCoordinateSystem(CoordinateSystem srsWkid) {
    this.coordinateSystem = srsWkid;
  }

  public BigDecimal getFeatureArea() {
    return featureArea;
  }

  public void setFeatureArea(BigDecimal featureArea) {
    this.featureArea = featureArea;
  }

  public Feature getParentFeature() {
    return parentFeature;
  }

  public void setParentFeature(Feature parentFeature) {
    this.parentFeature = parentFeature;
  }

  public Integer getLegacyId() {
    return legacyId;
  }

  public void setLegacyId(Integer legacyId) {
    this.legacyId = legacyId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }
}

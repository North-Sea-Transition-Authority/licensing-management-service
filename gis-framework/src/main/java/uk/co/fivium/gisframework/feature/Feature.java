package uk.co.fivium.gisframework.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.fivium.grpc.CoordinateSystem;

@Entity
@Table(name = "gis_framework_features")
@Audited
public class Feature {

  @Id
  @UuidGenerator
  private UUID id;

  private String featureName;

  @Enumerated(EnumType.STRING)
  @Column(name = "feature_type")
  private FeatureType type;

  @Enumerated(EnumType.STRING)
  private CoordinateSystem coordinateSystem;

  private BigDecimal featureArea;

  @ManyToOne
  @JoinColumn(name = "parent_feature_id")
  private Feature parentFeature;

  public UUID getId() {
    return id;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public FeatureType getType() {
    return type;
  }

  public void setType(FeatureType type) {
    this.type = type;
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
}

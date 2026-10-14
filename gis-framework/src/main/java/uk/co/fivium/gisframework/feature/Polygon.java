package uk.co.fivium.gisframework.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "gis_framework_polygons")
@Audited
public class Polygon {

  public static final long INFINITE_DEPTH = Long.MIN_VALUE;

  @Id
  @UuidGenerator
  private UUID id;

  @JoinColumn(name = "feature_id")
  @ManyToOne
  private Feature feature;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes;

  private Long startDepth;

  private Long endDepth;

  private Integer legacyId;

  Polygon(UUID id) {
    this.id = id;
  }

  public Polygon() {
  }

  public UUID getId() {
    return id;
  }

  public Feature getFeature() {
    return feature;
  }

  public void setFeature(Feature feature) {
    this.feature = feature;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public Long getStartDepth() {
    return startDepth;
  }

  public void setStartDepth(Long startDepth) {
    this.startDepth = startDepth;
  }

  public Long getEndDepth() {
    return endDepth;
  }

  public void setEndDepth(Long endDepth) {
    this.endDepth = endDepth;
  }

  public Integer getLegacyId() {
    return legacyId;
  }

  public void setLegacyId(Integer legacyId) {
    this.legacyId = legacyId;
  }

  public long getEffectiveStartDepth() {
    return startDepth == null ? 0L : startDepth;
  }

  public long getEffectiveEndDepth() {
    return endDepth == null ? INFINITE_DEPTH : endDepth;
  }

  public boolean coversDepth(long depth) {
    return getEffectiveStartDepth() >= depth && getEffectiveEndDepth() < depth;
  }
}

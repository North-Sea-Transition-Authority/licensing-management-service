package uk.co.fivium.gisframework.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import uk.co.fivium.grpc.gis.LineNavigationType;

@Entity
@Table(name = "gis_framework_lines")
@Audited
public class Line {

  @Id
  @UuidGenerator
  private UUID id;

  @JoinColumn(name = "polygon_id")
  @ManyToOne
  private Polygon polygon;

  @Enumerated(EnumType.STRING)
  private LineNavigationType navigationType;

  private Integer ringNumber;

  private Integer ringConnectionOrder;

  private String esriJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> attributes;

  private Integer legacyId;

  Line(UUID id) {
    this.id = id;
  }

  public Line() {
  }

  public UUID getId() {
    return id;
  }

  public Polygon getPolygon() {
    return polygon;
  }

  public void setPolygon(Polygon polygon) {
    this.polygon = polygon;
  }

  public LineNavigationType getNavigationType() {
    return navigationType;
  }

  public void setNavigationType(LineNavigationType navigationType) {
    this.navigationType = navigationType;
  }

  public Integer getRingNumber() {
    return ringNumber;
  }

  public void setRingNumber(Integer ringNumber) {
    this.ringNumber = ringNumber;
  }

  public Integer getRingConnectionOrder() {
    return ringConnectionOrder;
  }

  public void setRingConnectionOrder(Integer ringConnectionOrder) {
    this.ringConnectionOrder = ringConnectionOrder;
  }

  public String getEsriJson() {
    return esriJson;
  }

  public void setEsriJson(String lineEsriJson) {
    this.esriJson = lineEsriJson;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public Integer getLegacyId() {
    return legacyId;
  }

  public void setLegacyId(Integer legacyId) {
    this.legacyId = legacyId;
  }
}

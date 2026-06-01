package uk.co.fivium.gisframework.migration.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@IdClass(OracleShapeLinkCompositeKey.class)
@Immutable
@Table(name = "MIGRATION_SHAPE_LINKS")
public class OracleShapeLink {

  @Id
  @Column(name = "CHILD_SHAPE_SI_ID")
  private Integer childShapeId;

  @Id
  @Column(name = "PARENT_SHAPE_SI_ID")
  private Integer parentShapeId;

  public Integer getChildShapeId() {
    return childShapeId;
  }

  void setChildShapeId(Integer childShapeId) {
    this.childShapeId = childShapeId;
  }

  public Integer getParentShapeId() {
    return parentShapeId;
  }

  void setParentShapeId(Integer parentShapeId) {
    this.parentShapeId = parentShapeId;
  }
}

package uk.co.fivium.gisframework.migration.oracle;

import java.io.Serial;
import java.io.Serializable;

public class OracleShapeLinkCompositeKey implements Serializable {

  @Serial
  private static final long serialVersionUID = 7238002810000000000L;

  private Integer childShapeId;
  private Integer parentShapeId;

  public OracleShapeLinkCompositeKey(Integer childShapeId, Integer parentShapeId) {
    this.childShapeId = childShapeId;
    this.parentShapeId = parentShapeId;
  }

  public OracleShapeLinkCompositeKey() {
  }

  public Integer getChildShapeId() {
    return childShapeId;
  }

  public void setChildShapeId(Integer childShapeId) {
    this.childShapeId = childShapeId;
  }

  public Integer getParentShapeId() {
    return parentShapeId;
  }

  public void setParentShapeId(Integer parentShapeId) {
    this.parentShapeId = parentShapeId;
  }
}

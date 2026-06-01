package uk.co.fivium.gisframework.migration.oracle;

import java.io.Serial;
import java.io.Serializable;

public class OracleAttributeCompositeKey implements Serializable {

  @Serial
  private static final long serialVersionUID = 7238002817265532511L;

  private Integer associatedSiId;
  private String attributeName;

  public OracleAttributeCompositeKey(Integer associatedSiId, String attributeName) {
    this.associatedSiId = associatedSiId;
    this.attributeName = attributeName;
  }

  public OracleAttributeCompositeKey() {
  }

  public Integer getAssociatedSiId() {
    return associatedSiId;
  }

  public void setAssociatedSiId(Integer associatedSiId) {
    this.associatedSiId = associatedSiId;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }

}

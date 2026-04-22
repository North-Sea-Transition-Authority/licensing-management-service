package uk.co.fivium.gisframework.migration.oracle;

import java.io.Serial;
import java.io.Serializable;

public class OracleShapeCompositeKey implements Serializable {

  @Serial
  private static final long serialVersionUID = 7238002817265532511L;

  private Integer shapeSidId;
  private String testCase;

  public OracleShapeCompositeKey(Integer shapeSidId, String testCase) {
    this.shapeSidId = shapeSidId;
    this.testCase = testCase;
  }

  public OracleShapeCompositeKey() {
  }

  public Integer getShapeSidId() {
    return shapeSidId;
  }

  public void setShapeSidId(Integer shapeSidId) {
    this.shapeSidId = shapeSidId;
  }

  public String getTestCase() {
    return testCase;
  }

  public void setTestCase(String testCase) {
    this.testCase = testCase;
  }
}

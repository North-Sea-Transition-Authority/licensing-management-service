package uk.co.fivium.gisframework.migration.oracle;

public class OracleAttributeTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private AttributeLevel attributeLevel = AttributeLevel.SHAPE;
    private Integer associatedSiId = 1;
    private String attributeName = "FULL_BLOCK_REF";
    private String attributeValue = "Test value";

    public Builder withAttributeLevel(AttributeLevel attributeLevel) {
      this.attributeLevel = attributeLevel;
      return this;
    }

    public Builder withAssociatedSiId(Integer associatedSiId) {
      this.associatedSiId = associatedSiId;
      return this;
    }

    public Builder withAttributeName(String attributeName) {
      this.attributeName = attributeName;
      return this;
    }

    public Builder withAttributeValue(String attributeValue) {
      this.attributeValue = attributeValue;
      return this;
    }

    public OracleAttribute build() {
      var attribute = new OracleAttribute();
      attribute.setAttributeLevel(attributeLevel);
      attribute.setAssociatedSiId(associatedSiId);
      attribute.setAttributeName(attributeName);
      attribute.setAttributeValue(attributeValue);
      return attribute;
    }
  }
}

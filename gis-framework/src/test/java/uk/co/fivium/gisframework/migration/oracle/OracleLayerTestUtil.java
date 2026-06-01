package uk.co.fivium.gisframework.migration.oracle;

public class OracleLayerTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer id = 1;
    private Layer layer = Layer.SUBAREAS;
    private String scope = "Test scope";

    public Builder withId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder withLayer(Layer layer) {
      this.layer = layer;
      return this;
    }

    public Builder withScope(String scope) {
      this.scope = scope;
      return this;
    }

    public OracleLayer build() {
      var oracleLayer = new OracleLayer();
      oracleLayer.setId(id);
      oracleLayer.setLayer(layer);
      oracleLayer.setScope(scope);
      return oracleLayer;
    }
  }
}

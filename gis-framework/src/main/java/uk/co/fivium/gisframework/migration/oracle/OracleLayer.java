package uk.co.fivium.gisframework.migration.oracle;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;
import uk.co.fivium.gisframework.feature.Layer;

@Profile("gis-migration")
@Entity
@Immutable
@Table(name = "MIGRATION_LAYERS")
public class OracleLayer {

  @Id
  @Column(name = "LAYER_ID")
  private Integer id;

  @Column(name = "LAYER_NAME")
  @Enumerated(EnumType.STRING)
  private Layer layer;

  @Column(name = "LAYER_SCOPE")
  private String scope;

  public Integer getId() {
    return id;
  }

  void setId(Integer id) {
    this.id = id;
  }

  public Layer getLayer() {
    return layer;
  }

  public void setLayer(Layer layer) {
    this.layer = layer;
  }

  public String getScope() {
    return scope;
  }

  void setScope(String scope) {
    this.scope = scope;
  }
}

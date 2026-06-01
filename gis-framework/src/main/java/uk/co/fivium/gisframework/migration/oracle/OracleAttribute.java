package uk.co.fivium.gisframework.migration.oracle;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Profile("gis-migration")
@Entity
@IdClass(OracleAttributeCompositeKey.class)
@Immutable
@Table(name = "MIGRATION_ATTRIBUTES")
public class OracleAttribute {

  @Id
  @Column(name = "ASSOCIATED_SID_ID")
  private Integer associatedSiId;

  @Column(name = "ATTRIBUTE_LEVEL")
  @Enumerated(EnumType.STRING)
  private AttributeLevel attributeLevel;

  @Id
  @Column(name = "ATTRIBUTE_NAME")
  private String attributeName;

  @Column(name = "ATTRIBUTE_VALUE")
  private String attributeValue;

  public AttributeLevel getAttributeLevel() {
    return attributeLevel;
  }

  void setAttributeLevel(AttributeLevel attributeLevel) {
    this.attributeLevel = attributeLevel;
  }

  public Integer getAssociatedSiId() {
    return associatedSiId;
  }

  void setAssociatedSiId(Integer associatedSiId) {
    this.associatedSiId = associatedSiId;
  }

  public String getAttributeName() {
    return attributeName;
  }

  void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }

  public String getAttributeValue() {
    return attributeValue;
  }

  void setAttributeValue(String attributeValue) {
    this.attributeValue = attributeValue;
  }
}

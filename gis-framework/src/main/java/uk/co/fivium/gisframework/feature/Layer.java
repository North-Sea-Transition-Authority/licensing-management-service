package uk.co.fivium.gisframework.feature;

import java.util.List;

public enum Layer {
  SUBAREAS("Subarea"),
  OFFSHORE_CROP_REF_BLOCKS("Offshore crop reference block"),
  ONSHORE_CROP_REF_BLOCKS("Onshore crop reference block"),
  OFFSHORE_REF_BLOCKS("Offshore reference block"),
  RETENTION_AREAS("Retention area"),
  BLOCKS("Block");

  private final String displayName;

  Layer(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static final List<Layer> REFERENCE_BLOCK_LAYERS = List.of(
      OFFSHORE_CROP_REF_BLOCKS,
      ONSHORE_CROP_REF_BLOCKS,
      OFFSHORE_REF_BLOCKS
  );
}

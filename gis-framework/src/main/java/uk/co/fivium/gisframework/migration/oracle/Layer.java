package uk.co.fivium.gisframework.migration.oracle;

import java.util.List;

public enum Layer {
  SUBAREAS,
  OFFSHORE_CROP_REF_BLOCKS,
  ONSHORE_CROP_REF_BLOCKS,
  OFFSHORE_REF_BLOCKS,
  RETENTION_AREAS,
  BLOCKS;

  public static final List<Layer> REFERENCE_BLOCK_LAYERS = List.of(
      OFFSHORE_CROP_REF_BLOCKS,
      ONSHORE_CROP_REF_BLOCKS,
      OFFSHORE_REF_BLOCKS
  );
}

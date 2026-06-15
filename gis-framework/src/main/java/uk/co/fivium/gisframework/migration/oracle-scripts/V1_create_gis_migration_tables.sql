-- lms gis data migration user
DROP USER lms_gis_migration CASCADE
/

CREATE USER lms_gis_migration
IDENTIFIED BY -- tpm password for mgr users on environment
DEFAULT TABLESPACE tbsdata
TEMPORARY TABLESPACE temp
ACCOUNT UNLOCK
QUOTA UNLIMITED ON tbsdata
QUOTA UNLIMITED ON tbsidx
/

-- store cached shape areas on tear down to reduce time on re-migration
CREATE TABLE lms_gis_migration.shape_area_cache (
  shape_si_id INTEGER
, shape_area_m2 NUMBER
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.shape_area_cache ADD
  CONSTRAINT shape_area_cache_pk
  PRIMARY KEY (shape_si_id)
  USING INDEX TABLESPACE tbsidx
/

-- cache of shapes that have been scribed to reduce time on re-migration
CREATE TABLE lms_gis_migration.scribed_shapes (
  shape_si_id INTEGER
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.scribed_shapes ADD
  CONSTRAINT scribed_shapes_pk
  PRIMARY KEY (shape_si_id)
  USING INDEX TABLESPACE tbsidx
/

-- support centralised tracking of migration outcomes and reporting errors
CREATE TABLE lms_gis_migration.migration_tracker (
  migration_shape_si_id INTEGER
, migration_shape_name VARCHAR2(4000) NOT NULL
, migration_shape_start_date DATE
, migration_shape_end_date DATE
, migration_layer_id INTEGER NOT NULL
, migration_order INTEGER NOT NULL
, migration_start_datetime TIMESTAMP
, migration_end_datetime TIMESTAMP
, migrated_flag CHAR(1) DEFAULT 'N' NOT NULL
, error_message VARCHAR2(4000)
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_tracker ADD
  CONSTRAINT migration_tracker_pk
  PRIMARY KEY (migration_shape_si_id)
  USING INDEX TABLESPACE tbsidx
/

-- layers we are migrating
CREATE TABLE lms_gis_migration.migration_layers (
  layer_id INTEGER
, layer_name VARCHAR2(4000) NOT NULL
, layer_scope VARCHAR2(4000)
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_layers ADD
  CONSTRAINT migration_layers_pk
  PRIMARY KEY (layer_id)
  USING INDEX TABLESPACE tbsidx
/

-- child parent links required for cascading dense points
CREATE TABLE lms_gis_migration.migration_shape_links (
  child_shape_si_id INTEGER NOT NULL
, parent_shape_si_id INTEGER NOT NULL
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_shape_links ADD
  CONSTRAINT migration_shape_links_pk
  PRIMARY KEY (child_shape_si_id, parent_shape_si_id)
  USING INDEX TABLESPACE tbsidx
/

ALTER TABLE lms_gis_migration.migration_shape_links ADD
  CONSTRAINT migration_shape_links_fk1
  FOREIGN KEY (child_shape_si_id)
  REFERENCES lms_gis_migration.migration_tracker (migration_shape_si_id)
/

ALTER TABLE lms_gis_migration.migration_shape_links ADD
  CONSTRAINT migration_shape_links_fk2
  FOREIGN KEY (parent_shape_si_id)
  REFERENCES lms_gis_migration.migration_tracker (migration_shape_si_id)
/

-- shapes within target layer
CREATE TABLE lms_gis_migration.migration_shapes (
  shape_sid_id INTEGER
, shape_si_id INTEGER -- links to pears model
, layer_id INTEGER NOT NULL
, shape_name VARCHAR2(4000) NOT NULL
, shape_srs VARCHAR2(60) NOT NULL
, shape_area_m2 NUMBER NOT NULL
, shape_start_date DATE
, shape_end_date DATE
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_shapes ADD
  CONSTRAINT migration_shapes_pk
  PRIMARY KEY (shape_si_id)
  USING INDEX TABLESPACE tbsidx
/

ALTER TABLE lms_gis_migration.migration_shapes ADD
  CONSTRAINT migration_shapes_fk1
  FOREIGN KEY (layer_id)
  REFERENCES lms_gis_migration.migration_layers (layer_id)
/

-- polygons that make up shape, may be single polygon or disjoint polygons in 2D or 3D
CREATE TABLE lms_gis_migration.migration_shape_polygons (
  polygon_sid_id INTEGER
, shape_sid_id INTEGER
, shape_si_id INTEGER
, feature_offset_low_m INTEGER
, feature_offset_high_m INTEGER
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_shape_polygons ADD
  CONSTRAINT migration_shape_polygons_pk
  PRIMARY KEY (polygon_sid_id)
  USING INDEX TABLESPACE tbsidx
/

ALTER TABLE lms_gis_migration.migration_shape_polygons ADD
  CONSTRAINT migration_shape_polygons_fk1
  FOREIGN KEY (shape_si_id)
  REFERENCES lms_gis_migration.migration_shapes (shape_si_id)
/

-- polygons may have holes represented as inner boundaries
CREATE TABLE lms_gis_migration.migration_polygon_boundaries (
  boundary_sid_id INTEGER
, polygon_sid_id INTEGER
, shape_si_id INTEGER
, boundary_type VARCHAR2(4000)
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_polygon_boundaries ADD
  CONSTRAINT migration_polygon_boundaries_pk
  PRIMARY KEY (boundary_sid_id)
  USING INDEX TABLESPACE tbsidx
/

ALTER TABLE lms_gis_migration.migration_polygon_boundaries ADD
  CONSTRAINT migration_polygon_boundaries_fk1
  FOREIGN KEY (polygon_sid_id)
  REFERENCES lms_gis_migration.migration_shape_polygons (polygon_sid_id)
/

-- the lines that make up each polygon boundary
CREATE TABLE lms_gis_migration.migration_boundary_lines (
  line_sid_id INTEGER
, boundary_sid_id INTEGER
, shape_si_id INTEGER
, connection_order INTEGER NOT NULL
, line_navigation_type VARCHAR2(4000) NOT NULL
, line_geojson CLOB NOT NULL
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_boundary_lines ADD
  CONSTRAINT migration_boundary_lines_pk
  PRIMARY KEY (line_sid_id)
  USING INDEX TABLESPACE tbsidx
/

ALTER TABLE lms_gis_migration.migration_boundary_lines ADD
  CONSTRAINT migration_boundary_lines_fk1
  FOREIGN KEY (boundary_sid_id)
  REFERENCES lms_gis_migration.migration_polygon_boundaries (boundary_sid_id)
/

CREATE TABLE lms_gis_migration.migration_attributes (
  attribute_level VARCHAR2(4000) NOT NULL
, associated_sid_id INTEGER NOT NULL
, attribute_name VARCHAR2(4000) NOT NULL
, attribute_value VARCHAR2(4000)
) TABLESPACE tbsdata
/

ALTER TABLE lms_gis_migration.migration_attributes ADD
  CONSTRAINT migration_attributes_pk
  PRIMARY KEY (associated_sid_id, attribute_name)
  USING INDEX TABLESPACE tbsidx
/

GRANT CREATE SESSION TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_data_points TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_current_data_points TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_licence_blocks TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_subareas TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_licence_block_refs TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_transactions TO lms_gis_migration
/

GRANT SELECT ON pedmgr.xview_ped_transactions TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_operations TO lms_gis_migration
/

GRANT SELECT ON pedmgr.xview_ped_operations TO lms_gis_migration
/

GRANT SELECT ON pedmgr.xview_ped_block_transfers TO lms_gis_migration
/

GRANT SELECT ON pedmgr.ped_retention_areas TO lms_gis_migration
/

GRANT SELECT ON spatialmgr.spatial_instance_periods TO lms_gis_migration
/

GRANT SELECT ON spatialmgr.spatial_layers TO lms_gis_migration
/

GRANT SELECT ON spatialmgr.spatial_instance_details TO lms_gis_migration
/

GRANT SELECT ON spatialmgr.spatial_attributes TO lms_gis_migration
/

GRANT EXECUTE ON spatialmgr.spm TO lms_gis_migration
/

GRANT EXECUTE ON spatialmgr.sp_command TO lms_gis_migration
/

GRANT EXECUTE ON spatialmgr.sp_datum TO lms_gis_migration
/

GRANT EXECUTE ON spatialmgr.sp_util TO lms_gis_migration
/

GRANT EXECUTE ON envmgr.st TO lms_gis_migration
/

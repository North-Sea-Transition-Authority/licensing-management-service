CREATE OR REPLACE PACKAGE lms_gis_migration.gis_migration
IS

  /*
  * Rebuilds a connection geometry with reversed ordinates
  */
  FUNCTION reverse_connection (
    p_connection_geom mdsys.sdo_geometry
  ) RETURN mdsys.sdo_geometry;    
  
  /*
  * Prepares Oracle GIS data for migration
  *
  * 1. Clear past migration run
  * 2. Stages data into the tracker.
  * 3. Establishes parent/child links.
  * 4. Iteratively processes shapes based on defined layer priorities
  *
  * Successful shape migrations are committed individually. 
  * Shape errors are logged in lms_gis_migration.migration_tracker with the migration for the specific shape being rolled back.
  */
  PROCEDURE migrate_lms_gis_data;
  
END gis_migration;
/

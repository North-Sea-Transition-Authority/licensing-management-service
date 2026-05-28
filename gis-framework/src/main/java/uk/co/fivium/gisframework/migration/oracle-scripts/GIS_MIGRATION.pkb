CREATE OR REPLACE PACKAGE BODY lms_gis_migration.gis_migration
IS

  K_LIVE_SIMULATION_ID CONSTANT INTEGER := 0;
  
  K_BLOCK_CREATE_MIGRATION_ORDER CONSTANT INTEGER := 10;
  K_REDEFINITION_POINT_MIGRATION_ORDER CONSTANT INTEGER := 15;
  K_BLOCK_CHANGE_MIGRATION_ORDER CONSTANT INTEGER := 20;
  K_SUBAREA_MIGRATION_ORDER CONSTANT INTEGER := 30;
  K_RETENTION_AREA_MIGRATION_ORDER CONSTANT INTEGER := 40;
  K_CROP_REF_BLOCKS_MIGRATION_ORDER CONSTANT INTEGER := 50;
  
  -- insert spatial migration data
  PROCEDURE insert_boundary_lines (
    p_boundary_sid_id INTEGER
  )
  IS
  BEGIN
  
    INSERT INTO lms_gis_migration.migration_boundary_lines (
      line_sid_id
    , boundary_sid_id
    , shape_si_id
    , connection_order
    , line_navigation_type
    , line_geojson
    )
    WITH connections AS (
      SELECT
        sid.id line_sid_id
      , (
          SELECT MIN(sid_c.id)
          FROM spatialmgr.spatial_instance_details sid_c
          WHERE sid_c.ancestor_b_sid_id = sid.ancestor_b_sid_id
          AND sid_c.class = 'C'
        ) min_line_sid_id
      , (
          SELECT sid_n.original_coord_str
          FROM spatialmgr.spatial_instance_details sid_n
          WHERE sid_n.ancestor_c_sid_id = sid.id
          AND sid_n.class = 'N'
          AND sid_n.anticlockwise_seq = 1
        ) line_start_node
      , (
          SELECT sid_n.original_coord_str
          FROM spatialmgr.spatial_instance_details sid_n
          WHERE sid_n.ancestor_c_sid_id = sid.id
          AND sid_n.class = 'N'
          AND sid_n.anticlockwise_seq = (
            SELECT MAX(sid_n2.anticlockwise_seq)
            FROM spatialmgr.spatial_instance_details sid_n2
            WHERE sid_n2.ancestor_c_sid_id = sid.id
            AND sid_n2.class = 'N'
          )
        ) line_end_node
      , sid.navigation_type
      , sid.ancestor_s_sid_id
      FROM spatialmgr.spatial_instance_details sid
      WHERE sid.ancestor_b_sid_id = p_boundary_sid_id
      AND sid.class = 'C'
    )
    SELECT
      c.line_sid_id
    , p_boundary_sid_id
    , sid_s.si_id
    , level connection_order
    , spatialmgr.sp_util.generalise_nav_type(c.navigation_type) line_navigation_type
    , sdo_util.to_geojson(
        spatialmgr.sp_command.get_connection_geometry(
          p_connection_sid_id => c.line_sid_id
        , p_transformation_target_srid => spatialmgr.sp_datum.lookup_oracle_sdo_srid(sid_s.class_srs)
        , p_densify_loxodromes => 0
        , p_grid_densify_tolerance => 0.05
        )
      ) line_geojson
    FROM connections c
    JOIN spatialmgr.spatial_instance_details sid_s ON sid_s.id = c.ancestor_s_sid_id
    START WITH c.line_sid_id = c.min_line_sid_id
    CONNECT BY NOCYCLE PRIOR c.line_end_node = c.line_start_node
    ORDER BY level;
    
    -- add boundary line attributes
    INSERT INTO lms_gis_migration.migration_attributes (
      attribute_level
    , associated_sid_id
    , attribute_name
    , attribute_value
    )
    SELECT
      'BOUNDARY_LINE'
    , sid.id
    , sa.name
    , sa.value
    FROM spatialmgr.spatial_instance_details sid
    JOIN spatialmgr.spatial_attributes sa ON sa.sid_id = sid.id AND sa.status_control = 'C'
    WHERE sid.ancestor_b_sid_id = p_boundary_sid_id
    AND sid.class = 'C';
  
  END insert_boundary_lines;
  
  PROCEDURE insert_feature_boundary (
    p_boundary_sid_id INTEGER
  )
  IS
  BEGIN
  
    INSERT INTO lms_gis_migration.migration_polygon_boundaries (
      boundary_sid_id
    , polygon_sid_id
    , shape_si_id
    , boundary_type
    )
    SELECT
      p_boundary_sid_id
    , sid.ancestor_f_sid_id
    , sid.si_id
    , sid.boundary_type
    FROM spatialmgr.spatial_instance_details sid
    WHERE sid.id = p_boundary_sid_id;
    
    -- add boundary attributes
    INSERT INTO lms_gis_migration.migration_attributes (
      attribute_level
    , associated_sid_id
    , attribute_name
    , attribute_value
    )
    SELECT
      'POLYGON_BOUNDARY'
    , p_boundary_sid_id
    , sa.name
    , sa.value
    FROM spatialmgr.spatial_attributes sa
    WHERE sa.sid_id = p_boundary_sid_id
    AND sa.status_control = 'C';
  
  END insert_feature_boundary;
  
  PROCEDURE insert_shape_feature (
    p_feature_sid_id INTEGER
  )
  IS
  BEGIN
  
    INSERT INTO lms_gis_migration.migration_shape_polygons (
      polygon_sid_id
    , shape_sid_id
    , shape_si_id
    , feature_offset_low_m
    , feature_offset_high_m
    )
    SELECT
      p_feature_sid_id
    , sid.ancestor_s_sid_id
    , sid.si_id
    , COALESCE(sid.feature_offset_low_m, sid.feature_offset_min_m)
    , COALESCE(sid.feature_offset_high_m, sid.feature_offset_max_m)
    FROM spatialmgr.spatial_instance_details sid
    WHERE sid.id = p_feature_sid_id;
    
    -- add feature attributes
    INSERT INTO lms_gis_migration.migration_attributes (
      attribute_level
    , associated_sid_id
    , attribute_name
    , attribute_value
    )
    SELECT
      'SHAPE_POLYGON'
    , p_feature_sid_id
    , sa.name
    , sa.value
    FROM spatialmgr.spatial_attributes sa
    WHERE sa.sid_id = p_feature_sid_id
    AND sa.status_control = 'C';
  
  END insert_shape_feature;

  PROCEDURE insert_shape (
    p_migration_shape_si_id INTEGER
  )
  IS
  
    l_missing_parent_shapes INTEGER;
    
  BEGIN
  
    -- check that parent shapes have been migrated
    SELECT COUNT(*)
    INTO l_missing_parent_shapes
    FROM lms_gis_migration.migration_shape_links msl
    LEFT JOIN lms_gis_migration.migration_shapes ms ON ms.shape_si_id = msl.parent_shape_si_id
    WHERE msl.child_shape_si_id = p_migration_shape_si_id
    AND ms.shape_si_id IS NULL;
    
    IF l_missing_parent_shapes > 0 THEN
      RAISE_APPLICATION_ERROR(-20000, 'Shape ' || p_migration_shape_si_id || ' migration failed as ' || l_missing_parent_shapes || ' parent shapes have not been migrated.');
    END IF;
    
    -- insert shape level migration data
    INSERT INTO lms_gis_migration.migration_shapes (
      shape_sid_id
    , shape_si_id
    , layer_id
    , shape_name
    , shape_srs
    , shape_area_m2
    , shape_start_date
    , shape_end_date
    )
    SELECT
      sid.id
    , mt.migration_shape_si_id
    , mt.migration_layer_id
    , mt.migration_shape_name
    , sid.class_srs
    -- use cached area if exists for shape id
    -- means shape won't have changed and can skip slow recalculation
    , CASE
        WHEN sac.shape_area_m2 IS NOT NULL
          THEN sac.shape_area_m2
        ELSE spatialmgr.spm.sum_fragmented_area(sid.id)
       END area_m2
    , mt.migration_shape_start_date
    , mt.migration_shape_end_date
    FROM lms_gis_migration.migration_tracker mt
    JOIN spatialmgr.spatial_instance_periods sip ON sip.si_id = mt.migration_shape_si_id AND sip.status_control = 'C'
    JOIN spatialmgr.spatial_instance_details sid ON sid.siv_id = sip.siv_id AND sid.class = 'S'
    LEFT JOIN lms_gis_migration.shape_area_cache sac ON sac.shape_si_id = mt.migration_shape_si_id
    WHERE mt.migration_shape_si_id = p_migration_shape_si_id;
    
    -- add shape attributes
    INSERT INTO lms_gis_migration.migration_attributes (
      attribute_level
    , associated_sid_id
    , attribute_name
    , attribute_value
    )
    SELECT
      'SHAPE'
    , sid.id
    , sa.name
    , sa.value
    FROM spatialmgr.spatial_instance_details sid
    JOIN spatialmgr.spatial_attributes sa ON sa.sid_id = sid.id AND sa.status_control = 'C'
    WHERE sid.si_id = p_migration_shape_si_id
    AND sid.class = 'S';
  
  END insert_shape;

  PROCEDURE migrate_shape (
    p_migration_shape_si_id INTEGER
  )
  IS
  BEGIN
    
    insert_shape (
      p_migration_shape_si_id => p_migration_shape_si_id
    );
    
    -- insert feature level polygons
    FOR shape_feature_rec IN (
      SELECT sid.id feature_sid_id
      FROM spatialmgr.spatial_instance_details sid
      WHERE sid.si_id = p_migration_shape_si_id
      AND sid.class = 'F'
    )
    LOOP
    
      insert_shape_feature (
        p_feature_sid_id => shape_feature_rec.feature_sid_id
      );
      
      FOR feature_boundary_rec IN (
        SELECT sid.id boundary_sid_id
        FROM spatialmgr.spatial_instance_details sid
        WHERE sid.ancestor_f_sid_id = shape_feature_rec.feature_sid_id
        AND sid.class = 'B'
      )
      LOOP
      
        insert_feature_boundary (
          p_boundary_sid_id => feature_boundary_rec.boundary_sid_id
        );
        
        insert_boundary_lines (
          p_boundary_sid_id => feature_boundary_rec.boundary_sid_id
        );
      
      END LOOP;

    END LOOP;
  
  END migrate_shape;
  
  PROCEDURE create_migration_layers
  IS
  BEGIN
  
    INSERT INTO lms_gis_migration.migration_layers (
      layer_id
    , layer_name
    , layer_scope
    )
    SELECT DISTINCT
      sl.id
    , sl.layer_type
    , sl.primary_data_uref
    FROM lms_gis_migration.migration_tracker mt
    JOIN spatialmgr.spatial_layers sl ON sl.id = mt.migration_layer_id;
  
  END create_migration_layers;
  
  -- load target migration data  
  PROCEDURE load_blocks
  IS
  
    TYPE t_root_block_si_id_map IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    l_root_block_si_id_cache t_root_block_si_id_map;
    
    l_final_root_block_si_id INTEGER;
    
    -- Local function to handle the walking
    FUNCTION get_root_block_si_id (
      p_current_block_si_id NUMBER
    ) RETURN INTEGER
    IS
    
      l_prior_block_si_id INTEGER;
      l_root_block_si_id INTEGER;
      
      l_regulator_reference VARCHAR2(4000);
      l_is_root_block INTEGER;
      
    BEGIN
    
      -- if we have already found through root block in prior chain back then just return it
      IF l_root_block_si_id_cache.EXISTS(p_current_block_si_id) THEN
        RETURN l_root_block_si_id_cache(p_current_block_si_id);
      END IF;
    
      -- get the prior block si_id
      WITH block_change_tran AS (
        SELECT
          plbr.quadrant_no
        , plbr.block_no
        , plbr.suffix
        , pdp.ped_tran_id
        FROM pedmgr.ped_data_points pdp
        JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = pdp.id
        JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
        WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID
        AND plb.si_id = p_current_block_si_id
        ORDER BY
          pdp.position_datetime
        , pdp.position_sequence
        FETCH FIRST 1 ROWS ONLY
      )
      -- find the block reference it came from
      , before_block_reference AS (
        SELECT
          bct.ped_tran_id
        , xpo.execution_date
        , xpbt.before_quadrant_no
        , xpbt.before_block_no
        , xpbt.before_block_suffix
        FROM block_change_tran bct
        JOIN pedmgr.ped_transactions pt ON pt.id = bct.ped_tran_id
        JOIN pedmgr.ped_operations po ON po.ped_tran_id = pt.id
        JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_operation_id = po.id
        JOIN pedmgr.xview_ped_block_transfers xpbt
          ON xpbt.ped_operation_id = xpo.ped_operation_id
          AND bct.quadrant_no = xpbt.after_quadrant_no
          AND bct.block_no = xpbt.after_block_no
          AND (bct.suffix IS NULL AND xpbt.after_block_suffix IS NULL OR bct.suffix = xpbt.after_block_suffix)
        WHERE po.status IN (
          'LIVE'
        , 'LEGACY'
        , 'CORRECTED'
        )
      )
      -- get the si_id of the block with the prior reference that ended at this position
      SELECT
        plb.si_id
      , xpt.regulator_reference_full
      INTO
        l_prior_block_si_id
      , l_regulator_reference
      FROM before_block_reference bbr
      JOIN pedmgr.xview_ped_transactions xpt ON xpt.ped_tran_id = bbr.ped_tran_id
      JOIN pedmgr.ped_data_points pdp ON pdp.ped_tran_id = xpt.ped_tran_id
      JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = pdp.id AND plb.end_datetime = pdp.position_datetime
      JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
      WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID
      AND plbr.quadrant_no = bbr.before_quadrant_no
      AND plbr.block_no = bbr.before_block_no
      AND (plbr.suffix IS NULL AND bbr.before_block_suffix IS NULL OR plbr.suffix = bbr.before_block_suffix);
      
      -- licence boundaries were redefined via patched corrections in 2017
      -- they were cropped to the 2014 MLW and the UKCS treaty boundaries
      -- we don't want to override this, so treat these as the root point to cascade from
      IF LOWER(l_regulator_reference) LIKE 'oga boundary redefinition%' THEN
      
        l_root_block_si_id := p_current_block_si_id;
              
      ELSE

        -- chain back to next prior block
        l_root_block_si_id := get_root_block_si_id(l_prior_block_si_id);      
      
      END IF;
      
      -- once found store root in cache so can reuse
      l_root_block_si_id_cache(p_current_block_si_id) := l_root_block_si_id;
      
      RETURN l_root_block_si_id;
      
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
      
        -- if we don't find an earlier block version then this should always be a block create
        SELECT COUNT(*)
        INTO l_is_root_block
        FROM lms_gis_migration.migration_tracker mt
        WHERE mt.migration_order = K_BLOCK_CREATE_MIGRATION_ORDER
        AND mt.migration_shape_si_id = p_current_block_si_id;
        
        IF l_is_root_block = 0 THEN
          RAISE_APPLICATION_ERROR(-20000, 'Found root block that was not created by a block create operation, root_block_si_id=' || p_current_block_si_id);
        END IF;
      
        RETURN p_current_block_si_id; -- if no prior block then we found the root
        
    END get_root_block_si_id;
    
  BEGIN
  
    -- block creates to be migrated first as used as densified source for later blocks
    INSERT INTO lms_gis_migration.migration_tracker (
      migration_shape_si_id
    , migration_shape_name
    , migration_shape_start_date
    , migration_shape_end_date
    , migration_layer_id
    , migration_order
    ) 
    WITH root_blocks AS (
      SELECT plb.si_id
      FROM pedmgr.ped_data_points pdp
      JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = pdp.id AND plb.end_datetime IS NULL
      JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
      JOIN pedmgr.ped_transactions pt ON pt.id = pdp.ped_tran_id
      JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_tran_id = pt.id
      JOIN pedmgr.ped_operations po ON po.id = xpo.ped_operation_id
      CROSS JOIN XMLTABLE(
          '/OPERATION/BLOCK_ENTRY_LIST/BLOCK_ENTRY'
        PASSING po.xml_data
        COLUMNS
          quadrant_no VARCHAR2(4000) PATH 'PRIMARY_BLOCK/QUADRANT_NO/text()'
        , block_no VARCHAR2(4000) PATH 'PRIMARY_BLOCK/BLOCK_NO/text()'
        , block_suffix VARCHAR2(4000) PATH 'PRIMARY_BLOCK/BLOCK_SUFFIX/text()'
      ) bc
      WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID
      AND xpo.operation_type = 'PED_BLOCK_CREATE'
      AND bc.quadrant_no = plbr.quadrant_no
      AND bc.block_no = plbr.block_no
      AND (bc.block_suffix IS NULL AND plbr.suffix IS NULL OR bc.block_suffix = plbr.suffix)
      AND plb.si_id IS NOT NULL
      AND xpo.status IN ('LIVE','LEGACY','CORRECTED')
    )
    SELECT 
      plb.si_id
    , plbr.block_ref
    , plb.start_datetime
    , plb.end_datetime
    , sip.sl_id
    , K_BLOCK_CREATE_MIGRATION_ORDER
    FROM pedmgr.ped_current_data_points pdp
    JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = pdp.id
    JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
    JOIN spatialmgr.spatial_instance_periods sip ON sip.si_id = plb.si_id AND sip.status_control = 'C'
    JOIN root_blocks rb ON rb.si_id = sip.si_id
    WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID;
    
    -- add block changes as secondary migration
    INSERT INTO lms_gis_migration.migration_tracker (
      migration_shape_si_id
    , migration_shape_name
    , migration_shape_start_date
    , migration_shape_end_date
    , migration_layer_id
    , migration_order
    )
    SELECT 
      plb.si_id
    , plbr.block_ref
    , plb.start_datetime
    , plb.end_datetime
    , sip.sl_id
    , K_BLOCK_CHANGE_MIGRATION_ORDER
    FROM pedmgr.ped_current_data_points pdp
    JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = pdp.id
    JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
    JOIN spatialmgr.spatial_instance_periods sip ON sip.si_id = plb.si_id AND sip.status_control = 'C'
    WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID
    -- shouldn't need this since si_id would change on block change
    -- single invalid timeline on dev with this issue
    -- in theory could happen in corrections or applications as well, but if same si_id then no need to migrate the change block
    MINUS
    SELECT
      mt.migration_shape_si_id
    , mt.migration_shape_name
    , mt.migration_shape_start_date
    , mt.migration_shape_end_date
    , mt.migration_layer_id
    , K_BLOCK_CHANGE_MIGRATION_ORDER -- to ensure our minus works, even though we are querying out the block creates
    FROM lms_gis_migration.migration_tracker mt
    WHERE mt.migration_order = K_BLOCK_CREATE_MIGRATION_ORDER;
      
    -- get root parent for block changes which will be densification source
    FOR change_block_rec IN (
      SELECT mt.migration_shape_si_id
      FROM lms_gis_migration.migration_tracker mt
      WHERE mt.migration_order = K_BLOCK_CHANGE_MIGRATION_ORDER
    )
    LOOP
      
      l_final_root_block_si_id := get_root_block_si_id(change_block_rec.migration_shape_si_id);
      
      -- if we are processing an si_id created from a 2017 boundary redefinition then it will be the root
      -- no need to add parent link as it has no parent (parent is itself) 
      IF l_final_root_block_si_id != change_block_rec.migration_shape_si_id THEN
            
        INSERT INTO lms_gis_migration.migration_shape_links (
          child_shape_si_id
        , parent_shape_si_id
        )
        VALUES (
          change_block_rec.migration_shape_si_id
        , l_final_root_block_si_id
        );
        
      END IF;
      
    END LOOP;
    
    -- move block change si_ids that are parents to an earlier migration order
    -- this is to ensure they are migrated before their child si_ids
    UPDATE lms_gis_migration.migration_tracker mt
    SET mt.migration_order = K_REDEFINITION_POINT_MIGRATION_ORDER
    WHERE mt.migration_order = K_BLOCK_CHANGE_MIGRATION_ORDER
    AND EXISTS (
      SELECT 1
      FROM lms_gis_migration.migration_shape_links msl
      WHERE msl.parent_shape_si_id = mt.migration_shape_si_id
    );
    
  END load_blocks;
  
  PROCEDURE load_retention_areas
  IS
  BEGIN
  
    INSERT INTO lms_gis_migration.migration_tracker (
        migration_shape_si_id
      , migration_shape_name
      , migration_shape_start_date
      , migration_shape_end_date
      , migration_layer_id
      , migration_order
      )
      SELECT DISTINCT
        pra.si_id
      , pra.short_name map_display_name
      , pra.start_datetime
      , pra.end_datetime
      , sip.sl_id
      , K_RETENTION_AREA_MIGRATION_ORDER migration_order
      FROM pedmgr.ped_current_data_points pdp
      JOIN pedmgr.ped_retention_areas pra ON pra.ped_dp_id = pdp.id
      JOIN spatialmgr.spatial_instance_periods sip ON sip.si_id = pra.si_id AND sip.status_control = 'C'
      WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID;
      
      INSERT INTO lms_gis_migration.migration_shape_links (
        child_shape_si_id
      , parent_shape_si_id      
      )
      SELECT DISTINCT
        mt.migration_shape_si_id
      , plb.si_id
      FROM migration_tracker mt
      JOIN spatialmgr.spatial_instance_periods sip ON sip.si_id = mt.migration_shape_si_id AND sip.status_control = 'C'
      JOIN spatialmgr.spatial_instance_details sid_f ON sid_f.siv_id = sip.siv_id
      JOIN spatialmgr.spatial_attributes sa_quad ON sid_f.id = sa_quad.sid_id AND sa_quad.status_control = 'C'
      JOIN spatialmgr.spatial_attributes sa_block ON sid_f.id = sa_block.sid_id AND sa_block.status_control = 'C'
      JOIN spatialmgr.spatial_attributes sa_suffix ON sid_f.id = sa_suffix.sid_id AND sa_suffix.status_control = 'C'
      JOIN pedmgr.ped_retention_areas pra ON pra.si_id = mt.migration_shape_si_id
      JOIN pedmgr.ped_data_points pdp ON pdp.id = pra.ped_dp_id AND pdp.position_datetime = mt.migration_shape_start_date
      JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = pdp.id AND plb.end_datetime IS NULL
      JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
      WHERE sid_f.class = 'F'
      AND sa_quad.name = 'QUADRANT_NO'
      AND sa_block.name = 'BLOCK_NO'
      AND sa_suffix.name = 'BLOCK_SUFFIX'
      AND plbr.quadrant_no = sa_quad.value
      AND plbr.block_no = sa_block.value
      AND (plbr.suffix IS NULL AND sa_suffix.value IS NULL OR plbr.suffix = sa_suffix.value);
      
  END load_retention_areas;
  
  PROCEDURE load_subareas
  IS
  BEGIN
  
    INSERT ALL
      INTO lms_gis_migration.migration_tracker (
        migration_shape_si_id
      , migration_shape_name
      , migration_shape_start_date
      , migration_shape_end_date
      , migration_layer_id
      , migration_order
      )
      VALUES (
        si_id
      , map_display_name
      , start_datetime
      , end_datetime
      , sl_id
      , migration_order
      )
      INTO lms_gis_migration.migration_shape_links (
        child_shape_si_id
      , parent_shape_si_id      
      )
      VALUES (
        si_id
      , COALESCE(parent_si_id_latest, parent_si_id_earliest)
      )
      -- a block could be partially surrendered without impacting the subarea shape
      -- this would result in the subarea shape being associated with multiple block shapes
      -- we only want to cascade subarea geodesic from a single block, take the latest version      
      SELECT DISTINCT
        ps.si_id
      , ps.short_name map_display_name
      , ps.start_datetime
      -- shape may get a new version with a new end date, but not start date, as a result of a block change
      -- shape itself has not changed since fits inside new block, so end date should match the latest version
      , FIRST_VALUE(ps.end_datetime) OVER (PARTITION BY ps.si_id ORDER BY ps.end_datetime DESC NULLS FIRST) end_datetime
      , sip.sl_id
      , K_SUBAREA_MIGRATION_ORDER migration_order
      -- single subarea shape may remain unchanged over multiple blocks changes
      -- since shape always fit inside block take latest version as this is the most likely to share a node
      , FIRST_VALUE(plb.si_id) OVER (PARTITION BY ps.si_id ORDER BY plb.start_datetime DESC) parent_si_id_latest
      -- timeline errors could result in block shape being in error and not created, take earliest value instead
      , FIRST_VALUE(plb.si_id) OVER (PARTITION BY ps.si_id ORDER BY plb.start_datetime) parent_si_id_earliest
      FROM pedmgr.ped_current_data_points pdp
      JOIN pedmgr.ped_subareas ps ON ps.ped_dp_id = pdp.id
      JOIN pedmgr.ped_licence_blocks plb ON plb.id = ps.ped_lb_id
      JOIN spatialmgr.spatial_instance_periods sip ON sip.si_id = ps.si_id AND sip.status_control = 'C'
      WHERE pdp.ped_sim_id = K_LIVE_SIMULATION_ID;
      
  END load_subareas;
  
  PROCEDURE load_ref_blocks
  IS
  BEGIN
    
    INSERT INTO lms_gis_migration.migration_tracker (
      migration_shape_si_id
    , migration_shape_name
    , migration_layer_id
    , migration_order
    )
    SELECT
      sid.si_id
    , sa.value
    , sid.sl_id
    , K_CROP_REF_BLOCKS_MIGRATION_ORDER
    FROM spatialmgr.spatial_layers sl
    JOIN spatialmgr.spatial_instance_periods sip ON sip.sl_id = sl.id AND sip.status_control = 'C'
    JOIN spatialmgr.spatial_instance_details sid ON sid.siv_id = sip.siv_id AND sid.class = 'S'
    JOIN spatialmgr.spatial_attributes sa ON sa.sid_id = sid.id AND sa.name = 'FULL_BLOCK_REF' AND sa.status_control = 'C'
    WHERE sl.layer_type IN ('OFFSHORE_CROP_REF_BLOCKS','ONSHORE_CROP_REF_BLOCKS')
    AND sl.primary_data_uref IS NULL;
    
    -- insert parent child links, these will be based on active licence blocks only
    INSERT INTO lms_gis_migration.migration_shape_links (
      child_shape_si_id
    , parent_shape_si_id      
    )
    SELECT
      mt.migration_shape_si_id
    , plb.si_id
    FROM lms_gis_migration.migration_tracker mt
    JOIN spatialmgr.spatial_instance_details sid ON sid.si_id = mt.migration_shape_si_id AND sid.class = 'S'
    JOIN spatialmgr.spatial_attributes sa_quad ON sa_quad.sid_id = sid.id AND sa_quad.name = 'QUADRANT_NO' AND sa_quad.status_control = 'C'
    JOIN spatialmgr.spatial_attributes sa_block ON sa_block.sid_id = sid.id AND sa_block.name = 'BLOCK_NO' AND sa_block.status_control = 'C'
    JOIN pedmgr.ped_licence_block_refs plbr ON plbr.quadrant_no = sa_quad.value AND plbr.block_no = sa_block.value
    JOIN pedmgr.ped_licence_blocks plb ON plb.id = plbr.plb_id AND plb.end_datetime IS NULL
    JOIN pedmgr.ped_current_data_points pcdp ON pcdp.id = plb.ped_dp_id
    WHERE mt.migration_order = K_CROP_REF_BLOCKS_MIGRATION_ORDER
    AND pcdp.ped_sim_id = K_LIVE_SIMULATION_ID
    -- on dev there are some live sims that are not valid and are missing block si_ids
    -- on prod this never occurs, but filter to valid ones for other envs
    AND pcdp.status = 'PROCESSED';
  
  END load_ref_blocks;
  
  PROCEDURE load_migration_shapes
  IS 
  BEGIN
  
    load_blocks;
    load_subareas;
    load_ref_blocks;
    load_retention_areas;
    
  END load_migration_shapes;
  
  -- migration setup procedures
  PROCEDURE clear_migration_data
  IS
  BEGIN
  
    -- Delete in reverse order of FK dependencies
    DELETE FROM lms_gis_migration.migration_attributes;
    DELETE FROM lms_gis_migration.migration_boundary_lines;
    DELETE FROM lms_gis_migration.migration_polygon_boundaries;
    DELETE FROM lms_gis_migration.migration_shape_polygons;
    DELETE FROM lms_gis_migration.migration_shape_links;
    DELETE FROM lms_gis_migration.migration_shapes;
    DELETE FROM lms_gis_migration.migration_layers;
    
    -- Clear the tracking metadata
    DELETE FROM lms_gis_migration.migration_tracker;
  
  END clear_migration_data;
  
  PROCEDURE cache_shape_areas
  IS
  BEGIN
  
    -- if spatial instance id has not changed then the shape has not changed
    -- cache any new areas from past migrations to avoid having to recalculate
    INSERT INTO lms_gis_migration.shape_area_cache (
      shape_si_id
    , shape_area_m2
    )
    SELECT 
      ms.shape_si_id
    , ms.shape_area_m2
    FROM lms_gis_migration.migration_shapes ms
    MINUS
    SELECT 
      sac.shape_si_id
    , sac.shape_area_m2
    FROM lms_gis_migration.shape_area_cache sac;
    
  END cache_shape_areas;
  
  -- public migration procedures
  PROCEDURE migrate_lms_gis_data
  IS
  
    l_total_count INTEGER;
    l_current_count INTEGER := 0;
    l_error_count INTEGER;
    
  BEGIN
  
    dbms_application_info.set_client_info('Clearing previous migration');
    
    cache_shape_areas;
    clear_migration_data;
  
    dbms_application_info.set_client_info('Setting up target shape data');
    
    load_migration_shapes;
    create_migration_layers;
    
    SELECT COUNT(*)
    INTO l_total_count 
    FROM lms_gis_migration.migration_tracker mt;
    
    FOR migration_shape_rec IN (
      SELECT mt.migration_shape_si_id
      FROM lms_gis_migration.migration_tracker mt
      ORDER BY mt.migration_order
    ) LOOP
    
      l_current_count := l_current_count + 1;
      
      dbms_application_info.set_client_info(
        'Creating shape for migration ' || l_current_count || '/' || l_total_count 
        || ' (SI_ID: ' || migration_shape_rec.migration_shape_si_id || ')'
      );
    
      DECLARE
      
        l_error_message VARCHAR2(4000);
        
      BEGIN
  
        SAVEPOINT start_shape_migration;
            
        UPDATE lms_gis_migration.migration_tracker mt
        SET mt.migration_start_datetime = SYSTIMESTAMP
        WHERE mt.migration_shape_si_id = migration_shape_rec.migration_shape_si_id;
    
        migrate_shape (
          p_migration_shape_si_id => migration_shape_rec.migration_shape_si_id
        );
        
        UPDATE lms_gis_migration.migration_tracker mt
        SET mt.migrated_flag = 'Y'
        , mt.migration_end_datetime = SYSTIMESTAMP
        , mt.error_message = NULL 
        WHERE mt.migration_shape_si_id = migration_shape_rec.migration_shape_si_id;

        COMMIT;
        
      EXCEPTION
      
        WHEN OTHERS THEN
        
          ROLLBACK TO start_shape_migration;
          
          l_error_message := SUBSTR(SQLERRM || CHR(10) || DBMS_UTILITY.FORMAT_ERROR_STACK || CHR(10) || DBMS_UTILITY.FORMAT_ERROR_BACKTRACE, 1, 4000);
          
          UPDATE lms_gis_migration.migration_tracker mt 
          SET mt.error_message = l_error_message
          WHERE mt.migration_shape_si_id = migration_shape_rec.migration_shape_si_id;
              
          COMMIT;
          
      END;
      
    END LOOP;
    
    SELECT COUNT(*)
    INTO l_error_count
    FROM lms_gis_migration.migration_tracker mt
    WHERE mt.migrated_flag = 'N';
    
    dbms_application_info.set_client_info('Migration preparation complete: ' || l_error_count || ' out of ' || l_total_count || ' shapes failed.');
    
  EXCEPTION
    WHEN OTHERS THEN
      dbms_application_info.set_client_info('Migration failed.');
      ROLLBACK;
      RAISE;
  
  END migrate_lms_gis_data;

END gis_migration;
/

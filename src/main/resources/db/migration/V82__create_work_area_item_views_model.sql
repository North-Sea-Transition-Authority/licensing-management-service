CREATE TABLE work_area_item_views(
    item_id UUID,
    item_type TEXT,
    user_id BIGINT,
    PRIMARY KEY (item_type, item_id, user_id)
);

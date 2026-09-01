# Event tracker cache table structure

* Status: Proposed
* Date: 26/08/2026

## Problem

We have determined in the original event tracker SPIKE (ADR-0007) that we want to approach saving and retrieving
data for the cross licence event tracker to be handled by flat cache table.

In the ADR it was also decided that we will reference the original tables for each entity to avoid the need to worry
about keeping every single data point up to date.

We now need to determine the structure of the cache table and how we keep the references up to date when they are added/
removed or new schedule versions are created.

## Options

* Option 1 - Link table referencing the existing tables
* Option 2 - Mostly flat table storing the data required

### Option 1 - Link table referencing the existing tables

This option primarily references the original tables instead of storing the data itself.

* Pro - reduces duplication
* Pro - easy to keep up to date as we do not have to worry about every data point
* Con - makes displaying the data complicated as we have to pull all the data together on retrieval
* Con - makes filtering the data complicated as it is stored in separate tables
* Con - does not provide a significant improvement over retrieving the data from the existing tables

### Example SQL

```sql
CREATE TABLE licence_event_cache(
    id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    licence_schedule_detail_id UUID,
    original_event_id UUID,
    event_type TEXT,
    application_id UUID,
    application_type TEXT                     
);
```

### Option 2 - Mostly flat table storing the data required

This option stores most of the data required by the event tracker in a flat table, using minimal references where convenient.

* Pro - simplifies data retrieval as most of the data is in one place
* Pro - simplifies filtering as all the data we want to filter is in the same table
* Con - Duplicates a large amount of data
* Con - Makes keeping the cache table up-to-date more complicated

### Example SQL

```sql
CREATE TABLE licence_event_cache(
    id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    licence_reference TEXT,
    original_event_id UUID,
    event_type TEXT,
    current_term_phase TEXT,
    next_term_phase TEXT,
    activity_type TEXT,
    event_date DATE,
    quad_block TEXT,
    steward_wua_id BIGINT, 
    application_id UUID,
    application_type TEXT
);
```

## Outcome

We have chosen to go with Option 2. We value simplifying the retrieval and filtering over reducing duplication and are
willing to deal with populating the table being more complex as it only needs to be updated in a few places.

## Secondary considerations

### Synchronous vs asynchronous updates
We have decided to update the cache table synchronously when a change to a schedule is applied or an application is
submitted. This is to prevent the cache table from becoming out of sync with the schedule/application data.

### Storing and retrieving data
For storing data we are going to use standard JPA repositories

For retrieving data we have decided to use... (jooq?)
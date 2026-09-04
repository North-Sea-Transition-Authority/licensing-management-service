<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/splitByCoordinateEntryPage/splitByCoordinateEntryPage.ftl" as coordinateEntry>

<@defaultPage htmlTitle="Split by coordinate entry tester" pageSize=PageSize.FULL_COLUMN>
    <@coordinateEntry.splitByCoordinateEntryPage
        commandJourneyId=commandJourneyId
        srsWkid=srsWkid
        precision=precision
    />
</@defaultPage>

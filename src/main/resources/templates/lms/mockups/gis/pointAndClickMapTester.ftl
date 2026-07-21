<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/baseMap/baseMap.ftl" as gis>

<@defaultPage
htmlTitle="GIS framework point and click map tester"
pageSize=PageSize.FULL_COLUMN
>
    <div class="govuk-!-padding-bottom-6">
      <p class="govuk-body">ED50</p>
      <@gis.baseMap featureIds=featureIdsEd50 srsWkid="4230"/>
    </div>
    <div>
      <p class="govuk-body">BNG</p>
      <@gis.baseMap featureIds=featureIdsBng srsWkid="27700"/>
    </div>
</@defaultPage>
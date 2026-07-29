<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/mapWithTextualDescription/mapWithTextualDescription.ftl" as gis>

<@defaultPage
htmlTitle="Map with textual description tester"
pageSize=PageSize.FULL_COLUMN
>
    <div class="govuk-!-padding-bottom-6">
      <h2 class="govuk-heading-m">Horizontal (description to the right of the map)</h2>
      <@gis.mapWithTextualDescription featureIds=featureIds srsWkid=srsWkid layout="horizontal"/>
    </div>
    <div>
      <h2 class="govuk-heading-m">Vertical (description underneath the map)</h2>
      <@gis.mapWithTextualDescription featureIds=featureIds srsWkid=srsWkid layout="vertical"/>
    </div>
</@defaultPage>

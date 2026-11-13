<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import "../../../../../gis/components/mapWithTextualDescription/mapWithTextualDescription.ftl" as mapWithTextualDescription>
<#import '../../../../licence/position/_positionChanges.ftl' as positionChanges>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
  pageSize=PageSize.FULL_COLUMN
>
  <@fdsForm.htmlForm actionUrl=springUrl(startCorrectionUrl)>
    <@fdsSummaryList.summaryList>
      <@positionChanges.partialSurrenderBlockRows blockRows=blockRows/>
    </@fdsSummaryList.summaryList>

    <h2 class="govuk-heading-m">Area surrendered</h2>
    <div class="govuk-!-margin-bottom-5">
      <@mapWithTextualDescription.mapWithTextualDescription featureIds=surrenderedFeatureIds srsWkid=srsWkid />
    </div>

    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>

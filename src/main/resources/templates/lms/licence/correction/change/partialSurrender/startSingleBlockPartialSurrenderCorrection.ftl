<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import "../../../../../gis/components/mapWithTextualDescription/mapWithTextualDescription.ftl" as mapWithTextualDescription>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
  pageSize=PageSize.FULL_COLUMN
>
  <@fdsForm.htmlForm actionUrl=springUrl(startCorrectionUrl)>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Block to surrender">
        <#list blockRows as blockRow>
          ${blockRow.blockLabel()}<#if blockRow.surrenderType()??> - ${blockRow.surrenderType()}</#if>
        </#list>
      </@fdsSummaryList.summaryListRowNoAction>
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

<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import '../../../../summary/_summarySections.ftl' as sectionSummary>

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkEnabled=true
>
  <@fdsForm.htmlForm actionUrl=(submitUrl??)?then(springUrl(submitUrl), "")>
    <@sectionSummary.summarySections summarySections=summarySections accordionId="subarea-changes"/>

    <#if submitUrl??>
      <@fdsAction.submitButtons
        primaryButtonText="Submit"
        secondaryLinkText="Back"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    </#if>
  </@fdsForm.htmlForm>
</@defaultPage>

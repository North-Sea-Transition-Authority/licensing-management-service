<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import '../../../../summary/_summarySections.ftl' as sectionSummary>

<#assign isReadOnly = readOnly!false>

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkUrl=springUrl(backLinkUrl)
>
  <#if isReadOnly>
    <@sectionSummary.summarySections summarySections=summarySections accordionId=accordionId/>
  <#else>
    <@fdsForm.htmlForm>
      <@sectionSummary.summarySections summarySections=summarySections accordionId=accordionId/>

      <@fdsAction.submitButtons
        primaryButtonText="Submit"
        secondaryLinkText=backLinkText
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    </@fdsForm.htmlForm>
  </#if>
</@defaultPage>

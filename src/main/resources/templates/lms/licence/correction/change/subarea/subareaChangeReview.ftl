<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import '../../../../summary/_summarySections.ftl' as sectionSummary>

<#assign isReadOnly = readOnly!false>
<#assign summaryAccordionId = accordionId!"subarea-changes">

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkEnabled=true
>
  <#if isReadOnly>
    <@sectionSummary.summarySections summarySections=summarySections accordionId=summaryAccordionId/>
  <#else>
    <@fdsForm.htmlForm actionUrl=(submitUrl??)?then(springUrl(submitUrl), "")>
      <@sectionSummary.summarySections summarySections=summarySections accordionId=summaryAccordionId/>

      <#if submitUrl??>
        <@fdsAction.submitButtons
          primaryButtonText="Submit"
          secondaryLinkText="Back"
          linkSecondaryAction=true
          linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
      </#if>
    </@fdsForm.htmlForm>
  </#if>
</@defaultPage>

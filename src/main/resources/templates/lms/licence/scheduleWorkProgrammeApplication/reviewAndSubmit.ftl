<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryMissingInformationNotificationBanner.ftl' as banner>
<#import 'scheduleSummary.ftl' as scheduleSummary>

<#assign pageTitle = "Review your submission before submitting"/>



<@defaultPage
htmlTitle=pageTitle
caption=pageCaption
pageHeading=pageTitle
pageHeadingClass="govuk-heading-xl"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
backLinkUrl=springUrl(cancelUrl)>
    <#if !isSubmittable>
      <@banner.summaryMissingInformationNotificationBanner/>
    </#if>

    <@fdsForm.htmlForm>
        <@scheduleSummary.scheduleSummary accordionId=accordionId summarySections=summarySections/>

        <#if isSubmittable>
            <@fdsAction.submitButtons
            primaryButtonText="Submit"
            secondaryLinkText="Back to task list"
            linkSecondaryAction=true
            linkSecondaryActionUrl="${springUrl(cancelUrl)}"
            />
        <#else>
            <@fdsAction.link linkText="Back to task list" linkUrl="${springUrl(cancelUrl)}"/>
        </#if>
    </@fdsForm.htmlForm>

</@defaultPage>
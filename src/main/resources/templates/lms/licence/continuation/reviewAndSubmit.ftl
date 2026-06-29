<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryMissingInformationNotificationBanner.ftl' as banner>
<#import 'continuationApplicationSummary.ftl' as continuationApplicationSummary>

<#assign pageTitle = "Review your application before submitting"/>

<@defaultPage
htmlTitle=pageTitle
caption=pageCaption
pageHeading=pageTitle
pageHeadingClass="govuk-heading-xl"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
backLinkUrl=springUrl(cancelUrl)>
    <@fdsForm.htmlForm>
        <@continuationApplicationSummary.continuationApplicationSummary accordionId=accordionId summarySections=summarySections workProgrammeActivities=workProgrammeActivities![]/>
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
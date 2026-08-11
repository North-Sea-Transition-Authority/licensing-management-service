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
breadcrumbs=breadcrumbs>
    <#if !hasLicenceContact>
        <@fdsError.singleErrorSummary
            errorMessage="This application cannot be submitted as there is no contact for the licensee. Provide a contact for the submitting licensee in the Licence contacts list."/>
    </#if>

    <@fdsForm.htmlForm>
        <@continuationApplicationSummary.continuationApplicationSummary accordionId=accordionId summarySections=summarySections workProgrammeActivities=workProgrammeActivities![]/>
        <#if isSubmittable && hasLicenceContact>
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
<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryMissingInformationNotificationBanner.ftl' as banner>
<#import 'scheduleApplicationSummary.ftl' as scheduleApplicationSummary>

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

    <#if !isSubmittable>
      <@banner.summaryMissingInformationNotificationBanner/>
    <#elseif !userCanSubmit>
        <@fdsNotificationBanner.notificationBannerInfo fullWidth=true bannerTitleText="Permission required">
            <@fdsNotificationBanner.notificationBannerContent headingText="You do not have the required role to submit this application.">
                <p class="govuk-body">Only users assigned the ‘${submitterRoleName}’ role for this organisation can submit applications to the NSTA.</p>
            </@fdsNotificationBanner.notificationBannerContent>
        </@fdsNotificationBanner.notificationBannerInfo>
    </#if>

    <@fdsForm.htmlForm>
        <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>

        <#if isSubmittable && hasLicenceContact && userCanSubmit>
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
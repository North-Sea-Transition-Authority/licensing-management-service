<#include '../../fds/objects/layouts/generic.ftl'>

<#-- @ftlvariable name="serviceBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.ServiceConfigurationProperties" -->

<#macro contactDetails
includeHeading=true
emailSubject=""
errorReference=""
>
    <@fdsSummaryList.summaryListCard
    summaryListId="technical-support-contact-details"
    headingText=includeHeading?then("Techical support details", "")
    >
        <#if errorReference?has_content>
            <@fdsSummaryList.summaryListRowNoAction keyText="Error reference">
              <p class="govuk-body">${errorReference}</p>
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>

        <@fdsSummaryList.summaryListRowNoAction keyText="Email address">
            <#assign emailAddress = serviceBranding.supportContact().email() />
            <@fdsAction.link
            linkText=emailAddress
            linkUrl="mailto:${emailAddress + emailSubject?has_content?then('?subject=${serviceBranding.mnemonic()} - ' + emailSubject, '')}"
            />
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Phone number">
          <p class="govuk-body">${serviceBranding.supportContact().phone()}</p>
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
</#macro>

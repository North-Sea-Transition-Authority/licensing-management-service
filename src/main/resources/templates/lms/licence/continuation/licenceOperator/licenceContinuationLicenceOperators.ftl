<#include '../../../layout/layout.ftl'>
<#import 'licenceContinuationLicenceOperatorsCard.ftl' as licenceOperatorsCard>

<@defaultPage
htmlTitle=pageTitle
pageHeading="Licence operators"
caption=pageCaption
captionClass="govuk-caption-m"
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs
errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <#list subareas as subarea>
            <@licenceOperatorsCard.subareaCard subarea=subarea/>
        </#list>

        <#if hasMissingOperators?? && hasMissingOperators>
            <@fdsTextarea.textarea
            path="form.pendingActionsExplanation"
            labelText="You have blocks without an assigned operator. What actions are being taken to assign an operator to these blocks?" />
            <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
        <#else>
          <p class="govuk-body">All blocks currently have an assigned operator. No further action is required for this section.</p>
            <@fdsAction.link linkText="Back" linkUrl="${springUrl(cancelUrl)}"/>
        </#if>

    </@fdsForm.htmlForm>
</@defaultPage>
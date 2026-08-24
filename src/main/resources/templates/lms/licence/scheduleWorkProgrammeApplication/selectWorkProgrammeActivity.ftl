<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=workProgrammeActivityViews?has_content?then("", pageTitle)
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#if workProgrammeActivityViews?has_content>
            <@fdsRadio.radioGroup
                path="form.workProgrammeActivityId"
                labelText=pageTitle
                fieldsetHeadingSize="h1"
                fieldsetHeadingClass="govuk-fieldset__legend--l">
                <#assign isFirstActivity=true/>
                <#list workProgrammeActivityViews as view>
                    <@fdsRadio.radioItem
                        path="form.workProgrammeActivityId"
                        itemMap={view.id() : view.description()}
                        itemHintText=view.category()
                        isFirstItem=isFirstActivity/>
                    <#assign isFirstActivity=false/>
                </#list>
            </@fdsRadio.radioGroup>

            <@fdsAction.submitButtons
                primaryButtonText="Save and continue"
                secondaryLinkText="Cancel"
                linkSecondaryAction=true
                linkSecondaryActionUrl=springUrl(cancelUrl)/>
        <#else>
            <@fdsInsetText.insetText>
                <#if allActivitiesActioned>
                    A decision has been recorded against every work programme activity on this licence schedule.
                <#else>
                    There are no work programme activities on this licence schedule to record a decision against.
                </#if>
            </@fdsInsetText.insetText>

            <@fdsAction.link linkText="Return to task list" linkUrl=springUrl(cancelUrl)/>
        </#if>

    </@fdsForm.htmlForm>
</@defaultPage>

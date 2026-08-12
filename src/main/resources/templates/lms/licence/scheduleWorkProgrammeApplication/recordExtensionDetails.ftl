<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#assign hasTermOptions = false>
        <#list extensionDetailsViews as view>
            <#if !view.isPhase()>
                <#assign hasTermOptions = true>
            </#if>
        </#list>

        <#if canExtendMoreThanOneOption>
            <@fdsCheckbox.checkboxGroup
                path=hasTermOptions?then("form.selectedTerm", "form.selectedPhase")
                fieldsetHeadingText="Select the terms and phases being extended"
                fieldsetHeadingSize="h2"
                fieldsetHeadingClass="govuk-label govuk-label--l"
                hiddenContent=true>

                <#list extensionDetailsViews as view>
                    <#assign selectionPath = view.isPhase()?then("form.selectedPhase", "form.selectedTerm")>
                    <@fdsCheckbox.checkboxItem
                        path="${selectionPath}[${view.id()}]"
                        labelText="${view.displayName()}"
                        inputHintText="Due to end ${view.endDate()}">
                        <@duration.threeFieldDuration
                            dayPath="form.extensionDuration[${view.id()}].days"
                            monthPath="form.extensionDuration[${view.id()}].months"
                            yearPath="form.extensionDuration[${view.id()}].years"
                            nestingPath="${selectionPath}[${view.id()}]"
                            fieldNamePath="form.extensionDuration[${view.id()}].fieldName"
                            fieldDisplayTextPath="form.extensionDuration[${view.id()}].fieldDisplayText"
                            labelText="How long is this term/phase to be extended by?"
                            formId="${view.id()}"/>
                    </@fdsCheckbox.checkboxItem>
                </#list>
            </@fdsCheckbox.checkboxGroup>
        <#else>
            <#list extensionDetailsViews as view>
                <@duration.threeFieldDuration
                    dayPath="form.extensionDuration[${view.id()}].days"
                    monthPath="form.extensionDuration[${view.id()}].months"
                    yearPath="form.extensionDuration[${view.id()}].years"
                    fieldNamePath="form.extensionDuration[${view.id()}].fieldName"
                    fieldDisplayTextPath="form.extensionDuration[${view.id()}].fieldDisplayText"
                    labelText="How long is ${view.displayName()} to be extended by?"
                    formId="${view.id()}"/>
            </#list>
        </#if>

        <@fdsAction.submitButtons
            primaryButtonText="Save and continue"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>

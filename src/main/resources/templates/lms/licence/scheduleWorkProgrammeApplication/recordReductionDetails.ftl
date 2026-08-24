<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>

        <#assign hasTermOptions = false>
        <#list reductionDetailsViews as view>
            <#if !view.isPhase()>
                <#assign hasTermOptions = true>
            </#if>
        </#list>

        <#if canReduceMoreThanOneOption>
            <@fdsCheckbox.checkboxGroup
                path=hasTermOptions?then("form.selectedTerm", "form.selectedPhase")
                fieldsetHeadingText="What do you want to reduce?"
                fieldsetHeadingSize="h2"
                fieldsetHeadingClass="govuk-label govuk-label--l"
                hiddenContent=true>

                <#list reductionDetailsViews as view>
                    <#assign selectionPath = view.isPhase()?then("form.selectedPhase", "form.selectedTerm")>
                    <@fdsCheckbox.checkboxItem
                        path="${selectionPath}[${view.id()}]"
                        labelText="${view.displayName()}"
                        inputHintText="Due to end ${view.endDate()}">
                        <@duration.threeFieldDuration
                            dayPath="form.reductionDuration[${view.id()}].days"
                            monthPath="form.reductionDuration[${view.id()}].months"
                            yearPath="form.reductionDuration[${view.id()}].years"
                            nestingPath="${selectionPath}[${view.id()}]"
                            fieldNamePath="form.reductionDuration[${view.id()}].fieldName"
                            fieldDisplayTextPath="form.reductionDuration[${view.id()}].fieldDisplayText"
                            labelText="How long is this to be reduced by?"
                            formId="${view.id()}"/>
                    </@fdsCheckbox.checkboxItem>
                </#list>
            </@fdsCheckbox.checkboxGroup>
        <#else>
            <#list reductionDetailsViews as view>
                <@duration.threeFieldDuration
                    dayPath="form.reductionDuration[${view.id()}].days"
                    monthPath="form.reductionDuration[${view.id()}].months"
                    yearPath="form.reductionDuration[${view.id()}].years"
                    fieldNamePath="form.reductionDuration[${view.id()}].fieldName"
                    fieldDisplayTextPath="form.reductionDuration[${view.id()}].fieldDisplayText"
                    labelText="How long is ${view.displayName()} to be reduced by?"
                    formId="${view.id()}"/>
            </#list>
        </#if>

        <@fdsAction.submitButtons
            primaryButtonText="Save and complete"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>

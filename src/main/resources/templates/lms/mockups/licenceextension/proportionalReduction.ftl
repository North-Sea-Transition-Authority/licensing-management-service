<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <@fdsCheckbox.checkboxGroup
            path="form.selectedExtend['all']"
            fieldsetHeadingText="Select the terms and phases you want to extend"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-label govuk-label--l"
            hiddenContent=true>

            <#list terms as term>
                <#if term.canExtend()>
                <@fdsCheckbox.checkboxItem
                    path="form.selectedExtend['${term.id()}']"
                    labelText="${term.name()}">

                    <@fdsCheckbox.checkboxGroup
                        path="form.selectedReduce['${term.id()}']"
                        fieldsetHeadingText="What do you want to reduce?"
                        fieldsetHeadingSize="h2"
                        fieldsetHeadingClass="govuk-label govuk-label--m"
                        nestingPath="form.selectedExtend['${term.id()}']"
                        hiddenContent=true>

                        <#list terms as reduceTerm>
                            <#if reduceTerm?index gt term?index>
                                <@fdsCheckbox.checkboxItem
                                    path="form.selectedReduce['${term.id()}-${reduceTerm.id()}']"
                                    labelText="${reduceTerm.name()}"
                                    inputHintText="Due to end ${reduceTerm.endDate()}">
                                    <@duration.threeFieldDuration
                                        dayPath="form.reductionDuration['${term.id()}-${reduceTerm.id()}'].days"
                                        monthPath="form.reductionDuration['${term.id()}-${reduceTerm.id()}'].months"
                                        yearPath="form.reductionDuration['${term.id()}-${reduceTerm.id()}'].years"
                                        nestingPath="form.selectedReduce['${term.id()}-${reduceTerm.id()}']"
                                        fieldNamePath="form.reductionDuration['${term.id()}-${reduceTerm.id()}'].fieldName"
                                        fieldDisplayTextPath="form.reductionDuration['${term.id()}-${reduceTerm.id()}'].fieldDisplayText"
                                        labelText="How long is this to be reduced by?"
                                        formId="${term.id()}-${reduceTerm.id()}"/>
                                </@fdsCheckbox.checkboxItem>
                            </#if>
                        </#list>
                    </@fdsCheckbox.checkboxGroup>
                </@fdsCheckbox.checkboxItem>
                </#if>
            </#list>
        </@fdsCheckbox.checkboxGroup>

        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl("/mockups/decision-journey")/>

    </@fdsForm.htmlForm>
</@defaultPage>

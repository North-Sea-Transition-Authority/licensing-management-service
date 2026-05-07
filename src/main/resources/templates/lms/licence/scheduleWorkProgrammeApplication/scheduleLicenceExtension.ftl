<#include '../../layout/layout.ftl'>
<#import '../../component/duration/threeFieldDuration.ftl' as duration>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsSummaryList.summaryListCard headingText="Current Term Details" summaryListId="Current Term Details" >
            <@fdsSummaryList.summaryListRowNoAction keyText="Term">
                ${currentTerm.getTermType().getDisplayName()}
            </@fdsSummaryList.summaryListRowNoAction>

            <#if (currentPhase.phaseType)??>
                <@fdsSummaryList.summaryListRowNoAction keyText="Phase">
                    ${currentPhase.getPhaseType().getDisplayName()}
                </@fdsSummaryList.summaryListRowNoAction>
            </#if>
            <@fdsSummaryList.summaryListRowNoAction  keyText="Term end date">
                ${currentTermEndDate}
            </@fdsSummaryList.summaryListRowNoAction>
            <#if (currentPhaseEndDate)??>
                <@fdsSummaryList.summaryListRowNoAction keyText="Phase end date">
                    ${currentPhaseEndDate}
                </@fdsSummaryList.summaryListRowNoAction>
            </#if>
        </@fdsSummaryList.summaryListCard>

        <#if canExtendMoreThanOneOption>
            <@fdsCheckbox.checkboxGroup
            path="form.selectedTerm"
            fieldsetHeadingText="Select the terms and phases you want to extend"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-label govuk-label--l"
            hiddenContent=true>

                <#list validTermsAndPhases as term>
                    <#if term.termId()??>
                        <@fdsCheckbox.checkboxItem
                        path="form.selectedTerm[${term.termId()}]"
                        labelText="${term.termName()}">
                            <@duration.threeFieldDuration
                            dayPath="form.extensionDuration[${term.termId()}].days"
                            monthPath="form.extensionDuration[${term.termId()}].months"
                            yearPath="form.extensionDuration[${term.termId()}].years"
                            nestingPath="form.selectedTerm[${term.termId()}]"
                            fieldNamePath="form.extensionDuration[${term.termId()}].fieldName"
                            fieldDisplayTextPath="form.extensionDuration[${term.termId()}].fieldDisplayText"
                            labelText="How long are you requesting this term to be extended by?"
                            formId="term-${term.termId()}"/>
                        </@fdsCheckbox.checkboxItem>
                    </#if>

                    <#if term.phases()?has_content>
                        <#list term.phases() as phase>
                            <@fdsCheckbox.checkboxItem
                            path="form.selectedPhase[${phase.phaseId()}]"
                            labelText="${phase.phaseName()}">
                                <@duration.threeFieldDuration
                                dayPath="form.extensionDuration[${phase.phaseId()}].days"
                                monthPath="form.extensionDuration[${phase.phaseId()}].months"
                                yearPath="form.extensionDuration[${phase.phaseId()}].years"
                                nestingPath="form.selectedPhase[${phase.phaseId()}]"
                                fieldNamePath="form.extensionDuration[${phase.phaseId()}].fieldName"
                                fieldDisplayTextPath="form.extensionDuration[${phase.phaseId()}].fieldDisplayText"
                                labelText="How long are you requesting this phase to be extended by?"
                                formId="phase-${phase.phaseId()}"/>
                            </@fdsCheckbox.checkboxItem>
                        </#list>
                    </#if>
                </#list>
            </@fdsCheckbox.checkboxGroup>
        <#else>
            <#list validTermsAndPhases as term>

                <#if term.termId()??>
                    <@duration.threeFieldDuration
                    dayPath="form.extensionDuration[${term.termId()}].days"
                    monthPath="form.extensionDuration[${term.termId()}].months"
                    yearPath="form.extensionDuration[${term.termId()}].years"
                    fieldNamePath="form.extensionDuration[${term.termId()}].fieldName"
                    fieldDisplayTextPath="form.extensionDuration[${term.termId()}].fieldDisplayText"
                    labelText="How long are you requesting this term to be extended by?"
                    formId="term-${term.termId()}"/>
                </#if>

                <#if term.phases()?size == 1>
                    <#assign phase = term.phases()[0]>
                    <@duration.threeFieldDuration
                    dayPath="form.extensionDuration[${phase.phaseId()}].days"
                    monthPath="form.extensionDuration[${phase.phaseId()}].months"
                    yearPath="form.extensionDuration[${phase.phaseId()}].years"
                    fieldNamePath="form.extensionDuration[${phase.phaseId()}].fieldName"
                    fieldDisplayTextPath="form.extensionDuration[${phase.phaseId()}].fieldDisplayText"
                    labelText="How long are you requesting this phase to be extended by?"
                    formId="phase-${phase.phaseId()}"/>
                </#if>
            </#list>
        </#if>
        <@fdsAction.submitButtons primaryButtonText="Save and complete" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
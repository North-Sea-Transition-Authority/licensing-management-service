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

        <@duration.threeFieldDuration
        dayPath="form.extensionDuration.days"
        monthPath="form.extensionDuration.months"
        yearPath="form.extensionDuration.years"
        labelText="How long are you requesting to extend by?"
        formId="extension"
        />

        <@fdsTextarea.textarea
        path="form.explanation"
        labelText="Provide detailed reasons for the extension(s) requested"
        />

        <@fdsDetails.summaryDetails summaryTitle="What do I need to provide?">
          <p class="govuk-body">Provide detailed reason(s) for requiring an extension, including:</p>
          <ul class="govuk-list govuk-list--bullet">
            <li>details of any ongoing or proposed Licence assignments or changes of control</li>
            <li>explain why the obligations have been unable to be delivered under the original timelines.</li>
            <li>explain why additional time is required</li>
          </ul>
        </@fdsDetails.summaryDetails>

        <@fdsAction.submitButtons primaryButtonText="Save" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
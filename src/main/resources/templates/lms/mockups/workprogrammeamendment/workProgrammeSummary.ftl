<#include '../../layout/layout.ftl'>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>
    <@fdsForm.htmlForm>

        <#list decisions as decision>
            <@fdsSummaryList.summaryListCard summaryListId="wp-decision-${decision?index}" headingText=decision.description headingSize="h2">
                <@fdsSummaryList.summaryListRowNoAction keyText="Decision">${decision.action}</@fdsSummaryList.summaryListRowNoAction>
            </@fdsSummaryList.summaryListCard>
        </#list>

        <@fdsRadio.radioGroup
            path="form.addAnotherOption"
            labelText="Do you want to add another work programme to the scope of this decision?"
            fieldsetHeadingSize="h2"
            fieldsetHeadingClass="govuk-fieldset__legend--m">

            <@fdsRadio.radioItem
                path="form.addAnotherOption"
                itemMap={"yes": "Yes, I want to add it now"}
                isFirstItem=true/>

            <@fdsRadio.radioItem
                path="form.addAnotherOption"
                itemMap={"no": "No, I have added all work programmes I need to"}/>

        </@fdsRadio.radioGroup>

        <@fdsAction.button buttonText="Save and continue"/>

    </@fdsForm.htmlForm>
</@defaultPage>

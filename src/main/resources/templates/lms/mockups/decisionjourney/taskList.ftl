<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN>

    <@summaryDetails.summaryDetails summaryItem=summaryItem/>

    <@fdsTaskList.singleTaskList>
        <@fdsTaskList.taskListSection
        sectionNumber="1"
        sectionHeadingText="Record of decision">
            <@fdsTaskList.taskListItem
                itemText="What is the decision?"
                itemUrl=springUrl("/mockups/decision-journey/record-decision")
                useNotCompletedLabels=true />
            <@fdsTaskList.taskListItem
                itemText="Extension decision details"
                itemUrl=springUrl("/mockups/licence-extension/extension-only")
                useNotCompletedLabels=true />
            <@fdsTaskList.taskListItem
                itemText="Corresponding reduction details"
                itemUrl=springUrl("/mockups/licence-extension/reduction-only")
                useNotCompletedLabels=true />
            <@fdsTaskList.taskListItem
                itemText="Work programme amendment details"
                itemUrl=springUrl("/mockups/work-programme-amendment")
                useNotCompletedLabels=true />
        </@fdsTaskList.taskListSection>
        <@fdsTaskList.taskListSection
        sectionNumber="2"
        sectionHeadingText="Review">
            <@fdsTaskList.taskListItem
            itemText="Review record of decision"
            itemUrl=springUrl("#")
            useNotCompletedLabels=true />
        </@fdsTaskList.taskListSection>
    </@fdsTaskList.singleTaskList>

</@defaultPage>

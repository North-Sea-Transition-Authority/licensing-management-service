<#include '../layout/layout.ftl'>

<#macro standardTaskList taskListSections>
  <@fdsTaskList.taskList>
    <#list taskListSections as section>
        <@fdsTaskList.taskListSection
          sectionNumber="${section?index + 1}"
          sectionHeadingText=section.displayName()>
          <#list section.items() as item>
            <@fdsTaskList.taskListItem
              itemText=item.displayName()
              itemUrl=springUrl(item.actionUrl())
              showTag=item.label()?has_content
              completed=item.label()?has_content && item.label().name() == 'COMPLETE'
              useNotCompletedLabels=true />
          </#list>
        </@fdsTaskList.taskListSection>
    </#list>
  </@fdsTaskList.taskList>
</#macro>
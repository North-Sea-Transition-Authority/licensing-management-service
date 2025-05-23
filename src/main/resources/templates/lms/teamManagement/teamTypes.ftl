<#-- @ftlvariable name="teamTypeViews" type="java.util.List<uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamTypeView>" -->
<#include '../layout/layout.ftl'>

<#assign pageTitle="Select a team"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle

pageSize=PageSize.TWO_THIRDS_COLUMN
>

    <@fdsResultList.resultList>
        <#list teamTypeViews as teamTypeView>
            <@fdsResultList.resultListItem
            linkHeadingUrl=springUrl(teamTypeView.manageUrl())
            linkHeadingText=teamTypeView.teamTypeName()/>
        </#list>
    </@fdsResultList.resultList>

</@defaultPage>
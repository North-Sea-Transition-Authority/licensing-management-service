<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import 'customTimeline.ftl' as custom>

<@defaultPage
htmlTitle="CS021 - Licence schedule and work programme"
pageHeading="CS021 - Licence schedule and work programme"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
            1 August 2023
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="End date">
            1 August 2029
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Duration">
            6 years
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText="Licence round">
            3
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
    <@fdsSearch.searchPage>

        <@fdsSearch.searchFilter>
            <@fdsSearch.searchFilterList
            clearFilterText="Clear filters"
            clearFilterUrl="#">
                <@fdsSearch.searchFilterItem itemName="Show" expanded=true>
                    <@fdsSearch.searchCheckboxes path="form.filter" checkboxes=options/>
                </@fdsSearch.searchFilterItem>
            </@fdsSearch.searchFilterList>
        </@fdsSearch.searchFilter>

        <@fdsSearch.searchPageContent>

            <@fdsAccordion.accordion accordionId="accordion-example">
                <@fdsAccordion.accordionSection sectionHeading="Appraisal Term" summaryText="1 August 2023 - 1 August 2029 (6 years)">
                    <@fdsTimeline.timeline>
                        <@fdsTimeline.timelineSection>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="Start of Appraisal Term"
                            timeStampHeadingHint="1 August 2023 - 1 August 2029 (6 years)"
                            >
                            </@fdsTimeline.timelineTimeStamp>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Early risk assessment"
                            timeStampHeadingHint="31 January 2024"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Seismic reprocessing & interpretation"
                            timeStampHeadingHint="31 January 2024"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Obtain or otherwise access a minimum of 900 km2 3D seismic.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Early risk assessment workshop"
                            timeStampHeadingHint="29 February 2024"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Early risk assessment further measures"
                            timeStampHeadingHint="30 March 2024"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Seismic reprocessing & interpretation"
                            timeStampHeadingHint="31 January 2025"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Reprocess to pre-SDM and interpret a minimum of 900 km2 3D seismic.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Above-ground assessment"
                            timeStampHeadingHint="31 January 2026"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                        Above-ground assessment including but not limited to:
                                        a) a pipeline CO2 transportation study including evaluating the
                                        technical and commercial potential of pipeline-based CO2
                                        transportation to the potential site(s); and
                                        b) a shipped CO2 transportation study including evaluating the
                                        technical and commercial potential of ship-borne transportation
                                        of CO2 to the potential site(s).
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Well or injectivity test"
                            timeStampHeadingHint="31 July 2026"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                    Complete the drilling of an appraisal well on a preferred candidate storage site to a depth of 1650 metres TVDSS or 30 metres below the base of the Bunter Sandstone Formation, and acquire data such that the key uncertainties pertinent to the potential storage of carbon dioxide within the...
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Site characterisation review report"
                            timeStampHeadingHint="30 April 2027"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Seismic acquisition and processing"
                            timeStampHeadingHint="30 April 2027"
                            >
                                <@fdsTimeline.timelineEvent>
                                    <p class="govuk-body">
                                    Commit to acquire (shoot) a minimum of 200 sq km 3D seismic or surrender the Licence, unless the OGA agrees that the reprocessed seismic data acquired in paragraph 2.2 is of sufficient quality to characterise the site and complex sufficiently to submit a permit application capable of being approved.
                                    </p>
                                </@fdsTimeline.timelineEvent>
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="End assess phase review"
                            timeStampHeadingHint="30 April 2028"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="End define phase review"
                            timeStampHeadingHint="31 September 2028"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@custom.timelineTimeStampSmall
                            timeStampHeading="Storage permit application"
                            timeStampHeadingHint="31 January 2029"
                            >
                            </@custom.timelineTimeStampSmall>
                            <@fdsTimeline.timelineTimeStamp
                            timeStampHeading="End of Appraisal Term"
                            timeStampHeadingHint="1 August 2029"
                            timeStampClass="fds-timeline__time-stamp--no-border">
                            </@fdsTimeline.timelineTimeStamp>
                        </@fdsTimeline.timelineSection>
                    </@fdsTimeline.timeline>
                </@fdsAccordion.accordionSection>
            </@fdsAccordion.accordion>

        </@fdsSearch.searchPageContent>
    </@fdsSearch.searchPage>

</@defaultPage>
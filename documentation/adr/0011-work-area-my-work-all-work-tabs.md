# Work Area My Work / All Work Tabs

* Status: proposed
* Date: 2026-09-14

Technical Story: [LMS1-649](https://fivium.atlassian.net/browse/LMS1-649)

## Background

The work area currently shows one flat list, combining all four application types (schedule,
schedule + work programme, continuation, correction) with no split by who it's assigned to.

Both tabs already only ever show what the current user's licence-type role gives them access to
today (unchanged by this ticket). Within that existing scope, case allocation needs two tabs:

* **My work**: of the applications you already have access to, only the ones personally
  actionable by you. What that means depends on your role: a steward's My work is cases
  assigned to them; a case manager's My work is unassigned cases, since allocating a steward is
  the case manager's job to do.
* **All work**: every application you already have access to, whoever it's assigned to (or not
  yet assigned to anyone). The shared pool everyone uses to review or reassign already-assigned
  cases.

Only schedule + work programme applications have this kind of "assigned steward" today.
Schedule and correction applications track who created them (so a drafter only sees their own
in-progress drafts), but that's ownership of a draft, not a case being assigned to a case
manager. Continuation applications track neither. So 3 of the 4 application types have no
existing concept of "this case belongs to a specific person" to build the split on.

## What already decides who sees what

The system already asks two questions per application type, answered differently each time.
Tabs add a third, answered the same way.

* **Can this person open it directly?** Mostly team roles and organisation membership.
  Correction instead checks who it's allocated to.
* **Does it show up in the work area at all?** Already a different answer to the one above:
  * A reviewer keeps direct access to a continuation application once an issuer takes over, but
    it drops out of their work area.
  * Regulators never see drafts in their work area, even where they could otherwise open one.
  * Correction and schedule ignore the role/organisation rules here entirely: each person sees
    only their own allocated cases.
* **Which tab should it show in?** Not answered anywhere today. Same as above: no single central
  rule works for all four types, so each type answers this one itself too. It's asked only once
  an application has already passed the two checks above, so it doesn't change who can open an
  application directly or the existing work-area visibility rules.

That's now three separate rule sets per application type, kept in sync by convention rather than
by design. [ADR-0005](0005-application-access-and-work-area-visibility-policy-dsl.md) already
proposed a single place to define all of this per application type, but was never implemented.
Worth revisiting once this feature ships, so future rule changes aren't spread across the same
three places again.

## Approach

Each application type is given two separate answers to provide instead of one: "what's in my
work?" and "what's in all work?" Every application type has to answer both, so none can be
skipped or forgotten when a new one is added later.

* **Schedule + work programme applications** (the only type with a steward assigned to a case):
  * *My work* depends on your role. A steward's My work = cases within your existing access
    scope assigned to you. A case manager's My work = unassigned cases within that scope,
    since allocating a steward is their job, not something assigned to them.
  * *All work* = every case within your existing access scope, assigned or not: unassigned ones
    so any case manager can claim one, assigned ones (to you or anyone else) so case managers
    and stewards can review or reassign them.
* **Schedule, continuation, correction applications** (no case-level assignment yet):
  * Both answers are the same list: whatever your existing access scope shows today.
  * They show up in both tabs unchanged. There's no one to claim these from yet, in the
    case-allocation sense this ticket is about. A drafter still only sees their own drafts, as
    now. That's unrelated to this split.

Another Fivium service (`integrated-reporting-system`) already runs this exact "My/All" split in
production for its own work area, built the same way. This isn't a new pattern.

For developers, this is implemented as two methods on the existing per-type work area class,
replacing the single method it has today:

```java
public interface WorkAreaItemProvider extends PhaseGated {
  List<SearchResultItem> getMyWorkItems(WorkAreaFilterForm form, ServiceUserDetail user);
  List<SearchResultItem> getAllWorkItems(WorkAreaFilterForm form, ServiceUserDetail user);
}
```

## Pages

* `/work-area` stays the "My work" page (the default, so nothing that already links here breaks)
* `/work-area/all-work` is the new "All work" page

Both use a tabs component already used elsewhere in this app, for individual case screens, just
not the work area yet. It's a normal page load per tab, so no new front-end work is needed.
Applying or clearing a filter needs to know which tab you were on, so it can send you back to
the right one.

## Links

* `integrated-reporting-system`'s work area: the equivalent My/All split, live in production,
  used as the model for this design.
* [ADR-0005](0005-application-access-and-work-area-visibility-policy-dsl.md) (a single rule
  definition per application type, for access and work-area visibility). Never implemented; this
  decision doesn't depend on it, but adds a third scattered rule set to the same problem it
  raised.

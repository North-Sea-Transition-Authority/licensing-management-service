# Investigation: PEARS Operation Statuses

## Problem

`src/main/resources/pears-migration/licence-history.sql` and `licence-references.sql` both end with

```sql
AND po.status IN ('LIVE','LEGACY','CORRECTED') -- only operations in status relevant to the execution
```

`PearsOperation.OperationStatus` names the same three. Nothing in this repository says what the
three mean, why PEARS holds seven statuses and we read three, or whether reading all three of them
at once risks counting the same operation twice. That is the question this document answers.

## Answer in one line

**The status records how an operation came to exist, not which version of it is current.** Currency
is a separate mechanism (`LEGACY-DELETED` plus transaction-row versioning), which is why all three
can be read together without double-counting.

## The seven statuses

Row counts from `PEDMGR.PED_OPERATIONS` on `db-ogadev1`. The second column is the set the migration
actually reads — live simulation (`ped_sim_id = 0`) and `EXECUTED` transactions.

| Status           | All rows | sim-0 EXECUTED | Meaning                                                                                         |
|------------------|---------:|---------------:|-------------------------------------------------------------------------------------------------|
| `LEGACY`         |  169,206 |         43,036 | Pre-PEARS licence history, loaded or keyed in outside the application workflow                  |
| `LIVE`           |   44,753 |          4,300 | Raised as an application in PEARS, approved, and executed                                       |
| `CORRECTED`      |   34,115 |          1,721 | Created or replaced inside a licence correction                                                 |
| `LEGACY-DELETED` |   13,153 |            441 | Tombstone — the row a correction replaced, or the operations of a cancelled transaction version |
| `DRAFT`          |    2,252 |              0 | In an application that has not executed                                                         |
| `RESUBMITTED`    |      201 |              0 | Industry asked for the operation to be executed on a later date                                 |
| `WITHDRAWN`      |       64 |              0 | Industry withdrew the operation from execution                                                  |

`DRAFT`, `RESUBMITTED` and `WITHDRAWN` cannot appear in the set we read: all three describe an
operation that has not executed, and the query only reads `EXECUTED` transactions.

## Where each status comes from

Every transition lives in `PEDMGR.PED_TXNS`. Line numbers are as of this investigation; the
procedure names are the durable anchor.

**`DRAFT` → `LIVE` on execution.** The execute path clones the approved transaction's operations
into the executed transaction row and maps the status as it goes (body L3379):

```sql
, DECODE(po.status, 'DRAFT', 'LIVE', po.status)
```

followed by L3396, which does the same to the operations left on the approved row:

```sql
UPDATE pedmgr.ped_operations SET status = 'LIVE' WHERE id IN (...) AND status = 'DRAFT';
```

Note the `DECODE`: anything that is not `DRAFT` keeps the status it already had. Execution never
produces `LEGACY`, and it never overwrites `CORRECTED`.

**`CORRECTED` is stamped at creation, while a correction is open.** Operations built during a
correction are created with it outright — `LICENSING_SPATIAL` L1855:

```sql
, p_status => CASE l_is_correcting WHEN 'true' THEN 'CORRECTED' ELSE 'DRAFT' END
```

and `PED_TXNS.correct_operation` (L6758–6829) corrects an existing operation by inserting a *copy*
of it as `CORRECTED` with `parent_ped_oper_id` pointing back at the original, then tombstoning the
original:

```sql
INSERT INTO pedmgr.ped_operations (... parent_ped_oper_id, status ...)
SELECT l_new_oper_id, po.ped_tran_id, po.operation_sequence, po.xml_data
     , p_ped_operation_id, 'CORRECTED', ... po.pom_id ...
FROM pedmgr.ped_operations po WHERE po.id = p_ped_operation_id;

update_operation_status(p_ped_operation_id, 'LEGACY-DELETED', p_wua_id);
```

The `parent_ped_oper_id` link only lives as long as the correction does — 56 rows carry one, all on
`CORRECTING` transactions, because the clone performed on execution passes `NULL` for it.

**`LEGACY` is never stamped on anything new.** No code path sets it on a created operation. It
appears only as the *restore* default when a correction is undone —
`undo_correct_operation(p_operation_status VARCHAR2 DEFAULT 'LEGACY')` and `undo_delete_operation`,
both meaning "put it back to how a pre-existing operation looks". Its origin is the load into
PEARS: all 9,851 `PED_TRANSACTION_MASTER` rows with status `MIGRATED` have no process behind them
(`pf_id` null), and every one of their 35,949 operations is `LEGACY`, with no other status present
under a `MIGRATED` master. `LEGACY` execution dates are effectively all pre-2013; PEARS' own epoch
starts 2012-03-31.

**`LEGACY-DELETED` is the tombstone, and it is the only one.** Besides `correct_operation` above,
`delete_operation` and `create_new_tran_version` apply it. The latter (L1067–1205) is the routine
that re-versions a transaction — it clones the whole transaction into a new row, **preserving each
operation's status**, cancels the old row, and tombstones its operations wholesale:

```sql
INSERT INTO pedmgr.ped_operations (... status, pom_id ...)
SELECT ..., po.status, po.pom_id, ... FROM pedmgr.ped_operations po
WHERE po.ped_tran_id = l_current_tran_id;             -- status carried across verbatim

UPDATE pedmgr.ped_transactions pt SET pt.status = 'CANCELLED', pt.end_datetime = l_sysdate ...
UPDATE pedmgr.ped_operations po SET po.status = 'LEGACY-DELETED' WHERE ped_tran_id = l_current_tran_id;
```

A `LEGACY` operation therefore stays `LEGACY` through every correction it survives. The status is
provenance; it does not change over an operation's life.

## Why PEARS needs the distinction

Two independent axes, one of which ended up in this column.

**Provenance (this column).** PEARS went live in 2012 on top of decades of licence history that had
to become positions in the live simulation without ever having been an application. `LEGACY` marks
"this predates PEARS, or was back-captured, and there was no consent workflow behind it". `LIVE`
marks "a licensee applied, the regulator approved, it executed". `CORRECTED` marks "the regulator
fixed the record after the fact". PEARS needs the distinction because it gates what may be done to
an operation — an operation that was never submitted cannot be withdrawn or resubmitted, and the
withdraw/resubmit paths in `PED_TXNS` key off exactly that.

**Currency (a different mechanism).** `PED_TRANSACTIONS` rows are versions of a `PTM_ID`, carrying
`start_datetime`/`end_datetime` and `start_correction_id`/`end_correction_id`;
`PED_OPERATION_MASTER` (`POM_ID`) is the operation's identity across those versions. Superseded
rows are tombstoned as `LEGACY-DELETED`. None of this touches the provenance value.

## Consequence for the migration

Within one executed transaction the three statuses are **complementary, not competing**. Across the
12,890 sim-0 executed transactions, `operation_sequence` is unique across `LIVE`/`LEGACY`/
`CORRECTED` in all but 5. A worked example — transaction 38463, licence P735, position 1997-06-14:

```
oseq 1  LIVE            PED_BLOCK_CHANGE
oseq 2  LIVE            PED_BLOCK_CHANGE
oseq 3  LEGACY          PED_SUBAREA_END
oseq 4  LEGACY          PED_SUBAREA_CHANGE
oseq 5  LEGACY-DELETED  PED_SUBAREA_CHANGE   <- superseded by a correction
oseq 5  CORRECTED       PED_SUBAREA_CHANGE   <- the survivor
```

One position, four surviving operations of three different provenances, plus one corrected pair.

Three things follow:

1. **The `IN ('LIVE','LEGACY','CORRECTED')` filter is correct and complete.** It selects the
   surviving operations of an executed transaction regardless of where they came from. PEARS' own
   queries use the same set plus `DRAFT` (`PED_TXNS` L2416/L4652, `LICENSING` L2256/L3385), and
   `DRAFT` is rightly absent here because we only read `EXECUTED` transactions.
2. **Status must never rank or dedupe operations** — only include or exclude them. A migrator that
   preferred `LIVE` over `LEGACY`, or treated `CORRECTED` as a duplicate of something, would drop
   real history.
3. **A `CORRECTED` operation is the post-correction truth.** Its predecessor is `LEGACY-DELETED`
   and is already excluded, so the migration reads corrected values rather than originals without
   having to do anything about it.

Status is not carried into the LMS model, and no `PearsOperationMigrator` branches on it.
`PearsOperation.Header.operationStatus` is bound as a tripwire rather than for a migrator's use:
`PearsOperationMapper.operationStatus()` throws on any value outside the three, so a status the
enum does not hold means the query's filter has changed underneath us. Any future migrator that
thinks it needs to branch on status should re-read this document first.

## Appendix — how this was established

Queried the PEARS Oracle dev instance (`jdbc:oracle:thin:@//db-ogadev1.sb2.dev:1521/ogadev1`, the
datasource in `application-local.yml`) directly:

- Status distributions across `PED_OPERATIONS`, cross-tabulated against `PED_TRANSACTIONS.STATUS`,
  `PED_TRANSACTION_MASTER.STATUS`/`PF_ID`, `PED_SIMULATION_TRANSACTIONS.PED_SIM_ID`, operation
  type, `EXECUTION_DATE`, `CREATED_DATETIME` and `CREATED_BY_WUA_ID`.
- A duplicate-`operation_sequence` check over every sim-0 executed transaction, to establish that
  the three statuses do not overlap within a transaction.
- Per-`POM_ID` version histories, to establish that status survives re-versioning unchanged.
- `ALL_SOURCE` for `PEDMGR`, reading every occurrence of the status literals in `PED_TXNS`,
  `LICENSING`, `LICENSING_SPATIAL`, `LICENCE_DECLARATIONS`, `LICENCE_REPLAY` and `PED_UTILS` to
  find every site that writes `PED_OPERATIONS.STATUS`.

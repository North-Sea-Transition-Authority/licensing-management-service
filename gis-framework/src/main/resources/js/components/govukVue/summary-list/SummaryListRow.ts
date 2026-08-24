import {SummaryListRowAction} from "./SummaryListRowAction";

export interface SummaryListRow {
    key: Symbol
    actions: SummaryListRowAction[]
}
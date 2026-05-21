"use client";

import { downloadCsv, toCsv } from "@/lib/csv";
import type { DailyPoint, WeeklyPoint } from "@/components/school-charts";

export function SchoolCsvButton({
  daily,
  weekly,
  from,
  to,
}: {
  daily: DailyPoint[];
  weekly: WeeklyPoint[];
  from: string;
  to: string;
}) {
  function run() {
    const dailyCsv = toCsv(
      ["day", "compliance_pct", "bypass_count"],
      daily.map((d) => [
        d.day,
        d.pct == null ? "" : String(d.pct),
        String(d.bypass),
      ]),
    );
    downloadCsv(`school-daily_${from}_${to}.csv`, dailyCsv);

    const weeklyCsv = toCsv(
      ["week_start_monday_utc", "compliance_pct", "bypass_count"],
      weekly.map((w) => [
        w.week,
        w.pct == null ? "" : String(w.pct),
        String(w.bypass),
      ]),
    );
    downloadCsv(`school-weekly_${from}_${to}.csv`, weeklyCsv);
  }

  return (
    <button
      type="button"
      onClick={run}
      className="rounded border border-white/15 px-3 py-1.5 text-sm text-white hover:bg-white/10"
    >
      Download CSV (daily + weekly)
    </button>
  );
}

"use client";

import { downloadCsv, toCsv } from "@/lib/csv";

export type RosterRow = {
  id: string;
  display_name: string;
  created_at: string;
};

export function StudentsRosterCsvButton({ rows }: { rows: RosterRow[] }) {
  function run() {
    const csv = toCsv(
      ["student_id", "display_name", "created_at_utc"],
      rows.map((r) => [r.id, r.display_name, r.created_at]),
    );
    downloadCsv("students-roster.csv", csv);
  }

  return (
    <button
      type="button"
      onClick={run}
      className="rounded border border-white/15 px-3 py-1.5 text-sm text-white hover:bg-white/10"
    >
      Download roster CSV
    </button>
  );
}

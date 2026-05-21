"use client";

import { downloadCsv, toCsv } from "@/lib/csv";

export type SessionExport = {
  id: string;
  started_at: string;
  ended_at: string | null;
  duration_seconds: number | null;
  client_session_id: string;
};

export function StudentSessionsCsvButton({
  rows,
  studentId,
}: {
  rows: SessionExport[];
  studentId: string;
}) {
  function run() {
    const csv = toCsv(
      [
        "session_row_id",
        "student_id",
        "client_session_id",
        "started_at_utc",
        "ended_at_utc",
        "duration_seconds",
      ],
      rows.map((r) => [
        r.id,
        studentId,
        r.client_session_id,
        r.started_at,
        r.ended_at ?? "",
        r.duration_seconds == null ? "" : String(r.duration_seconds),
      ]),
    );
    downloadCsv(`student-sessions_${studentId}.csv`, csv);
  }

  return (
    <button
      type="button"
      onClick={run}
      className="rounded border border-white/15 px-3 py-1.5 text-sm text-white hover:bg-white/10"
    >
      Download sessions CSV
    </button>
  );
}

export type BypassExport = {
  id: string;
  event_type: string;
  event_at: string;
  session_id: string | null;
};

export function StudentBypassCsvButton({
  rows,
  studentId,
}: {
  rows: BypassExport[];
  studentId: string;
}) {
  function run() {
    const csv = toCsv(
      ["bypass_row_id", "student_id", "session_id", "event_type", "event_at_utc"],
      rows.map((r) => [
        r.id,
        studentId,
        r.session_id ?? "",
        r.event_type,
        r.event_at,
      ]),
    );
    downloadCsv(`student-bypasses_${studentId}.csv`, csv);
  }

  return (
    <button
      type="button"
      onClick={run}
      className="rounded border border-white/15 px-3 py-1.5 text-sm text-white hover:bg-white/10"
    >
      Download bypass CSV
    </button>
  );
}

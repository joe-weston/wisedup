import Link from "next/link";
import { notFound } from "next/navigation";
import { DateRangeForm } from "@/components/date-range-form";
import {
  StudentBypassCsvButton,
  StudentSessionsCsvButton,
  type BypassExport,
  type SessionExport,
} from "@/components/student-detail-csv-buttons";
import { parseRangeFromSearchParams } from "@/lib/dateRange";
import { createClient } from "@/lib/supabase/server";

type Props = {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function StudentDetailPage({ params, searchParams }: Props) {
  const { id } = await params;
  const { from, to } = await parseRangeFromSearchParams(searchParams);
  const fromTs = `${from}T00:00:00.000Z`;
  const toTs = `${to}T23:59:59.999Z`;
  const qs = new URLSearchParams({ from, to }).toString();

  const supabase = await createClient();
  const { data: student } = await supabase
    .from("students")
    .select("id, display_name, device_id, created_at")
    .eq("id", id)
    .maybeSingle();

  if (!student) {
    notFound();
  }

  const [sessionsRes, bypassRes] = await Promise.all([
    supabase
      .from("focus_sessions")
      .select("id, started_at, ended_at, duration_seconds, client_session_id")
      .eq("student_id", id)
      .gte("started_at", fromTs)
      .lte("started_at", toTs)
      .order("started_at", { ascending: false })
      .limit(500),
    supabase
      .from("bypass_events")
      .select("id, event_type, event_at, session_id")
      .eq("student_id", id)
      .gte("event_at", fromTs)
      .lte("event_at", toTs)
      .order("event_at", { ascending: false })
      .limit(500),
  ]);

  const sessionRows: SessionExport[] = (sessionsRes.data ?? []) as SessionExport[];
  const bypassRows: BypassExport[] = (bypassRes.data ?? []) as BypassExport[];

  return (
    <div className="space-y-8">
      <div>
        <Link
          href={`/students?${qs}`}
          className="text-sm text-[var(--accent)] hover:underline"
        >
          ← Back to students
        </Link>
        <h1 className="mt-4 text-2xl font-semibold text-white">
          {student.display_name}
        </h1>
        <p className="mt-1 font-mono text-xs text-[var(--muted)]">{student.id}</p>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Device ID: <span className="font-mono text-white/80">{student.device_id}</span>
        </p>
      </div>

      <DateRangeForm from={from} to={to} actionPath={`/students/${id}`} />

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-medium text-white">Focus sessions</h2>
          <StudentSessionsCsvButton rows={sessionRows} studentId={id} />
        </div>
        <div className="overflow-x-auto rounded-lg border border-white/10">
          <table className="w-full min-w-[40rem] text-left text-sm">
            <thead className="border-b border-white/10 bg-black/20 text-xs uppercase text-[var(--muted)]">
              <tr>
                <th className="px-3 py-2">Started (UTC)</th>
                <th className="px-3 py-2">Ended (UTC)</th>
                <th className="px-3 py-2">Duration</th>
                <th className="px-3 py-2">Client session</th>
              </tr>
            </thead>
            <tbody>
              {sessionRows.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-3 py-6 text-center text-[var(--muted)]">
                    No sessions with start time in this range.
                  </td>
                </tr>
              ) : (
                sessionRows.map((r) => (
                  <tr
                    key={r.id}
                    className="border-b border-white/5 last:border-0 hover:bg-white/[0.03]"
                  >
                    <td className="px-3 py-2 font-mono text-xs text-white/90">
                      {fmt(r.started_at)}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs text-[var(--muted)]">
                      {r.ended_at ? fmt(r.ended_at) : "—"}
                    </td>
                    <td className="px-3 py-2 text-[var(--muted)]">
                      {r.duration_seconds != null ? `${r.duration_seconds}s` : "—"}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs text-[var(--muted)]">
                      {r.client_session_id}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-medium text-white">Bypass events</h2>
          <StudentBypassCsvButton rows={bypassRows} studentId={id} />
        </div>
        <div className="overflow-x-auto rounded-lg border border-white/10">
          <table className="w-full min-w-[28rem] text-left text-sm">
            <thead className="border-b border-white/10 bg-black/20 text-xs uppercase text-[var(--muted)]">
              <tr>
                <th className="px-3 py-2">Event at (UTC)</th>
                <th className="px-3 py-2">Type</th>
                <th className="px-3 py-2">Session</th>
              </tr>
            </thead>
            <tbody>
              {bypassRows.length === 0 ? (
                <tr>
                  <td colSpan={3} className="px-3 py-6 text-center text-[var(--muted)]">
                    No bypass events in this range.
                  </td>
                </tr>
              ) : (
                bypassRows.map((r) => (
                  <tr
                    key={r.id}
                    className="border-b border-white/5 last:border-0 hover:bg-white/[0.03]"
                  >
                    <td className="px-3 py-2 font-mono text-xs text-white/90">
                      {fmt(r.event_at)}
                    </td>
                    <td className="px-3 py-2 text-[var(--muted)]">{r.event_type}</td>
                    <td className="px-3 py-2 font-mono text-xs text-[var(--muted)]">
                      {r.session_id ?? "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function fmt(iso: string): string {
  return new Date(iso).toISOString().replace("T", " ").slice(0, 19);
}

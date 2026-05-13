import { DateRangeForm } from "@/components/date-range-form";
import { SchoolCharts, type DailyPoint, type WeeklyPoint } from "@/components/school-charts";
import { SchoolCsvButton } from "@/components/school-csv-button";
import { parseRangeFromSearchParams } from "@/lib/dateRange";
import type { BypassRow, FocusSessionRow } from "@/lib/metrics";
import {
  avgSessionLengthSeconds,
  buildDailyBypassCounts,
  buildDailyCompliance,
  buildWeeklyBypassCounts,
  buildWeeklyCompliance,
  distinctWeekMondayKeysInRange,
  eachUtcDayKey,
  overallCompliancePct,
} from "@/lib/metrics";
import { createClient } from "@/lib/supabase/server";

export default async function SchoolPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { from, to } = await parseRangeFromSearchParams(searchParams);
  const fromTs = `${from}T00:00:00.000Z`;
  const toTs = `${to}T23:59:59.999Z`;

  const supabase = await createClient();

  const [sessionsRes, bypassRes, enrolledRes] = await Promise.all([
    supabase
      .from("focus_sessions")
      .select("student_id, started_at, ended_at, duration_seconds")
      .gte("started_at", fromTs)
      .lte("started_at", toTs),
    supabase
      .from("bypass_events")
      .select("event_at")
      .gte("event_at", fromTs)
      .lte("event_at", toTs),
    supabase.from("students").select("id", { count: "exact", head: true }),
  ]);

  const sessions = (sessionsRes.data ?? []) as FocusSessionRow[];
  const bypasses = (bypassRes.data ?? []) as BypassRow[];
  const enrolledCount = enrolledRes.count ?? 0;

  const dayKeys = eachUtcDayKey(from, to);
  const weekKeys = distinctWeekMondayKeysInRange(from, to);

  const dailyCompliance = buildDailyCompliance(sessions, enrolledCount, dayKeys);
  const weeklyCompliance = buildWeeklyCompliance(sessions, enrolledCount, weekKeys);
  const dailyBypass = buildDailyBypassCounts(bypasses, dayKeys);
  const weeklyBypass = buildWeeklyBypassCounts(bypasses, weekKeys);

  const daily: DailyPoint[] = dailyCompliance.map((d, i) => ({
    day: d.day,
    pct: d.pct,
    bypass: dailyBypass[i]?.count ?? 0,
  }));

  const weekly: WeeklyPoint[] = weeklyCompliance.map((w, i) => ({
    week: w.week,
    pct: w.pct,
    bypass: weeklyBypass[i]?.count ?? 0,
  }));

  const overallPct = overallCompliancePct(sessions, enrolledCount);
  const avgLen = avgSessionLengthSeconds(sessions);
  const totalBypass = bypasses.length;

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-white">School overview</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            Metrics for sessions <strong>started</strong> between {from} and {to}{" "}
            (UTC). Bypass counts use <strong>event_at</strong> in the same range.
          </p>
        </div>
        <SchoolCsvButton daily={daily} weekly={weekly} from={from} to={to} />
      </div>

      <DateRangeForm from={from} to={to} actionPath="/school" />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="Enrolled students" value={String(enrolledCount)} />
        <Kpi
          label="Avg session length"
          value={avgLen == null ? "—" : formatDuration(avgLen)}
          hint="Completed sessions in range"
        />
        <Kpi
          label="Compliance (range)"
          value={overallPct == null ? "—" : `${overallPct}%`}
          hint="Students with ≥1 qualifying session / enrolled"
        />
        <Kpi label="Bypass events (range)" value={String(totalBypass)} />
      </div>

      <SchoolCharts daily={daily} weekly={weekly} />
    </div>
  );
}

function Kpi({
  label,
  value,
  hint,
}: {
  label: string;
  value: string;
  hint?: string;
}) {
  return (
    <div className="rounded-lg border border-white/10 bg-[var(--card)] p-4">
      <p className="text-xs uppercase tracking-wide text-[var(--muted)]">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-white">{value}</p>
      {hint ? <p className="mt-1 text-xs text-[var(--muted)]">{hint}</p> : null}
    </div>
  );
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m >= 60) {
    const h = Math.floor(m / 60);
    const mm = m % 60;
    return `${h}h ${mm}m`;
  }
  return `${m}m ${s}s`;
}

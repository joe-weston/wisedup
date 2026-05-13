/** Compliance metrics per ADR-006 (UTC buckets). */

export type FocusSessionRow = {
  student_id: string;
  started_at: string;
  ended_at: string | null;
  duration_seconds: number | null;
};

export type BypassRow = {
  event_at: string;
};

function utcDateKey(iso: string): string {
  const d = new Date(iso);
  return d.toISOString().slice(0, 10);
}

/** Monday-start week; label = Monday's UTC date (YYYY-MM-DD). */
export function utcWeekMondayKey(iso: string): string {
  const d = new Date(iso);
  const day = d.getUTCDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  const monday = new Date(
    Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() + mondayOffset),
  );
  return monday.toISOString().slice(0, 10);
}

export function eachUtcDayKey(fromIsoDate: string, toIsoDate: string): string[] {
  const out: string[] = [];
  const cur = new Date(fromIsoDate + "T00:00:00.000Z");
  const end = new Date(toIsoDate + "T00:00:00.000Z");
  if (cur > end) return out;
  while (cur <= end) {
    out.push(cur.toISOString().slice(0, 10));
    cur.setUTCDate(cur.getUTCDate() + 1);
  }
  return out;
}

export function isQualifyingSession(row: FocusSessionRow): boolean {
  if (row.ended_at == null) return false;
  const d = row.duration_seconds;
  return d != null && d >= 60;
}

export function buildDailyCompliance(
  sessions: FocusSessionRow[],
  enrolledCount: number,
  dayKeys: string[],
): { day: string; pct: number | null; compliantStudents: number }[] {
  const byDay = new Map<string, Set<string>>();
  for (const day of dayKeys) {
    byDay.set(day, new Set());
  }
  for (const s of sessions) {
    if (!isQualifyingSession(s)) continue;
    const day = utcDateKey(s.started_at);
    const set = byDay.get(day);
    if (set) set.add(s.student_id);
  }
  return dayKeys.map((day) => {
    const compliant = byDay.get(day)?.size ?? 0;
    const pct =
      enrolledCount > 0 ? Math.round((100 * compliant) / enrolledCount) : null;
    return { day, pct, compliantStudents: compliant };
  });
}

export function buildWeeklyCompliance(
  sessions: FocusSessionRow[],
  enrolledCount: number,
  weekKeys: string[],
): { week: string; pct: number | null; compliantStudents: number }[] {
  const byWeek = new Map<string, Set<string>>();
  for (const w of weekKeys) {
    byWeek.set(w, new Set());
  }
  const weekKeySet = new Set(weekKeys);
  for (const s of sessions) {
    if (!isQualifyingSession(s)) continue;
    const wk = utcWeekMondayKey(s.started_at);
    if (!weekKeySet.has(wk)) continue;
    const set = byWeek.get(wk);
    if (set) set.add(s.student_id);
  }
  return weekKeys.map((week) => {
    const compliant = byWeek.get(week)?.size ?? 0;
    const pct =
      enrolledCount > 0 ? Math.round((100 * compliant) / enrolledCount) : null;
    return { week, pct, compliantStudents: compliant };
  });
}

export function distinctWeekMondayKeysInRange(
  fromIsoDate: string,
  toIsoDate: string,
): string[] {
  const days = eachUtcDayKey(fromIsoDate, toIsoDate);
  const weeks = new Set<string>();
  for (const d of days) {
    weeks.add(utcWeekMondayKey(d + "T12:00:00.000Z"));
  }
  return Array.from(weeks).sort();
}

export function buildDailyBypassCounts(
  events: BypassRow[],
  dayKeys: string[],
): { day: string; count: number }[] {
  const counts = new Map<string, number>();
  for (const d of dayKeys) counts.set(d, 0);
  for (const e of events) {
    const day = utcDateKey(e.event_at);
    if (counts.has(day)) counts.set(day, (counts.get(day) ?? 0) + 1);
  }
  return dayKeys.map((day) => ({ day, count: counts.get(day) ?? 0 }));
}

export function buildWeeklyBypassCounts(
  events: BypassRow[],
  weekKeys: string[],
): { week: string; count: number }[] {
  const counts = new Map<string, number>();
  for (const w of weekKeys) counts.set(w, 0);
  const weekSet = new Set(weekKeys);
  for (const e of events) {
    const wk = utcWeekMondayKey(e.event_at);
    if (!weekSet.has(wk)) continue;
    counts.set(wk, (counts.get(wk) ?? 0) + 1);
  }
  return weekKeys.map((week) => ({ week, count: counts.get(week) ?? 0 }));
}

export function overallCompliancePct(
  sessions: FocusSessionRow[],
  enrolledCount: number,
): number | null {
  if (enrolledCount <= 0) return null;
  const compliant = new Set<string>();
  for (const s of sessions) {
    if (isQualifyingSession(s)) compliant.add(s.student_id);
  }
  return Math.round((100 * compliant.size) / enrolledCount);
}

export function avgSessionLengthSeconds(
  sessions: FocusSessionRow[],
): number | null {
  const vals: number[] = [];
  for (const s of sessions) {
    if (s.ended_at != null && s.duration_seconds != null && s.duration_seconds > 0) {
      vals.push(s.duration_seconds);
    }
  }
  if (vals.length === 0) return null;
  return Math.round(vals.reduce((a, b) => a + b, 0) / vals.length);
}

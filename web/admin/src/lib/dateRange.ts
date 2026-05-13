/** YYYY-MM-DD in UTC for "today". */
export function utcTodayDateString(): string {
  return new Date().toISOString().slice(0, 10);
}

export function defaultRange(): { from: string; to: string } {
  const to = utcTodayDateString();
  const t = new Date(to + "T00:00:00.000Z");
  t.setUTCDate(t.getUTCDate() - 13);
  const from = t.toISOString().slice(0, 10);
  return { from, to };
}

export function parseRange(search: URLSearchParams): { from: string; to: string } {
  const def = defaultRange();
  const from = search.get("from")?.trim() || def.from;
  const to = search.get("to")?.trim() || def.to;
  const fromOk = /^\d{4}-\d{2}-\d{2}$/.test(from);
  const toOk = /^\d{4}-\d{2}-\d{2}$/.test(to);
  if (!fromOk || !toOk) return def;
  if (from > to) return def;
  return { from, to };
}

export async function parseRangeFromSearchParams(
  searchParams: Promise<Record<string, string | string[] | undefined>>,
): Promise<{ from: string; to: string }> {
  const raw = await searchParams;
  const usp = new URLSearchParams();
  for (const [key, val] of Object.entries(raw)) {
    if (val == null) continue;
    if (Array.isArray(val)) {
      val.forEach((v) => usp.append(key, v));
    } else {
      usp.set(key, val);
    }
  }
  return parseRange(usp);
}

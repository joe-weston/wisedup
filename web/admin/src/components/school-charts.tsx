"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type DailyPoint = { day: string; pct: number | null; bypass: number };

export type WeeklyPoint = { week: string; pct: number | null; bypass: number };

export function SchoolCharts({
  daily,
  weekly,
}: {
  daily: DailyPoint[];
  weekly: WeeklyPoint[];
}) {
  return (
    <div className="grid gap-8 lg:grid-cols-2">
      <section>
        <h2 className="mb-2 text-sm font-medium text-white">Daily compliance %</h2>
        <p className="mb-3 text-xs text-[var(--muted)]">
          % of enrolled students with ≥1 completed session (≥60s) started that UTC day.
        </p>
        <div className="h-64 w-full min-w-0">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={daily}>
              <CartesianGrid strokeDasharray="3 3" stroke="#ffffff22" />
              <XAxis dataKey="day" tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <YAxis domain={[0, 100]} tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <Tooltip
                contentStyle={{ background: "#161d27", border: "1px solid #ffffff22" }}
                labelStyle={{ color: "#e7eef7" }}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="pct"
                name="Compliance %"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={false}
                connectNulls
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>
      <section>
        <h2 className="mb-2 text-sm font-medium text-white">Weekly compliance %</h2>
        <p className="mb-3 text-xs text-[var(--muted)]">
          Week starts Monday (UTC). Same compliance definition per week bucket.
        </p>
        <div className="h-64 w-full min-w-0">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={weekly}>
              <CartesianGrid strokeDasharray="3 3" stroke="#ffffff22" />
              <XAxis dataKey="week" tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <YAxis domain={[0, 100]} tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <Tooltip
                contentStyle={{ background: "#161d27", border: "1px solid #ffffff22" }}
                labelStyle={{ color: "#e7eef7" }}
              />
              <Legend />
              <Bar dataKey="pct" name="Compliance %" fill="#22c55e" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
      <section>
        <h2 className="mb-2 text-sm font-medium text-white">Bypass events per week</h2>
        <p className="mb-3 text-xs text-[var(--muted)]">
          Count of bypass_events with event_at in each UTC week (Monday start).
        </p>
        <div className="h-56 w-full min-w-0">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={weekly}>
              <CartesianGrid strokeDasharray="3 3" stroke="#ffffff22" />
              <XAxis dataKey="week" tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <YAxis allowDecimals={false} tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <Tooltip
                contentStyle={{ background: "#161d27", border: "1px solid #ffffff22" }}
                labelStyle={{ color: "#e7eef7" }}
              />
              <Bar dataKey="bypass" name="Bypasses" fill="#f97316" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
      <section className="lg:col-span-2">
        <h2 className="mb-2 text-sm font-medium text-white">Bypass events per day</h2>
        <p className="mb-3 text-xs text-[var(--muted)]">
          Count of bypass_events with event_at in range (UTC day).
        </p>
        <div className="h-56 w-full min-w-0">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={daily}>
              <CartesianGrid strokeDasharray="3 3" stroke="#ffffff22" />
              <XAxis dataKey="day" tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <YAxis allowDecimals={false} tick={{ fill: "#8b9cb3", fontSize: 10 }} />
              <Tooltip
                contentStyle={{ background: "#161d27", border: "1px solid #ffffff22" }}
                labelStyle={{ color: "#e7eef7" }}
              />
              <Bar dataKey="bypass" name="Bypasses" fill="#f97316" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
    </div>
  );
}

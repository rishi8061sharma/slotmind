import { useCallback, useEffect, useRef, useState } from "react";
import { decideSuggestion, loadQueue } from "./api";

const POLL_MS = Number(import.meta.env.VITE_POLL_INTERVAL_MS || 15000);
const TYPE_LABELS = { ROUTINE: "Routine", URGENT: "Urgent", FOLLOW_UP: "Follow-up" };
const TRIGGER_LABELS = { PROVIDER_OFFLINE: "Provider offline", SLOT_CANCELLED: "Slot cancelled", INITIAL: "Manual" };

function formatDate(value) {
  if (!value) return "Time unavailable";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function triggerLabel(reason) { return TRIGGER_LABELS[reason] || "Manual"; }

function App() {
  const [queue, setQueue] = useState({ appointments: [], suggestions: [], slots: [], providers: [] });
  const [initialLoading, setInitialLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [action, setAction] = useState(null);
  const inFlight = useRef(false);
  const requestController = useRef(null);
  const mounted = useRef(true);

  const refresh = useCallback(async (background = false) => {
    if (inFlight.current) return;
    inFlight.current = true;
    requestController.current?.abort();
    const controller = new AbortController();
    requestController.current = controller;
    if (background) setRefreshing(true); else setInitialLoading(true);
    try {
      const data = await loadQueue(controller.signal);
      if (!mounted.current || controller.signal.aborted) return;
      setQueue(data);
      setError("");
    } catch (err) {
      if (err.name !== "AbortError" && mounted.current) setError("We could not refresh the rescheduling queue.");
    } finally {
      if (mounted.current) { setInitialLoading(false); setRefreshing(false); }
      inFlight.current = false;
    }
  }, []);

  useEffect(() => {
    mounted.current = true;
    refresh(false);
    const timer = window.setInterval(() => refresh(true), POLL_MS);
    return () => { mounted.current = false; window.clearInterval(timer); requestController.current?.abort(); };
  }, [refresh]);

  async function handleDecision(suggestion, status) {
    if (!suggestion?.id || action) return;
    setAction({ id: suggestion.id, status });
    try {
      await decideSuggestion(suggestion.id, status);
      if (!mounted.current) return;
      setQueue((current) => ({
        ...current,
        suggestions: current.suggestions.filter((item) => item.id !== suggestion.id),
        appointments: status === "ACCEPTED"
          ? current.appointments.filter((item) => item.id !== suggestion.appointmentId)
          : current.appointments
      }));
      setError("");
    } catch (err) {
      if (!mounted.current) return;
      if (err.status === 409) setError("Another coordinator may have booked this slot. The queue was refreshed; please review this patient again.");
      else setError("The decision could not be saved. Nothing was changed on this screen.");
      await refresh(true);
    } finally { if (mounted.current) setAction(null); }
  }

  const suggestionsByAppointment = Object.fromEntries(queue.suggestions.map((item) => [item.appointmentId, item]));
  const slotById = Object.fromEntries(queue.slots.map((item) => [item.id, item]));
  const providerById = Object.fromEntries(queue.providers.map((item) => [item.id, item]));

  return <div className="app-shell">
    <header className="topbar">
      <div className="brand"><span className="brand-mark">S</span><span>SlotMind</span></div>
      <div className="header-meta"><span className="live-dot" /> Live queue <span className="divider" /> Front desk</div>
    </header>
    <main className="page">
      <section className="page-intro">
        <div><p className="eyebrow">CareBridge clinics</p><h1>Needs rescheduling</h1><p className="subheading">Review proposed changes with patients before confirming a new booking.</p></div>
        <div className="refresh-status" aria-live="polite">{refreshing ? <><span className="spinner small" /> Updating queue</> : `Updates every ${Math.round(POLL_MS / 1000)}s`}</div>
      </section>
      {error && <div className="alert error" role="alert"><strong>Action needed</strong><span>{error}</span><button className="text-button" onClick={() => refresh(true)}>Try again</button></div>}
      {initialLoading ? <div className="loading-panel"><span className="spinner" /><span>Loading affected appointments…</span></div> : queue.appointments.length === 0 ? <div className="empty-panel"><div className="empty-icon">✓</div><h2>All clear for now</h2><p>No appointments are waiting for rescheduling.</p></div> : <div className="queue" aria-label="Appointments needing rescheduling">
        <div className="queue-summary"><strong>{queue.appointments.length} {queue.appointments.length === 1 ? "appointment" : "appointments"}</strong><span>Patient confirmation required</span></div>
        {queue.appointments.map((appointment) => <AppointmentCard key={appointment.id} appointment={appointment} suggestion={suggestionsByAppointment[appointment.id]} slotById={slotById} providerById={providerById} action={action} onDecision={handleDecision} />)}
      </div>}
    </main>
  </div>;
}

function AppointmentCard({ appointment, suggestion, slotById, providerById, action, onDecision }) {
  const originalSlot = slotById[appointment.slotId];
  const suggestedSlot = suggestion && slotById[suggestion.recommendedSlotId];
  const originalProvider = providerById[appointment.providerId];
  const suggestedProvider = providerById[suggestedSlot?.providerId || suggestion?.alternateProviderId];
  const busy = action?.id === suggestion?.id;
  return <article className="appointment-card">
    <div className="card-head"><div><h2>{appointment.patientName}</h2><span className={`type type-${appointment.appointmentType.toLowerCase()}`}>{TYPE_LABELS[appointment.appointmentType] || appointment.appointmentType}</span></div><span className={`trigger trigger-${suggestion?.triggerReason?.toLowerCase() || "initial"}`}>{triggerLabel(suggestion?.triggerReason)}</span></div>
    {!suggestion ? <div className="no-suggestion"><strong>No suitable slot available.</strong><span>This patient remains visible for follow-up.</span></div> : <>
      <div className="comparison">
        <TimeBlock label="Original appointment" slot={originalSlot} provider={originalProvider} />
        <div className="arrow" aria-hidden="true">→</div>
        <TimeBlock label="Suggested appointment" slot={suggestedSlot} provider={suggestedProvider} fallbackId={suggestion.recommendedSlotId} />
      </div>
      <div className="recommendation"><div className="confidence"><span>Recommendation confidence</span><strong>{Math.round(Number(suggestion.confidence || 0) * 100)}%</strong></div><p>{suggestion.reasoning || "A suitable replacement slot is available."}</p></div>
      <div className="decision-row"><button className="button primary" disabled={Boolean(action)} onClick={() => onDecision(suggestion, "ACCEPTED")}>{busy && action.status === "ACCEPTED" && <span className="spinner tiny" />}Accept suggestion</button><button className="button secondary" disabled={Boolean(action)} onClick={() => onDecision(suggestion, "REJECTED")}>{busy && action.status === "REJECTED" && <span className="spinner tiny" />}Reject</button></div>
    </>}
  </article>;
}

function TimeBlock({ label, slot, provider, fallbackId }) {
  return <div className="time-block"><span className="field-label">{label}</span><strong>{formatDate(slot?.startTime)}</strong><span>{provider?.name || (provider ? provider.id : "Provider unavailable")}</span>{!slot && fallbackId && <small>Slot {fallbackId}</small>}</div>;
}

export default App;
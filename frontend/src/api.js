const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/slotmind").replace(/\/$/, "");
async function request(path, options = {}, signal) {
  const response = await fetch(`${API_BASE}${path}`, { ...options, signal, headers: { "Content-Type": "application/json", ...(options.headers || {}) } });
  if (!response.ok) { const error = new Error(`Request failed with status ${response.status}`); error.status=response.status; throw error; }
  return response.status === 204 ? null : response.json();
}
export function loadQueue(signal) {
  return Promise.all([
    request("/appointments?status=RESCHEDULE_PENDING", {}, signal),
    request("/suggestions?status=PENDING", {}, signal),
    request("/slots", {}, signal),
    request("/providers", {}, signal)
  ]).then(([appointments, suggestions, slots, providers]) => ({ appointments, suggestions, slots, providers }));
}
export function decideSuggestion(id, status, signal) { return request(`/suggestions/${id}`, { method:"PATCH", body:JSON.stringify({ status }) }, signal); }
export { API_BASE };
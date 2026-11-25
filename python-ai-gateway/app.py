from datetime import datetime
from decimal import Decimal
from typing import Literal
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="SlotMind AI Scheduling Gateway", version="1.0.0")

class Slot(BaseModel):
    slotId: str
    providerId: str
    startTime: datetime
    durationMinutes: int
    status: Literal["OPEN", "BOOKED", "CANCELLED"]

class Provider(BaseModel):
    providerId: str
    specialty: str
    status: str

class Request(BaseModel):
    appointmentId: str
    appointmentType: str
    providerId: str
    patientName: str
    requiredDurationMinutes: int = 30
    slots: list[Slot]
    providers: list[Provider]
    triggerReason: str

class Response(BaseModel):
    slotId: str
    alternateProviderId: str | None = None
    confidence: Decimal
    reasoning: str

@app.get("/health")
def health():
    return {"status": "UP"}

@app.post("/api/v1/schedule/recommend", response_model=Response)
def recommend(request: Request):
    available_providers = {
        p.providerId: p for p in request.providers if p.status != "OFFLINE"
    }
    candidates = [
        slot for slot in request.slots
        if slot.status == "OPEN"
        and slot.durationMinutes >= request.requiredDurationMinutes
        and slot.providerId in available_providers
    ]
    # A cancelled slot is replaced by a slot from the same provider when possible.
    # An offline provider permits an alternate available provider.
    same_provider_first = request.triggerReason == "SLOT_CANCELLED"
    candidates.sort(key=lambda slot: (
        same_provider_first and slot.providerId != request.providerId,
        slot.startTime,
    ))
    if not candidates:
        raise HTTPException(status_code=503, detail="No eligible slot available")

    selected = candidates[0]
    same_provider = selected.providerId == request.providerId
    if same_provider:
        reasoning = "The nearest suitable slot with the same provider was selected."
    elif request.triggerReason == "PROVIDER_OFFLINE":
        reasoning = "The provider is offline, so the nearest suitable alternate provider was selected."
    else:
        reasoning = "The nearest suitable slot with an available provider was selected."

    return Response(
        slotId=selected.slotId,
        alternateProviderId=None if same_provider else selected.providerId,
        confidence=Decimal("0.90" if same_provider else "0.80"),
        reasoning=reasoning,
    )
import json
import os
from datetime import datetime
from decimal import Decimal
from typing import Literal

import httpx
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

class LlmResponse(BaseModel):
    slotId: str
    alternateProviderId: str | None = None
    confidence: Decimal
    reasoning: str


def eligible_slots(request: Request) -> list[Slot]:
    available_providers = {
        p.providerId: p for p in request.providers if p.status != "OFFLINE"
    }
    candidates = [
        slot for slot in request.slots
        if slot.status == "OPEN"
        and slot.durationMinutes >= request.requiredDurationMinutes
        and slot.providerId in available_providers
    ]
    same_provider_first = request.triggerReason == "SLOT_CANCELLED"
    candidates.sort(key=lambda slot: (
        same_provider_first and slot.providerId != request.providerId,
        slot.startTime,
    ))
    return candidates


def rule_recommendation(request: Request) -> Response:
    candidates = eligible_slots(request)
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


def llm_recommendation(request: Request) -> Response:
    api_key = os.getenv("LITELLM_API_KEY")
    if not api_key:
        return rule_recommendation(request)

    trigger_instruction = (
        "The original provider is offline; alternate providers are allowed."
        if request.triggerReason == "PROVIDER_OFFLINE"
        else "The slot was cancelled; prefer a new slot with the same provider."
        if request.triggerReason == "SLOT_CANCELLED"
        else "This is a manual rescheduling request."
    )
    prompt = {
        "task": "Recommend one replacement clinic appointment.",
        "instruction": trigger_instruction,
        "response_format": "Return JSON only with slotId, alternateProviderId, confidence, reasoning.",
        "appointment": request.model_dump(mode="json"),
    }
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
        "product": os.getenv("LITELLM_PRODUCT", "PC1"),
    }
    payload = {
        "model": os.getenv("LITELLM_MODEL", "gpt-5.6-luna-azure-payg-global"),
        "messages": [{"role": "user", "content": json.dumps(prompt)}],
        "temperature": 0.1,
    }
    try:
        with httpx.Client(timeout=5.0) as client:
            response = client.post(
                os.getenv("LITELLM_URL", "https://litellm-qc.zycus.net/v1/chat/completions"),
                headers=headers,
                json=payload,
            )
            response.raise_for_status()
            content = response.json()["choices"][0]["message"]["content"]
            parsed = LlmResponse.model_validate_json(content)
            return Response(**parsed.model_dump())
    except Exception:
        # The Java strategy also validates and falls back; this keeps the Python
        # gateway useful when the remote LLM is unavailable.
        return rule_recommendation(request)


@app.get("/health")
def health():
    return {"status": "UP", "llmConfigured": bool(os.getenv("LITELLM_API_KEY"))}


@app.post("/api/v1/schedule/recommend", response_model=Response)
def recommend(request: Request):
    return llm_recommendation(request)
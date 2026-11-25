package com.slotmind.dto;

import jakarta.validation.constraints.NotBlank;

public record SuggestionDecisionRequest(@NotBlank String status) { }
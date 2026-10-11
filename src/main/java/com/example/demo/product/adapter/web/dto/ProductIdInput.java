package com.example.demo.product.adapter.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ProductIdInput(@NotNull UUID id) {
}

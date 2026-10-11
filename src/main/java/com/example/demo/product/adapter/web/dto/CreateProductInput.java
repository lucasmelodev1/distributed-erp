package com.example.demo.product.adapter.web.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;
import com.example.demo.product.domain.Product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductInput(
		@NotBlank @Size(max = Product.MAX_NAME_LENGTH) String name,
		@Nullable @Size(max = Product.MAX_SKU_LENGTH) String sku,
		@Nullable @Size(max = Product.MAX_DESCRIPTION_LENGTH) String description,
		@NotNull @DecimalMin("0") @Digits(integer = Product.PRICE_INTEGER_DIGITS, fraction = Product.PRICE_FRACTION_DIGITS) BigDecimal price) {
}

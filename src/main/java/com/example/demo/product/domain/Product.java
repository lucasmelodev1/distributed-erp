package com.example.demo.product.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jspecify.annotations.Nullable;
import com.example.demo.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Product extends BaseEntity {
	public static final int MAX_NAME_LENGTH = 255;
	public static final int MAX_SKU_LENGTH = 100;
	public static final int MAX_DESCRIPTION_LENGTH = 1000;
	public static final int PRICE_INTEGER_DIGITS = 9;
	public static final int PRICE_FRACTION_DIGITS = 10;

	private UUID tenantId;

	private String name;

	private @Nullable String sku;

	private @Nullable String description;

	private BigDecimal price;

	private @Nullable Instant inactivatedAt;

	protected Product() {
	}

	public Product(UUID tenantId, String name, @Nullable String sku, @Nullable String description, BigDecimal price) {
		this.tenantId = tenantId;
		change(name, sku, description, price);
	}

	/** Replaces every editable field; the rules for each field live here. */
	public void change(String name, @Nullable String sku, @Nullable String description, BigDecimal price) {
		this.name = requireName(name);
		this.sku = blankToNull(sku);
		this.description = blankToNull(description);
		this.price = requirePrice(price);
	}

	public void inactivate() {
		if (isActive()) {
			this.inactivatedAt = Instant.now();
		}
	}

	public void reactivate() {
		this.inactivatedAt = null;
	}

	public boolean isActive() {
		return inactivatedAt == null;
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("The product name is required");
		}
		var trimmed = name.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("The product name is too long");
		}
		return trimmed;
	}

	private static BigDecimal requirePrice(BigDecimal price) {
		if (price == null || price.signum() < 0) {
			throw new IllegalArgumentException("The product price must be zero or more");
		}
		var stripped = price.signum() == 0 ? BigDecimal.ZERO : price.stripTrailingZeros();
		var fraction = Math.max(stripped.scale(), 0);
		var integer = stripped.precision() - stripped.scale();
		if (fraction > PRICE_FRACTION_DIGITS || integer > PRICE_INTEGER_DIGITS) {
			throw new IllegalArgumentException("The product price allows at most " + PRICE_INTEGER_DIGITS
					+ " integer digits and " + PRICE_FRACTION_DIGITS + " decimal places");
		}
		return price.setScale(PRICE_FRACTION_DIGITS);
	}

	private static @Nullable String blankToNull(@Nullable String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public String getName() {
		return name;
	}

	public @Nullable String getSku() {
		return sku;
	}

	public @Nullable String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public @Nullable Instant getInactivatedAt() {
		return inactivatedAt;
	}
}

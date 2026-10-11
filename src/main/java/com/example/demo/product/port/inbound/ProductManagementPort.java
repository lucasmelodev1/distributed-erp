package com.example.demo.product.port.inbound;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Window;

import com.example.demo.product.adapter.web.dto.CreateProductInput;
import com.example.demo.product.adapter.web.dto.UpdateProductInput;
import com.example.demo.product.domain.Product;
import com.example.demo.product.domain.ProductStatus;

public interface ProductManagementPort {
	/** Forward keyset page ordered by id (creation time). */
	Window<Product> scrollProducts(@Nullable String query, @Nullable ProductStatus status, int limit,
			@Nullable UUID after, UUID tenantId);

	Product findProductById(UUID id, UUID tenantId);

	Product createProduct(CreateProductInput dto, UUID tenantId);

	Product updateProduct(UpdateProductInput dto, UUID tenantId);

	Product setProductActive(UUID id, boolean active, UUID tenantId);

	void deleteProduct(UUID id, UUID tenantId);
}

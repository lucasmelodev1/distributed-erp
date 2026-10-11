package com.example.demo.product.adapter.web.graphql;

import java.util.Map;
import java.util.UUID;

import com.example.demo.config.graphql.Connection;
import com.example.demo.config.graphql.Cursors;
import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.product.adapter.web.dto.CreateProductInput;
import com.example.demo.product.adapter.web.dto.ProductIdInput;
import com.example.demo.product.adapter.web.dto.UpdateProductInput;
import com.example.demo.product.domain.Product;
import com.example.demo.product.domain.ProductStatus;
import com.example.demo.product.port.inbound.ProductManagementPort;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class ProductDataFetcher {
	private final ProductManagementPort productService;
	private final GraphQlInputValidator validator;

	public ProductDataFetcher(ProductManagementPort productService, GraphQlInputValidator validator) {
		this.productService = productService;
		this.validator = validator;
	}

	@TenantAware(withPermissions = "products:read")
	@DgsQuery
	public Connection<Product> products(@InputArgument Integer first, @InputArgument String after,
			@InputArgument String query, @InputArgument ProductStatus status) {
		var window = productService.scrollProducts(query, status, Cursors.pageSize(first), Cursors.decode(after),
				TenantContext.getTenantId());
		return Cursors.toConnection(window, Product::getId, after);
	}

	@TenantAware(withPermissions = "products:read")
	@DgsQuery
	public Product product(@InputArgument UUID id) {
		return productService.findProductById(id, TenantContext.getTenantId());
	}

	@TenantAware(withPermissions = "products:write")
	@DgsMutation
	public Map<String, Object> createProduct(@InputArgument CreateProductInput input) {
		return Map.of("product", productService.createProduct(validator.validate(input), TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "products:write")
	@DgsMutation
	public Map<String, Object> updateProduct(@InputArgument UpdateProductInput input) {
		return Map.of("product", productService.updateProduct(validator.validate(input), TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "products:write")
	@DgsMutation
	public Map<String, Object> inactivateProduct(@InputArgument ProductIdInput input) {
		validator.validate(input);
		return Map.of("product", productService.setProductActive(input.id(), false, TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "products:write")
	@DgsMutation
	public Map<String, Object> reactivateProduct(@InputArgument ProductIdInput input) {
		validator.validate(input);
		return Map.of("product", productService.setProductActive(input.id(), true, TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "products:delete")
	@DgsMutation
	public Map<String, Object> deleteProduct(@InputArgument ProductIdInput input) {
		validator.validate(input);
		productService.deleteProduct(input.id(), TenantContext.getTenantId());
		return Map.of("success", true);
	}
}

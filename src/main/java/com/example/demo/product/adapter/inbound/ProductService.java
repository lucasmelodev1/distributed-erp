package com.example.demo.product.adapter.inbound;

import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.product.adapter.outbound.persistence.ProductRepository;
import com.example.demo.product.adapter.web.dto.CreateProductInput;
import com.example.demo.product.adapter.web.dto.UpdateProductInput;
import com.example.demo.product.domain.Product;
import com.example.demo.product.domain.ProductSpecification;
import com.example.demo.product.domain.ProductStatus;
import com.example.demo.product.port.inbound.ProductManagementPort;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductService implements ProductManagementPort {
	private static final Logger log = LoggerFactory.getLogger(ProductService.class);
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Window<Product> scrollProducts(@Nullable String query, @Nullable ProductStatus status, int limit,
			@Nullable UUID after, UUID tenantId) {
		var spec = Specification.allOf(
				ProductSpecification.hasTenantId(tenantId),
				ProductSpecification.hasStatus(status),
				ProductSpecification.matches(query));
		ScrollPosition position = after == null ? ScrollPosition.keyset()
				: ScrollPosition.forward(Map.of("id", after));
		return productRepository.findBy(spec, q -> q.sortBy(Sort.by("id")).limit(limit).scroll(position));
	}

	@Override
	@Transactional(readOnly = true)
	public Product findProductById(UUID id, UUID tenantId) {
		return getProduct(id, tenantId);
	}

	@Override
	@Transactional
	public Product createProduct(CreateProductInput dto, UUID tenantId) {
		var product = new Product(tenantId, dto.name(), dto.sku(), dto.description(), dto.price());
		return saveUnique(product, null);
	}

	@Override
	@Transactional
	public Product updateProduct(UpdateProductInput dto, UUID tenantId) {
		var product = getProduct(dto.id(), tenantId);
		product.change(dto.name(), dto.sku(), dto.description(), dto.price());
		return saveUnique(product, product.getId());
	}

	@Override
	@Transactional
	public Product setProductActive(UUID id, boolean active, UUID tenantId) {
		var product = getProduct(id, tenantId);
		if (active) {
			product.reactivate();
		} else {
			product.inactivate();
		}
		log.info("Product={} in tenant={} is now {}", id, tenantId, active ? "active" : "inactive");
		return productRepository.save(product);
	}

	@Override
	@Transactional
	public void deleteProduct(UUID id, UUID tenantId) {
		productRepository.delete(getProduct(id, tenantId));
		log.info("Product={} in tenant={} deleted", id, tenantId);
	}

	private Product getProduct(UUID id, UUID tenantId) {
		return productRepository.findByIdAndTenantId(id, tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Product not found for id: " + id));
	}

	/** Friendly checks among live products; {@link #saveUnique} backs them with the database constraint. */
	private void requireUnique(Product product, @Nullable UUID selfId) {
		var tenantId = product.getTenantId();
		boolean nameTaken = selfId == null
				? productRepository.existsByTenantIdAndName(tenantId, product.getName())
				: productRepository.existsByTenantIdAndNameAndIdNot(tenantId, product.getName(), selfId);
		if (nameTaken) {
			throw new IllegalArgumentException("A product named '" + product.getName() + "' already exists");
		}
		var sku = product.getSku();
		if (sku != null) {
			boolean skuTaken = selfId == null
					? productRepository.existsByTenantIdAndSku(tenantId, sku)
					: productRepository.existsByTenantIdAndSkuAndIdNot(tenantId, sku, selfId);
			if (skuTaken) {
				throw new IllegalArgumentException("The SKU '" + sku + "' is already in use");
			}
		}
	}

	/**
	 * The SKU stays reserved after a soft delete (and concurrent requests can race the checks), so the unique
	 * index is the final word. The checks run inside the try because their queries auto-flush a changed product.
	 */
	private Product saveUnique(Product product, @Nullable UUID selfId) {
		try {
			requireUnique(product, selfId);
			return productRepository.saveAndFlush(product);
		} catch (DataIntegrityViolationException e) {
			log.info("Product rejected for tenant={}: {}", product.getTenantId(), e.getMessage());
			throw new IllegalArgumentException("The SKU '" + product.getSku() + "' is already in use", e);
		}
	}
}

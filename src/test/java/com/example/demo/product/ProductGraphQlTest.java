package com.example.demo.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;

class ProductGraphQlTest extends BaseGraphQlTest {
	private static final String FIELDS = "id name sku description price active inactivatedAt";
	private static final String CREATE = "mutation($input: CreateProductInput!) { createProduct(input: $input) { product { "
			+ FIELDS + " } } }";
	private static final String UPDATE = "mutation($input: UpdateProductInput!) { updateProduct(input: $input) { product { "
			+ FIELDS + " } } }";
	private static final String INACTIVATE = "mutation($input: ProductIdInput!) { inactivateProduct(input: $input) { product { id active inactivatedAt } } }";
	private static final String REACTIVATE = "mutation($input: ProductIdInput!) { reactivateProduct(input: $input) { product { id active inactivatedAt } } }";
	private static final String DELETE = "mutation($input: ProductIdInput!) { deleteProduct(input: $input) { success } }";
	private static final String PRODUCT = "query($id: UUID!) { product(id: $id) { " + FIELDS + " } }";
	private static final String PRODUCTS = """
			query($first: Int, $after: String, $query: String, $status: ProductStatus) {
				products(first: $first, after: $after, query: $query, status: $status) {
					edges { cursor node { id name sku active } }
					pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
				}
			}""";

	private TestUser owner;
	private UUID tenant;
	private UUID otherTenant;
	private TestUser otherOwner;

	@BeforeAll
	void setup() {
		owner = createUser("Product Owner", "gql.product.owner@mail.com");
		otherOwner = createUser("Product Other", "gql.product.other@mail.com");
		tenant = newTenant(owner, "Product Tenant");
		otherTenant = newTenant(otherOwner, "Other Product Tenant");
	}

	private UUID newProduct(UUID tenantId, TestUser user, String name, String sku, String price) {
		var id = new UUID[1];
		graphqlIn(tenantId, user, CREATE, variables("input", variables("name", name, "sku", sku, "price", price)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createProduct.product.id").value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	@Test
	void createUpdateInactivateDelete_fullFlow() {
		var input = variables("name", "  Widget ", "sku", "W-1", "description", "A widget", "price", "12.3456789012");
		var id = new UUID[1];
		graphqlIn(tenant, owner, CREATE, variables("input", input))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createProduct.product.name").isEqualTo("Widget")
				.jsonPath("$.data.createProduct.product.sku").isEqualTo("W-1")
				.jsonPath("$.data.createProduct.product.price").isEqualTo("12.3456789012")
				.jsonPath("$.data.createProduct.product.active").isEqualTo(true)
				.jsonPath("$.data.createProduct.product.id").value(v -> id[0] = UUID.fromString(v.toString()));

		graphqlIn(tenant, owner, PRODUCT, variables("id", id[0]))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.product.price").isEqualTo("12.3456789012");

		graphqlIn(tenant, owner, UPDATE,
				variables("input", variables("id", id[0], "name", "Gadget", "price", "0")))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateProduct.product.name").isEqualTo("Gadget")
				.jsonPath("$.data.updateProduct.product.sku").isEmpty()
				.jsonPath("$.data.updateProduct.product.description").isEmpty()
				.jsonPath("$.data.updateProduct.product.price").isEqualTo("0.0000000000");

		graphqlIn(tenant, owner, INACTIVATE, variables("input", variables("id", id[0])))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inactivateProduct.product.active").isEqualTo(false)
				.jsonPath("$.data.inactivateProduct.product.inactivatedAt").isNotEmpty();
		graphqlIn(tenant, owner, REACTIVATE, variables("input", variables("id", id[0])))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.reactivateProduct.product.active").isEqualTo(true)
				.jsonPath("$.data.reactivateProduct.product.inactivatedAt").isEmpty();

		graphqlIn(tenant, owner, DELETE, variables("input", variables("id", id[0])))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.deleteProduct.success").isEqualTo(true);
		assertGraphQlError(graphqlIn(tenant, owner, PRODUCT, variables("id", id[0])), "NOT_FOUND");
		assertGraphQlError(graphqlIn(tenant, owner, DELETE, variables("input", variables("id", id[0]))), "NOT_FOUND");
	}

	@Test
	void products_pageForwardWithoutGapsOrDuplicates() {
		var t = newTenant(owner, "Paged Product Tenant");
		var created = new ArrayList<String>();
		for (int i = 1; i <= 5; i++) {
			created.add(newProduct(t, owner, "Paged " + i, "P-" + i, "1").toString());
		}
		var pages = collectPages("products", "id",
				after -> graphqlIn(t, owner, PRODUCTS, variables("first", 2, "after", after)));
		assertThat(pages.values()).containsExactlyElementsOf(created);
		assertThat(pages.count()).isEqualTo(3);
	}

	@Test
	void products_filterByNameOrSkuAndStatus() {
		var t = newTenant(owner, "Filter Product Tenant");
		var blue = newProduct(t, owner, "Blue Chair", "CH-100", "10");
		newProduct(t, owner, "Red Table", "TB-200", "20");
		graphqlIn(t, owner, INACTIVATE, variables("input", variables("id", blue))).expectBody()
				.jsonPath("$.errors").doesNotExist();

		graphqlIn(t, owner, PRODUCTS, variables()).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(2);
		graphqlIn(t, owner, PRODUCTS, variables("query", "")).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(2);
		graphqlIn(t, owner, PRODUCTS, variables("query", "chair")).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(1)
				.jsonPath("$.data.products.edges[0].node.name").isEqualTo("Blue Chair");
		graphqlIn(t, owner, PRODUCTS, variables("query", "tb-2")).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(1)
				.jsonPath("$.data.products.edges[0].node.name").isEqualTo("Red Table");
		graphqlIn(t, owner, PRODUCTS, variables("status", "ACTIVE")).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(1)
				.jsonPath("$.data.products.edges[0].node.name").isEqualTo("Red Table");
		graphqlIn(t, owner, PRODUCTS, variables("status", "INACTIVE")).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(1)
				.jsonPath("$.data.products.edges[0].node.name").isEqualTo("Blue Chair");
	}

	@Test
	void duplicateNameOrSku_isBadRequest() {
		var t = newTenant(owner, "Unique Product Tenant");
		var first = newProduct(t, owner, "Unique", "U-1", "1");
		var second = newProduct(t, owner, "Second", "U-2", "1");

		assertGraphQlError(graphqlIn(t, owner, CREATE,
				variables("input", variables("name", "unique", "price", "1"))), "BAD_REQUEST");
		assertGraphQlError(graphqlIn(t, owner, CREATE,
				variables("input", variables("name", "Other", "sku", "u-1", "price", "1"))), "BAD_REQUEST");
		assertGraphQlError(graphqlIn(t, owner, UPDATE,
				variables("input", variables("id", second, "name", "Second", "sku", "U-1", "price", "1"))), "BAD_REQUEST");
		// Keeping its own name and SKU is fine.
		graphqlIn(t, owner, UPDATE, variables("input", variables("id", first, "name", "Unique", "sku", "U-1", "price", "2")))
				.expectBody().jsonPath("$.errors").doesNotExist();
		// The same name and SKU are fine in another tenant.
		newProduct(otherTenant, otherOwner, "Unique", "U-1", "1");
		// Several products may have no SKU.
		graphqlIn(t, owner, CREATE, variables("input", variables("name", "No sku 1", "price", "1"))).expectBody()
				.jsonPath("$.errors").doesNotExist();
		graphqlIn(t, owner, CREATE, variables("input", variables("name", "No sku 2", "price", "1"))).expectBody()
				.jsonPath("$.errors").doesNotExist();
	}

	@Test
	void deletedProduct_keepsItsSkuButFreesItsName() {
		var t = newTenant(owner, "Deleted Product Tenant");
		var id = newProduct(t, owner, "Gone", "G-1", "1");
		graphqlIn(t, owner, DELETE, variables("input", variables("id", id))).expectBody()
				.jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphqlIn(t, owner, CREATE,
				variables("input", variables("name", "Fresh", "sku", "G-1", "price", "1"))), "BAD_REQUEST");
		graphqlIn(t, owner, CREATE, variables("input", variables("name", "Gone", "sku", "G-2", "price", "1")))
				.expectBody().jsonPath("$.errors").doesNotExist();
		graphqlIn(t, owner, PRODUCTS, variables("first", 100)).expectBody()
				.jsonPath("$.data.products.edges.length()").isEqualTo(1);
	}

	@Test
	void invalidInput_isBadRequest() {
		for (var bad : List.of(
				variables("name", "", "price", "1"),
				variables("name", "x".repeat(256), "price", "1"),
				variables("name", "Neg", "price", "-1"),
				variables("name", "Decimals", "price", "1.12345678901"),
				variables("name", "Digits", "price", "1234567890"),
				variables("name", "Sku", "sku", "s".repeat(101), "price", "1"),
				variables("name", "Desc", "description", "d".repeat(1001), "price", "1"))) {
			assertGraphQlError(graphqlIn(tenant, owner, CREATE, variables("input", bad)), "BAD_REQUEST");
		}
		// The largest legal price round-trips.
		var id = newProduct(tenant, owner, "Max price", null, "999999999.9999999999");
		graphqlIn(tenant, owner, PRODUCT, variables("id", id)).expectBody()
				.jsonPath("$.data.product.price").isEqualTo("999999999.9999999999");
		// Name of exactly 255 characters is legal.
		newProduct(tenant, owner, "n".repeat(255), null, "1");
	}

	@Test
	void productsOfOtherTenants_areInvisible() {
		var id = newProduct(tenant, owner, "Private", "PRIV-1", "5");

		assertGraphQlError(graphqlIn(otherTenant, otherOwner, PRODUCT, variables("id", id)), "NOT_FOUND");
		assertGraphQlError(graphqlIn(otherTenant, otherOwner, INACTIVATE, variables("input", variables("id", id))),
				"NOT_FOUND");
		assertGraphQlError(graphqlIn(otherTenant, otherOwner, DELETE, variables("input", variables("id", id))),
				"NOT_FOUND");
		assertGraphQlError(graphqlIn(otherTenant, otherOwner, UPDATE,
				variables("input", variables("id", id, "name", "Hijack", "price", "1"))), "NOT_FOUND");
		graphqlIn(otherTenant, otherOwner, PRODUCTS, variables("first", 100)).expectBody()
				.jsonPath("$.data.products.edges[?(@.node.id == '%s')]".formatted(id)).isEmpty();
		// A tenant the caller does not belong to is rejected.
		assertGraphQlError(graphqlIn(otherTenant, owner, PRODUCTS, variables()), "FORBIDDEN");
	}

	@Test
	void permissions_areEnforcedPerOperation() {
		var t = newTenant(owner, "Product Permission Tenant");
		var id = newProduct(t, owner, "Guarded", "GD-1", "1");
		var reader = createUser("Product Reader", "gql.product.reader@mail.com");
		var writer = createUser("Product Writer", "gql.product.writer@mail.com");
		var nobody = createUser("Product Nobody", "gql.product.nobody@mail.com");
		addMember(t, reader, newRole(t, owner, "Product readers", List.of("products:read")));
		addMember(t, writer, newRole(t, owner, "Product writers", List.of("products:read", "products:write")));
		addMember(t, nobody, newRole(t, owner, "Nothing", List.of()));

		graphqlIn(t, reader, PRODUCT, variables("id", id)).expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(t, reader, CREATE, variables("input", variables("name", "X", "price", "1"))),
				"FORBIDDEN");
		assertGraphQlError(graphqlIn(t, reader, INACTIVATE, variables("input", variables("id", id))), "FORBIDDEN");
		assertGraphQlError(graphqlIn(t, nobody, PRODUCT, variables("id", id)), "FORBIDDEN");
		assertGraphQlError(graphqlIn(t, nobody, PRODUCTS, variables()), "FORBIDDEN");

		graphqlIn(t, writer, CREATE, variables("input", variables("name", "By writer", "price", "1"))).expectBody()
				.jsonPath("$.errors").doesNotExist();
		graphqlIn(t, writer, INACTIVATE, variables("input", variables("id", id))).expectBody()
				.jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(t, writer, DELETE, variables("input", variables("id", id))), "FORBIDDEN");
	}

	@Test
	void adminRole_holdsTheProductPermissions() {
		graphqlIn(tenant, owner, "query { permissions { code level } }", variables()).expectBody()
				.jsonPath("$.data.permissions[?(@.code == 'products:read')].level")
				.value(v -> assertThat(v.toString()).contains("VIEW"))
				.jsonPath("$.data.permissions[?(@.code == 'products:write')].level")
				.value(v -> assertThat(v.toString()).contains("EDIT"))
				.jsonPath("$.data.permissions[?(@.code == 'products:delete')].level")
				.value(v -> assertThat(v.toString()).contains("SENSITIVE"));
	}

	@Test
	void unauthenticated_isRejected() {
		assertGraphQlError(graphql(PRODUCTS, variables()), "UNAUTHORIZED");
	}
}

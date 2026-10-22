package com.stokivo.stockflow;

import com.jayway.jsonpath.JsonPath;
import com.stokivo.stockflow.category.CategoryRepository;
import com.stokivo.stockflow.inventory.StockMovementRepository;
import com.stokivo.stockflow.product.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductInventoryApiTest extends EmbeddedPostgresSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ProductRepository products;

	@Autowired
	private StockMovementRepository movements;

	@BeforeEach
	void cleanDatabase() {
		movements.deleteAll();
		products.deleteAll();
		categories.deleteAll();
	}

	@Test
	void createsProductsAndKeepsThePriceChargedSeparateFromLaterEdits() throws Exception {
		long categoryId = createCategory("Drinks");

		long productId = createProduct(categoryId, "TEA-1", "Rooibos", "24.50", "10.00", 4, 5);

		mockMvc.perform(get("/api/products/{id}", productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sku").value("TEA-1"))
				.andExpect(jsonPath("$.sellingPrice").value(24.50))
				.andExpect(jsonPath("$.stockQuantity").value(4))
				.andExpect(jsonPath("$.lowStock").value(true))
				.andExpect(jsonPath("$.categoryName").value("Drinks"));

		mockMvc.perform(put("/api/products/{id}", productId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sku": "TEA-1",
								  "name": "Rooibos tin",
								  "categoryId": %d,
								  "sellingPrice": 26.00,
								  "costPrice": 10.00,
								  "minimumStockLevel": 5
								}
								""".formatted(categoryId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Rooibos tin"))
				.andExpect(jsonPath("$.sellingPrice").value(26.00))
				.andExpect(jsonPath("$.stockQuantity").value(4));
	}

	@Test
	void rejectsDuplicateSkusMissingFieldsAndNegativeStock() throws Exception {
		long categoryId = createCategory("Pantry");
		createProduct(categoryId, "RICE-1", "Rice 2kg", "40.00", "25.00", 2, 1);

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson(categoryId, "rice-1", "Rice bag", "40.00", "25.00", 0, 1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("A product with this SKU already exists."));

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson(categoryId, "OIL-1", "Oil", "0.00", "5.00", 1, 1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.sellingPrice").exists());

		long oilId = createProduct(categoryId, "OIL-1", "Oil", "18.00", "9.00", 2, 1);
		mockMvc.perform(post("/api/inventory/adjustments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"productId": %d, "quantity": 3, "direction": "DECREASE"}
								""".formatted(oilId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.reason").value("A reason is required."));

		mockMvc.perform(post("/api/inventory/adjustments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": %d,
								  "quantity": 3,
								  "direction": "DECREASE",
								  "reason": "Damaged"
								}
								""".formatted(oilId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Stock cannot become negative."));

		mockMvc.perform(get("/api/products/{id}", oilId))
				.andExpect(jsonPath("$.stockQuantity").value(2));
	}

	@Test
	void receiptsAndAdjustmentsLeaveAMovementHistory() throws Exception {
		long categoryId = createCategory("Household");
		long productId = createProduct(categoryId, "SOAP-1", "Soap", "12.00", "6.00", 0, 2);

		mockMvc.perform(post("/api/inventory/receipts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"productId": %d, "quantity": 6, "note": "Supplier delivery"}
								""".formatted(productId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("RECEIPT"))
				.andExpect(jsonPath("$.quantityChange").value(6))
				.andExpect(jsonPath("$.stockAfter").value(6));

		mockMvc.perform(post("/api/inventory/adjustments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": %d,
								  "quantity": 1,
								  "direction": "DECREASE",
								  "reason": "Display sample"
								}
								""".formatted(productId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("ADJUSTMENT"))
				.andExpect(jsonPath("$.quantityChange").value(-1))
				.andExpect(jsonPath("$.stockAfter").value(5));

		mockMvc.perform(get("/api/inventory/movements").param("productId", String.valueOf(productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].reason").value("Display sample"))
				.andExpect(jsonPath("$.content[1].reason").value("Supplier delivery"));
	}

	@Test
	void searchFilterAndDeactivationKeepTheProductRecord() throws Exception {
		long drinks = createCategory("Cold drinks");
		long snacks = createCategory("Snacks");
		long colaId = createProduct(drinks, "COLA-1", "Cola", "15.00", "8.00", 1, 3);
		createProduct(snacks, "CHIP-1", "Chips", "10.00", "4.00", 20, 5);

		mockMvc.perform(get("/api/products").param("search", "cola"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].sku").value("COLA-1"));

		mockMvc.perform(get("/api/products").param("lowStock", "true"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].sku").value("COLA-1"));

		mockMvc.perform(patch("/api/products/{id}/status", colaId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(get("/api/products").param("active", "true"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].sku").value("CHIP-1"));

		mockMvc.perform(get("/api/products/{id}", colaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Cola"));
	}

	private long createCategory(String name) throws Exception {
		String body = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private long createProduct(
			long categoryId,
			String sku,
			String name,
			String sellingPrice,
			String costPrice,
			int initialQuantity,
			int minimumStockLevel) throws Exception {
		String body = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(productJson(categoryId, sku, name, sellingPrice, costPrice, initialQuantity, minimumStockLevel)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private String productJson(
			long categoryId,
			String sku,
			String name,
			String sellingPrice,
			String costPrice,
			int initialQuantity,
			int minimumStockLevel) {
		return """
				{
				  "sku": "%s",
				  "name": "%s",
				  "categoryId": %d,
				  "sellingPrice": %s,
				  "costPrice": %s,
				  "initialQuantity": %d,
				  "minimumStockLevel": %d
				}
				""".formatted(sku, name, categoryId, sellingPrice, costPrice, initialQuantity, minimumStockLevel);
	}
}

package com.stokivo.stockflow;

import com.jayway.jsonpath.JsonPath;
import com.stokivo.stockflow.category.CategoryRepository;
import com.stokivo.stockflow.inventory.StockMovementRepository;
import com.stokivo.stockflow.product.ProductRepository;
import com.stokivo.stockflow.sale.SaleItemRepository;
import com.stokivo.stockflow.sale.SaleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMINISTRATOR")
class SaleApiTest extends EmbeddedPostgresSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ProductRepository products;

	@Autowired
	private StockMovementRepository movements;

	@Autowired
	private SaleItemRepository saleItems;

	@Autowired
	private SaleRepository sales;

	@BeforeEach
	void cleanDatabase() {
		movements.deleteAll();
		saleItems.deleteAll();
		sales.deleteAll();
		products.deleteAll();
		categories.deleteAll();
	}

	@Test
	void checkoutSavesThePriceChargedAndDeductsStockOnce() throws Exception {
		long categoryId = createCategory("Groceries");
		long teaId = createProduct(categoryId, "TEA-1", "Rooibos", "24.50", "10.00", 4, 1);
		long breadId = createProduct(categoryId, "BREAD-1", "Bread", "10.00", "4.00", 3, 1);

		String saleBody = mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentMethod": "CASH",
								  "total": 1,
								  "items": [
								    {"productId": %d, "quantity": 2, "unitPrice": 0.01},
								    {"productId": %d, "quantity": 1, "unitPrice": 0.01}
								  ]
								}
								""".formatted(teaId, breadId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saleNumber", matchesPattern("S-\\d{6}")))
				.andExpect(jsonPath("$.paymentMethod").value("CASH"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.subtotal").value(59.00))
				.andExpect(jsonPath("$.total").value(59.00))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].sku").value("TEA-1"))
				.andExpect(jsonPath("$.items[0].unitPrice").value(24.50))
				.andExpect(jsonPath("$.items[0].lineTotal").value(49.00))
				.andExpect(jsonPath("$.items[1].sku").value("BREAD-1"))
				.andExpect(jsonPath("$.items[1].unitPrice").value(10.00))
				.andExpect(jsonPath("$.items[1].lineTotal").value(10.00))
				.andReturn()
				.getResponse()
				.getContentAsString();

		long saleId = ((Number) JsonPath.read(saleBody, "$.id")).longValue();
		String saleNumber = JsonPath.read(saleBody, "$.saleNumber");

		mockMvc.perform(get("/api/products/{id}", teaId))
				.andExpect(jsonPath("$.stockQuantity").value(2));
		mockMvc.perform(get("/api/products/{id}", breadId))
				.andExpect(jsonPath("$.stockQuantity").value(2));
		mockMvc.perform(get("/api/inventory/movements").param("productId", String.valueOf(teaId)))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].type").value("SALE"))
				.andExpect(jsonPath("$.content[0].quantityChange").value(-2))
				.andExpect(jsonPath("$.content[0].stockAfter").value(2))
				.andExpect(jsonPath("$.content[0].saleId").value(saleId))
				.andExpect(jsonPath("$.content[0].reason").value("Sale " + saleNumber));

		mockMvc.perform(put("/api/products/{id}", teaId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sku": "TEA-1",
								  "name": "Rooibos tin",
								  "categoryId": %d,
								  "sellingPrice": 30.00,
								  "costPrice": 10.00,
								  "minimumStockLevel": 1
								}
								""".formatted(categoryId)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/sales/{id}", saleId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(59.00))
				.andExpect(jsonPath("$.items[0].productName").value("Rooibos"))
				.andExpect(jsonPath("$.items[0].unitPrice").value(24.50))
				.andExpect(jsonPath("$.items[1].productName").value("Bread"));

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentMethod": "CARD",
								  "items": [{"productId": %d, "quantity": 1}]
								}
								""".formatted(teaId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(30.00))
				.andExpect(jsonPath("$.items[0].productName").value("Rooibos tin"))
				.andExpect(jsonPath("$.items[0].unitPrice").value(30.00));

		mockMvc.perform(get("/api/sales"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].paymentMethod").value("CARD"))
				.andExpect(jsonPath("$.content[0].total").value(30.00))
				.andExpect(jsonPath("$.content[1].saleNumber").value(saleNumber));

		mockMvc.perform(patch("/api/products/{id}/status", breadId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\": false}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/sales/{id}", saleId))
				.andExpect(jsonPath("$.items[1].productName").value("Bread"))
				.andExpect(jsonPath("$.items[1].unitPrice").value(10.00));

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentMethod": "EFT",
								  "items": [{"productId": %d, "quantity": 1}]
								}
								""".formatted(breadId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Bread is not available for sale."));

		mockMvc.perform(get("/api/products/{id}", breadId))
				.andExpect(jsonPath("$.stockQuantity").value(2));
		mockMvc.perform(get("/api/products/{id}", teaId))
				.andExpect(jsonPath("$.stockQuantity").value(1));
	}

	@Test
	void insufficientStockRollsBackTheWholeSale() throws Exception {
		long categoryId = createCategory("Pantry");
		long oilId = createProduct(categoryId, "OIL-1", "Oil", "18.00", "9.00", 2, 1);
		long riceId = createProduct(categoryId, "RICE-1", "Rice", "40.00", "20.00", 5, 1);

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentMethod": "CASH",
								  "items": [
								    {"productId": %d, "quantity": 1},
								    {"productId": %d, "quantity": 3}
								  ]
								}
								""".formatted(riceId, oilId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Not enough stock for Oil."));

		mockMvc.perform(get("/api/sales"))
				.andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/products/{id}", oilId))
				.andExpect(jsonPath("$.stockQuantity").value(2));
		mockMvc.perform(get("/api/products/{id}", riceId))
				.andExpect(jsonPath("$.stockQuantity").value(5));
		mockMvc.perform(get("/api/inventory/movements").param("productId", String.valueOf(oilId)))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].type").value("RECEIPT"));
	}

	@Test
	void mergesDuplicateLinesAndRejectsAnInvalidCart() throws Exception {
		long categoryId = createCategory("Household");
		long soapId = createProduct(categoryId, "SOAP-1", "Soap", "12.00", "6.00", 8, 1);

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paymentMethod": "CASH",
								  "items": [
								    {"productId": %d, "quantity": 1},
								    {"productId": %d, "quantity": 2}
								  ]
								}
								""".formatted(soapId, soapId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].quantity").value(3))
				.andExpect(jsonPath("$.total").value(36.00));

		mockMvc.perform(get("/api/products/{id}", soapId))
				.andExpect(jsonPath("$.stockQuantity").value(5));
		mockMvc.perform(get("/api/inventory/movements").param("productId", String.valueOf(soapId)))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].type").value("SALE"))
				.andExpect(jsonPath("$.content[0].quantityChange").value(-3));

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"paymentMethod\": \"CASH\", \"items\": []}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.items").value("Add at least one product."));

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod": "CASH", "items": [{"productId": %d, "quantity": 0}]}
								""".formatted(soapId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors['items[0].quantity']").value("Quantity must be at least 1."));

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod": "BITCOIN", "items": [{"productId": %d, "quantity": 1}]}
								""".formatted(soapId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("The request body is invalid."));

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod": "CASH", "items": [{"productId": 999999, "quantity": 1}]}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Product not found."));

		mockMvc.perform(get("/api/sales/{id}", 999999))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Sale not found."));

		mockMvc.perform(get("/api/products/{id}", soapId))
				.andExpect(jsonPath("$.stockQuantity").value(5));
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
						.content("""
								{
								  "sku": "%s",
								  "name": "%s",
								  "categoryId": %d,
								  "sellingPrice": %s,
								  "costPrice": %s,
								  "initialQuantity": %d,
								  "minimumStockLevel": %d
								}
								""".formatted(sku, name, categoryId, sellingPrice, costPrice, initialQuantity, minimumStockLevel)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}
}

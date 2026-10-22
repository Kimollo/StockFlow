package com.stokivo.stockflow;

import com.jayway.jsonpath.JsonPath;
import com.stokivo.stockflow.category.CategoryRepository;
import com.stokivo.stockflow.inventory.StockMovementRepository;
import com.stokivo.stockflow.product.ProductRepository;
import com.stokivo.stockflow.sale.SaleItemRepository;
import com.stokivo.stockflow.sale.SaleRepository;
import com.stokivo.stockflow.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest extends EmbeddedPostgresSupport {

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

	@Autowired
	private UserRepository users;

	@BeforeEach
	void cleanDatabase() {
		movements.deleteAll();
		saleItems.deleteAll();
		sales.deleteAll();
		products.deleteAll();
		categories.deleteAll();
		users.deleteAll(users.findAll().stream()
				.filter(user -> !"admin".equalsIgnoreCase(user.getUsername()))
				.toList());
	}

	@Test
	void loginProtectsTheApiAndKeepsStaffToTheirOwnSales() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Sign in to continue."));

		String adminToken = login("admin", "stockflow");
		long categoryId = createCategory(adminToken);
		long productId = createProduct(adminToken, categoryId);

		String staffBody = mockMvc.perform(post("/api/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Asha",
								  "username": "asha",
								  "password": "counter-1",
								  "role": "STAFF"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("STAFF"))
				.andReturn().getResponse().getContentAsString();
		int staffId = JsonPath.read(staffBody, "$.id");
		String staffToken = login("asha", "counter-1");

		mockMvc.perform(get("/api/products/" + productId)
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sellingPrice").value(10.00))
				.andExpect(jsonPath("$.costPrice").value(nullValue()));
		mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sku": "NOPE",
								  "name": "Blocked",
								  "categoryId": %d,
								  "sellingPrice": "5.00",
								  "costPrice": "1.00",
								  "initialQuantity": 1,
								  "minimumStockLevel": 0
								}
								""".formatted(categoryId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have access to that."));
		mockMvc.perform(post("/api/inventory/adjustments")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"productId": %d, "quantity": 1, "direction": "DECREASE", "reason": "Nope"}
								""".formatted(productId)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/dashboard/summary")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/users")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isForbidden());

		String staffSale = checkout(staffToken, productId);
		int staffSaleId = JsonPath.read(staffSale, "$.id");
		String adminSale = checkout(adminToken, productId);
		int adminSaleId = JsonPath.read(adminSale, "$.id");

		mockMvc.perform(get("/api/sales")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(staffSaleId))
				.andExpect(jsonPath("$.content[0].cashierName").value("Asha"));
		int staffItemId = JsonPath.read(staffSale, "$.items[0].id");
		mockMvc.perform(post("/api/sales/" + staffSaleId + "/returns")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items": [{"saleItemId": %d, "quantity": 1}]}
								""".formatted(staffItemId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].quantity").value(1))
				.andExpect(jsonPath("$.items[0].returnedQuantity").value(1));
		int adminItemId = JsonPath.read(adminSale, "$.items[0].id");
		mockMvc.perform(post("/api/sales/" + adminSaleId + "/returns")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items": [{"saleItemId": %d, "quantity": 1}]}
								""".formatted(adminItemId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You can only return your own sales."));

		mockMvc.perform(get("/api/sales/" + adminSaleId)
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You can only view your own sales."));
		mockMvc.perform(get("/api/sales")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(patch("/api/users/" + staffId + "/status")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"active\": false}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username": "asha", "password": "counter-1"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("This account is deactivated."));
		mockMvc.perform(get("/api/sales")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("This account is deactivated."));
	}

	@Test
	void administratorCanReplaceAPasswordAndTheOldSignInStops() throws Exception {
		String adminToken = login("admin", "stockflow");
		String staffBody = mockMvc.perform(post("/api/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Asha",
								  "username": "asha",
								  "password": "counter-1",
								  "role": "STAFF"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		int staffId = JsonPath.read(staffBody, "$.id");
		String staffToken = login("asha", "counter-1");

		mockMvc.perform(patch("/api/users/" + staffId + "/password")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\": \"counter-2\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/users/" + staffId + "/password")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\": \"counter-2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("asha"));

		mockMvc.perform(get("/api/auth/me")
						.header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Sign in again."));
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username": "asha", "password": "counter-1"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("The username or password is incorrect."));
		login("asha", "counter-2");
	}

	private String login(String username, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username": "%s", "password": "%s"}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private long createCategory(String token) throws Exception {
		String body = mockMvc.perform(post("/api/categories")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\": \"Counter\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private long createProduct(String token, long categoryId) throws Exception {
		String body = mockMvc.perform(post("/api/products")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sku": "MUG-1",
								  "name": "Mug",
								  "categoryId": %d,
								  "sellingPrice": "10.00",
								  "costPrice": "4.00",
								  "initialQuantity": 5,
								  "minimumStockLevel": 1
								}
								""".formatted(categoryId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private String checkout(String token, long productId) throws Exception {
		return mockMvc.perform(post("/api/sales")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod": "CASH", "items": [{"productId": %d, "quantity": 1}]}
								""".formatted(productId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}
}

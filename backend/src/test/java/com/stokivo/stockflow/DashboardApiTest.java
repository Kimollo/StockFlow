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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMINISTRATOR")
class DashboardApiTest extends EmbeddedPostgresSupport {

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
	void summaryTrendAndExportMatchSavedSales() throws Exception {
		long categoryId = createCategory("Counter");
		long teaId = createProduct(categoryId, "TEA-1", "Rooibos", "10.00", "4.00", 4, 5);
		createProduct(categoryId, "CUP-1", "Cup", "6.00", "2.00", 12, 2);

		String saleBody = mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod": "CASH", "items": [{"productId": %d, "quantity": 2}]}
								""".formatted(teaId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String saleNumber = JsonPath.read(saleBody, "$.saleNumber");

		mockMvc.perform(post("/api/sales")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod": "CASH", "items": [{"productId": %d, "quantity": 9}]}
								""".formatted(teaId)))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/api/dashboard/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.todayTotal").value(20.00))
				.andExpect(jsonPath("$.todayCount").value(1))
				.andExpect(jsonPath("$.periodTotal").value(20.00))
				.andExpect(jsonPath("$.periodCount").value(1))
				.andExpect(jsonPath("$.activeProducts").value(2))
				.andExpect(jsonPath("$.lowStockProducts").value(1));

		mockMvc.perform(get("/api/dashboard/sales-trend"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.points.length()").value(1))
				.andExpect(jsonPath("$.points[0].total").value(20.00))
				.andExpect(jsonPath("$.points[0].count").value(1));

		mockMvc.perform(get("/api/dashboard/top-products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.products.length()").value(1))
				.andExpect(jsonPath("$.products[0].sku").value("TEA-1"))
				.andExpect(jsonPath("$.products[0].quantitySold").value(2))
				.andExpect(jsonPath("$.products[0].revenue").value(20.00));

		mockMvc.perform(get("/api/dashboard/sales.csv"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("stockflow-sales-")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(saleNumber)))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Rooibos")));

		mockMvc.perform(get("/api/dashboard/summary").param("from", "2026-01-02").param("to", "2026-01-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("The end date is before the start date."));
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

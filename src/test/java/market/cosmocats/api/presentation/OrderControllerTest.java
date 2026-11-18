package market.cosmocats.api.presentation;

import market.cosmocats.api.application.dto.OrderItemResp;
import market.cosmocats.api.application.dto.OrderReq;
import market.cosmocats.api.application.dto.OrderResp;
import market.cosmocats.api.application.exception.ConflictException;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.service.OrderService;
import market.cosmocats.api.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String ORDERS_URL = "/api/orders";
    private static final UUID ORDER_NUMBER = UUID.fromString("0a662d45-d0c3-46bd-8eba-18eca4e07cf9");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_shouldReturn201WithOrder() throws Exception {
        when(orderService.createOrder(any(OrderReq.class))).thenReturn(resp(OrderStatus.NEW));

        mockMvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER.toString()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-18T21:05:45.804603Z"))
                .andExpect(jsonPath("$.items[0].productName").value("Cosmic Milk"))
                .andExpect(jsonPath("$.totalAmount").value(91.00));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "no items      | {}                                         | Order must contain at least one item",
            "empty items   | {\"items\":[]}                             | Order must contain at least one item",
            "no product id | {\"items\":[{\"quantity\":1}]}             | Field 'items[0].productId' Product ID is required",
            "no quantity   | {\"items\":[{\"productId\":1}]}            | Field 'items[0].quantity' Quantity is required",
            "zero quantity | {\"items\":[{\"productId\":1,\"quantity\":0}]}    | Quantity must be at least 1",
            "huge quantity | {\"items\":[{\"productId\":1,\"quantity\":1001}]} | Quantity must be at most 1000"
    })
    void createOrder_shouldReturn400ForInvalidBody(String scenario, String body, String expectedError) throws Exception {
        mockMvc.perform(post(ORDERS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value(containsString(expectedError)));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_shouldReturn404WhenProductsMissing() throws Exception {
        when(orderService.createOrder(any(OrderReq.class)))
                .thenThrow(new ResourceNotFoundException("Products not found: [99]"));

        mockMvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":99,\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Products not found: [99]"));
    }

    @Test
    void getOrders_shouldReturn200WithList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(resp(OrderStatus.NEW)));

        mockMvc.perform(get(ORDERS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value(ORDER_NUMBER.toString()));
    }

    @Test
    void getOrder_shouldReturn200ByOrderNumber() throws Exception {
        when(orderService.getOrder(ORDER_NUMBER)).thenReturn(resp(OrderStatus.NEW));

        mockMvc.perform(get(ORDERS_URL + "/" + ORDER_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER.toString()));
    }

    @Test
    void getOrder_shouldReturn400ForMalformedOrderNumber() throws Exception {
        mockMvc.perform(get(ORDERS_URL + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrder_shouldReturn404WhenMissing() throws Exception {
        when(orderService.getOrder(ORDER_NUMBER))
                .thenThrow(new ResourceNotFoundException("Order " + ORDER_NUMBER + " not found"));

        mockMvc.perform(get(ORDERS_URL + "/" + ORDER_NUMBER))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_shouldReturn200WithNewStatus() throws Exception {
        when(orderService.updateStatus(ORDER_NUMBER, OrderStatus.PAID)).thenReturn(resp(OrderStatus.PAID));

        mockMvc.perform(patch(ORDERS_URL + "/" + ORDER_NUMBER + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void updateStatus_shouldReturn400ForMissingStatus() throws Exception {
        mockMvc.perform(patch(ORDERS_URL + "/" + ORDER_NUMBER + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Status is required")));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatus_shouldReturn400ForUnknownStatus() throws Exception {
        mockMvc.perform(patch(ORDERS_URL + "/" + ORDER_NUMBER + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatus_shouldReturn409ForFinishedOrder() throws Exception {
        when(orderService.updateStatus(ORDER_NUMBER, OrderStatus.PAID))
                .thenThrow(new ConflictException("Order is already CANCELLED and cannot change status"));

        mockMvc.perform(patch(ORDERS_URL + "/" + ORDER_NUMBER + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Order is already CANCELLED and cannot change status"));
    }

    @Test
    void deleteOrder_shouldReturn204() throws Exception {
        mockMvc.perform(delete(ORDERS_URL + "/" + ORDER_NUMBER))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(ORDER_NUMBER);
    }

    @Test
    void deleteOrder_shouldReturn404WhenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Order " + ORDER_NUMBER + " not found"))
                .when(orderService).deleteOrder(ORDER_NUMBER);

        mockMvc.perform(delete(ORDERS_URL + "/" + ORDER_NUMBER))
                .andExpect(status().isNotFound());
    }

    private static OrderResp resp(OrderStatus status) {
        OrderItemResp item = new OrderItemResp();
        item.productId = 1L;
        item.productName = "Cosmic Milk";
        item.quantity = 2;
        item.unitPrice = new BigDecimal("45.50");
        item.lineTotal = new BigDecimal("91.00");

        OrderResp resp = new OrderResp();
        resp.orderNumber = ORDER_NUMBER;
        resp.status = status;
        resp.createdAt = Instant.parse("2026-11-18T21:05:45.804603Z");
        resp.items = List.of(item);
        resp.totalAmount = new BigDecimal("91.00");
        return resp;
    }
}

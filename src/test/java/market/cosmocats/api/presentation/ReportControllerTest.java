package market.cosmocats.api.presentation;

import market.cosmocats.api.application.dto.ProductSalesReportResp;
import market.cosmocats.api.application.exception.BadRequestException;
import market.cosmocats.api.application.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(ReportControllerTest.FixedClockConfig.class)
class ReportControllerTest {

    private static final String URL = "/api/reports/top-products";
    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void shouldDefaultToLast30DaysAndLimit10() throws Exception {
        when(reportService.getTopSellingProducts(any(), any(), anyInt())).thenReturn(List.of(row()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(301))
                .andExpect(jsonPath("$[0].productName").value("Cosmic Milk"))
                .andExpect(jsonPath("$[0].categoryTitle").value("Space Food"))
                .andExpect(jsonPath("$[0].totalQuantity").value(6))
                .andExpect(jsonPath("$[0].totalRevenue").value(273.00));

        verify(reportService).getTopSellingProducts(Instant.parse("2026-10-19T12:00:00Z"), NOW, 10);
    }

    @Test
    void shouldPassExplicitWindowAndLimit() throws Exception {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        when(reportService.getTopSellingProducts(from, to, 3)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("from", from.toString()).param("to", to.toString()).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(reportService).getTopSellingProducts(from, to, 3);
    }

    @Test
    void shouldCountDefaultFromBackwardsFromExplicitTo() throws Exception {
        Instant to = Instant.parse("2026-03-31T00:00:00Z");

        mockMvc.perform(get(URL).param("to", to.toString()))
                .andExpect(status().isOk());

        verify(reportService).getTopSellingProducts(Instant.parse("2026-03-01T00:00:00Z"), to, 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101", "abc"})
    void shouldReturn400ForInvalidLimit(String limit) throws Exception {
        mockMvc.perform(get(URL).param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));

        verifyNoInteractions(reportService);
    }

    @Test
    void shouldReturn400ForMalformedDate() throws Exception {
        mockMvc.perform(get(URL).param("from", "yesterday"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }

    @Test
    void shouldReturn400WhenServiceRejectsWindow() throws Exception {
        when(reportService.getTopSellingProducts(any(), any(), anyInt()))
                .thenThrow(new BadRequestException("'from' must be before 'to'"));

        mockMvc.perform(get(URL).param("from", "2027-01-01T00:00:00Z").param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'from' must be before 'to'"))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/bad-request"));
    }

    private static ProductSalesReportResp row() {
        ProductSalesReportResp r = new ProductSalesReportResp();
        r.productId = 301L;
        r.productName = "Cosmic Milk";
        r.categoryTitle = "Space Food";
        r.totalQuantity = 6;
        r.totalRevenue = new BigDecimal("273.00");
        return r;
    }
}

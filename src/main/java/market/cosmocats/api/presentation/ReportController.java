package market.cosmocats.api.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import market.cosmocats.api.application.dto.ProductSalesReportResp;
import market.cosmocats.api.application.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    static final Duration DEFAULT_PERIOD = Duration.ofDays(30);

    private final ReportService reportService;
    private final Clock clock;

    public ReportController(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ProductSalesReportResp>> getTopProducts(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        Instant end = to != null ? to : clock.instant();
        Instant start = from != null ? from : end.minus(DEFAULT_PERIOD);
        return ResponseEntity.ok(reportService.getTopSellingProducts(start, end, limit));
    }
}

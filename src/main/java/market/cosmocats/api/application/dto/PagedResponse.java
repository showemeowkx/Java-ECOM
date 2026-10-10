package market.cosmocats.api.application.dto;

import java.util.List;

public class PagedResponse {
    public List<ProductResp> data;
    public int page;
    public int limit;
    public String totalElements;
}

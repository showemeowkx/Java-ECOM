package market.cosmocats.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryReq {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 chars")
    public String title;
}

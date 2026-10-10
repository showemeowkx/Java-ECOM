package market.cosmocats.api.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import market.cosmocats.api.application.validation.CosmicWordCheck;

public class ProductReq {
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 chars")
    @CosmicWordCheck
    public String name;
    
    public String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    public Float price;
    
    @NotNull(message = "Category ID is required")
    public String categoryId;
}

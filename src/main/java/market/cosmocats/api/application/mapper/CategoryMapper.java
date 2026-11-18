package market.cosmocats.api.application.mapper;

import market.cosmocats.api.application.dto.CategoryReq;
import market.cosmocats.api.application.dto.CategoryResp;
import market.cosmocats.api.domain.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryReq req) {
        return new Category(req.title.trim());
    }

    public void apply(Category category, CategoryReq req) {
        category.setTitle(req.title.trim());
    }

    public CategoryResp toDto(Category category) {
        CategoryResp resp = new CategoryResp();
        resp.id = category.getId();
        resp.title = category.getTitle();
        return resp;
    }
}

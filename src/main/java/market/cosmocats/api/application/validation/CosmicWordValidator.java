package market.cosmocats.api.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {

    private static final List<String> COSMIC_WORDS = Arrays.asList(
            "star", "galaxy", "comet", "space", "космічне", "космічний", "антигравітаційні"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty values
        }
        
        String lowerCaseValue = value.toLowerCase();
        return COSMIC_WORDS.stream().anyMatch(lowerCaseValue::contains);
    }
}

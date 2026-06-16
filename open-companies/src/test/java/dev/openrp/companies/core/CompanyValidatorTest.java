package dev.openrp.companies.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.companies.config.CompaniesSettings;

public class CompanyValidatorTest {

    private static CompanyValidator validator() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("companies.creation.name.min-length", 3);
        config.set("companies.creation.name.max-length", 16);
        config.set("companies.creation.name.allowed-regex", "^[A-Za-z0-9 _-]+$");
        config.set("companies.creation.name.reserved", List.of("admin", "staff", "chamber"));
        config.set("companies.creation.allowed-types", List.of("food", "retail", "generic"));
        CompaniesSettings settings = new CompaniesSettings();
        settings.load(config);
        return new CompanyValidator(settings);
    }

    @Test
    public void acceptsAValidName() {
        assertTrue(validator().validateName("Red Spot Foods").success());
    }

    @Test
    public void rejectsBlankAndShortAndLongNames() {
        CompanyValidator validator = validator();
        assertEquals("validation.name_required", validator.validateName("  ").messageKey());
        assertEquals("validation.name_too_short", validator.validateName("ab").messageKey());
        assertEquals("validation.name_too_long",
                validator.validateName("ThisNameIsWayTooLong").messageKey());
    }

    @Test
    public void rejectsInvalidCharsAndReservedNames() {
        CompanyValidator validator = validator();
        assertEquals("validation.name_invalid_chars", validator.validateName("Bad@Name").messageKey());
        assertEquals("validation.name_reserved", validator.validateName("Admin").messageKey());
        assertEquals("validation.name_reserved", validator.validateName("chamber").messageKey());
    }

    @Test
    public void validatesTypeAgainstAllowList() {
        CompanyValidator validator = validator();
        assertTrue(validator.validateType("food").success());
        assertTrue(validator.validateType("FOOD").success());
        assertFalse(validator.validateType("banking").success());
        assertEquals("validation.type_not_allowed", validator.validateType("banking").messageKey());
    }

    @Test
    public void slugifyProducesStableIds() {
        assertEquals("red-spot-foods", CompanyValidator.slugify("Red Spot Foods"));
        assertEquals("a-b", CompanyValidator.slugify("  A__B "));
        assertEquals("acme", CompanyValidator.slugify("Acme!!!"));
        assertEquals("caf", CompanyValidator.slugify("Café"));
        assertEquals("", CompanyValidator.slugify("@@@"));
        assertEquals("", CompanyValidator.slugify(null));
    }
}

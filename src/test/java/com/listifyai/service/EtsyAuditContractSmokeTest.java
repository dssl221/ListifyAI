package com.listifyai.service;

import com.listifyai.dto.EtsyAuditDtos.AuditResultDTO;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EtsyAuditContractSmokeTest {

    private final EtsyAuditValidator validator = new EtsyAuditValidator();

    @Test
    void auditPromptContainsWeightedScoringAndStrictSchemaRules() throws Exception {
        Field promptField = EtsyAuditClaudeService.class.getDeclaredField("SYSTEM_PROMPT");
        promptField.setAccessible(true);
        String prompt = (String) promptField.get(null);

        assertTrue(prompt.contains("Score 0-100 using these weighted factors:"));
        assertTrue(prompt.contains("Title quality (20 pts): keyword-first, 120+ chars, buyer-facing language"));
        assertTrue(prompt.contains("Tag quality (20 pts): 13 tags, specific phrases, under 3 generic tags"));
        assertTrue(prompt.contains("Buyer clarity (15 pts): shopper can picture the product immediately"));
        assertTrue(prompt.contains("Search specificity (15 pts): long-tail phrases, niche terms present"));
        assertTrue(prompt.contains("Readability (10 pts): short sentences, no jargon, scannable structure"));
        assertTrue(prompt.contains("Gifting intent (10 pts): occasion words, gift framing present"));
        assertTrue(prompt.contains("Personalization signals (10 pts): custom options mentioned, clear who it's for"));
        assertTrue(prompt.contains("scoreExplanation must name the 2 lowest-scoring factors specifically."));
        assertTrue(prompt.contains("\"optimizedTags\": [\"tag1\", \"tag2\", \"tag3\", \"tag4\", \"tag5\", \"tag6\", \"tag7\", \"tag8\", \"tag9\", \"tag10\", \"tag11\", \"tag12\", \"tag13\"]"));
        assertTrue(prompt.contains("Output strict JSON only. No markdown. No commentary outside JSON."));
    }

    @Test
    void sampleAuditShapePassesValidator() {
        AuditResultDTO sampleAudit = new AuditResultDTO(
                54,
                "The 2 weakest areas are tag quality and personalization signals. The tags are mostly broad words, and the listing does not mention custom options.",
                "This listing has a clear product, but it feels too thin for shoppers comparing similar rings. The title, tags, and description need more specific language.",
                insight("Many tags are single broad words.", "Use phrases that combine product, material, style, and gift use."),
                insight("The title is very short.", "Lead with handmade copper ring, then add style and gift details."),
                insight("A shopper can understand the item, but not enough about its look.", "Describe the warm copper material and handmade finish."),
                insight("The listing does not show custom options or who it is good for.", "Only add personalization details if the seller offers them."),
                insight("The tags mention gift, but the title and description do not explain the gift angle.", "Add gentle gift framing for handmade jewelry shoppers."),
                insight("The description is easy to read, but thin.", "Use short paragraphs with more helpful detail."),
                List.of(
                        "Replace broad tags like nice and cool with specific copper ring phrases.",
                        "Expand the title so shoppers understand the material, style, and gift angle.",
                        "Rewrite the description with clearer details about the ring's look and everyday use."
                ),
                "Handmade Copper Ring, Minimalist Artisan Jewelry, Warm Everyday Ring, Thoughtful Gift, Ships Fast",
                List.of(
                        "handmade ring",
                        "copper ring",
                        "artisan ring",
                        "minimalist ring",
                        "everyday ring",
                        "copper jewelry",
                        "ring gift",
                        "jewelry gift",
                        "warm tone ring",
                        "made by hand",
                        "simple copper ring",
                        "unique copper ring",
                        "copper accessory"
                ),
                """
                This handmade copper ring brings a warm, simple finish to everyday jewelry. Made by hand with copper, it has an artisan feel that works well as a small personal treat or a thoughtful jewelry gift.
                
                The design is easy to wear and simple to style, making it a good choice for shoppers who like handmade pieces with a natural, crafted look. Its copper tone adds warmth without feeling too flashy, so it can fit casual outfits, workday looks, or a dressed-up weekend style.
                
                Each ring is made by hand using quality materials, giving it a personal feel that mass-produced jewelry often misses. It is a lovely option for someone who enjoys handmade accessories, copper jewelry, or meaningful pieces from independent makers.
                
                Ships fast and ready to enjoy.
                """
        );

        EtsyAuditValidator.ValidationReport report = validator.validateFull(sampleAudit, sampleListing());

        assertTrue(report.passed(), () -> "Expected sample audit to pass, failed: " + report.failedSections());
    }

    @Test
    void scoreExplanationMustNameTwoWeightedFactors() {
        AuditResultDTO weakExplanation = validAuditWithExplanation(
                "The listing needs more detail and clearer wording."
        );

        EtsyAuditValidator.ValidationReport report = validator.validateFull(weakExplanation, sampleListing());

        assertFalse(report.passed());
        assertTrue(report.failedSections().contains("summary"));
    }

    private AuditResultDTO validAuditWithExplanation(String scoreExplanation) {
        return new AuditResultDTO(
                80,
                scoreExplanation,
                "The listing is understandable and gives shoppers a good starting point. It still needs more specifics.",
                insight("Specific tags help.", "Keep tags focused."),
                insight("Title is clear.", "Keep the strongest phrase first."),
                insight("Buyer can picture it.", "Add use details."),
                insight("Personalization is clear.", "Mention only supported options."),
                insight("Gift framing is present.", "Add occasion words."),
                insight("Readable and short.", "Keep it scannable."),
                List.of("Fix one.", "Fix two.", "Fix three."),
                "Handmade Copper Ring, Minimalist Artisan Jewelry, Warm Everyday Ring, Thoughtful Gift",
                List.of("copper ring", "artisan ring", "minimalist ring", "everyday ring", "ring gift", "jewelry gift", "warm tone ring", "made by hand", "simple ring", "unique ring", "copper jewelry", "handmade ring", "gift for her"),
                "This handmade copper ring is warm, simple, and easy to wear. It is made by hand from copper and works as an everyday jewelry piece or a thoughtful gift. The style is clean and approachable for shoppers who like handmade accessories with a natural look. It can be worn with casual outfits or saved for a special occasion. The wording stays clear and practical so buyers can quickly understand what they are getting and why it may be a good fit."
        );
    }

    private SectionInsightDTO insight(String message, String fix) {
        return new SectionInsightDTO("needs_work", message, fix);
    }

    private CompactEtsyListing sampleListing() {
        return new CompactEtsyListing(
                "1234567890",
                "https://www.etsy.com/listing/1234567890",
                "Handmade Ring",
                "This is a handmade copper ring. It is very pretty and made by hand. You will love it. Good quality materials used. Ships fast.",
                List.of("ring", "handmade", "copper", "jewelry", "gift", "pretty", "nice", "lovely", "cool", "trendy", "fashion", "style", "accessories"),
                "Jewelry > Rings",
                "i_did",
                "2020_2024",
                "false",
                "2800",
                "USD",
                Map.of("views", 47, "numFavorers", 3, "quantity", 10, "hasVariations", false, "state", "active")
        );
    }
}

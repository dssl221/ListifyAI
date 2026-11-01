package com.listifyai.security;

import com.listifyai.entity.AnalyzedListing;
import com.listifyai.entity.ListingAnalysis;
import com.listifyai.entity.User;
import com.listifyai.repository.AnalyzedListingRepository;
import com.listifyai.repository.ListingAnalysisRepository;
import com.listifyai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalyzedListingRepository analyzedListingRepository;

    @Autowired
    private ListingAnalysisRepository listingAnalysisRepository;

    private User adminUser;
    private User normalUser;
    private User anotherUser;
    private ListingAnalysis anotherUserAudit;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("admin");
        adminUser.setSupabaseId(UUID.randomUUID().toString());
        adminUser = userRepository.save(adminUser);

        normalUser = new User();
        normalUser.setEmail("normal@example.com");
        normalUser.setRole("USER");
        normalUser.setSupabaseId(UUID.randomUUID().toString());
        normalUser = userRepository.save(normalUser);

        anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setRole("USER");
        anotherUser.setSupabaseId(UUID.randomUUID().toString());
        anotherUser = userRepository.save(anotherUser);

        AnalyzedListing listing = new AnalyzedListing();
        listing.setUser(anotherUser);
        listing.setEtsyListingId("12345");
        listing.setListingUrl("https://etsy.com");
        listing.setTitle("Test");
        listing.setListingSnapshot("{}");
        listing.setCacheHit(false);
        analyzedListingRepository.save(listing);

        anotherUserAudit = new ListingAnalysis();
        anotherUserAudit.setAnalyzedListing(listing);
        anotherUserAudit.setUser(anotherUser);
        anotherUserAudit.setScore(100);
        anotherUserAudit.setAuditJson("{}");
        anotherUserAudit.setValidationJson("{}");
        anotherUserAudit.setPromptVersion("v1");
        listingAnalysisRepository.save(anotherUserAudit);
    }

    @AfterEach
    void tearDown() {
        listingAnalysisRepository.deleteAll();
        analyzedListingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Value("${supabase.jwt.secret}")
    private String jwtSecret;

    private String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getSupabaseId())
                .claim("email", user.getEmail())
                .claim("role", "authenticated")
                .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(SupabaseJwtFilter.buildSigningKey(jwtSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        String token = generateToken(normalUser);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoints() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanSuspendUser() throws Exception {
        String token = generateToken(adminUser);

        mockMvc.perform(post("/api/admin/users/" + normalUser.getId() + "/suspend")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}

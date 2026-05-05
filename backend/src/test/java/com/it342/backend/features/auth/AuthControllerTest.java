package com.it342.backend.features.auth;

import com.it342.backend.features.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.findByEmailIgnoreCase("test@tradeoff.com")
                .ifPresent(userRepository::delete);
    }

    @Test
    void register_success() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"fullName":"Test User","displayName":"testuser1","email":"test@tradeoff.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_duplicateEmail_fails() throws Exception {
        String body = """
            {"fullName":"Test User","displayName":"testuser2","email":"dupe@tradeoff.com","password":"pass1234"}
            """;
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        userRepository.findByEmailIgnoreCase("dupe@tradeoff.com").ifPresent(userRepository::delete);
    }

    @Test
    void register_shortUsername_fails() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"fullName":"Test User","displayName":"short","email":"short@tradeoff.com","password":"pass1234"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_success() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Login User","displayName":"loginuser1","email":"login@tradeoff.com","password":"pass1234"}
                    """));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"login@tradeoff.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionToken").isNotEmpty());

        userRepository.findByEmailIgnoreCase("login@tradeoff.com").ifPresent(userRepository::delete);
    }

    @Test
    void login_wrongPassword_fails() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Wrong Pass","displayName":"wrongpass1","email":"wrongpass@tradeoff.com","password":"correct"}
                    """));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"wrongpass@tradeoff.com","password":"incorrect"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        userRepository.findByEmailIgnoreCase("wrongpass@tradeoff.com").ifPresent(userRepository::delete);
    }

    @Test
    void adminExists_returnsBool() throws Exception {
        mvc.perform(get("/api/auth/admin-exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isBoolean());
    }
}

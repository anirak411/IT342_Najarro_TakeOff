package com.it342.backend.features.item;

import com.it342.backend.features.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void getItems_returnsArray() throws Exception {
        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchItems_noParams_returnsArray() throws Exception {
        mvc.perform(get("/api/items/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchItems_withQuery_returnsArray() throws Exception {
        mvc.perform(get("/api/items/search").param("q", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

}

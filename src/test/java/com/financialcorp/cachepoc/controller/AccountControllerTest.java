package com.financialcorp.cachepoc.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUpdateAndDeleteAccount() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "ACC-1001",
                                  "ownerName": "Alex Rivera",
                                  "organizationName": "FinancialCorp",
                                  "balance": 250.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationName", is("FinancialCorp")))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String id = body.get("id").asText();

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is("ACC-1001")));

        mockMvc.perform(put("/api/accounts/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "ACC-1001",
                                  "ownerName": "Alex Rivera",
                                  "organizationName": "FinancialCorp",
                                  "balance": 400.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(400.0)));

        mockMvc.perform(delete("/api/accounts/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void databaseBalanceUpdateRefreshesCache() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "ACC-2002",
                                  "ownerName": "Jordan Lee",
                                  "organizationName": "FinancialCorp",
                                  "balance": 100.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(100.0)));

        mockMvc.perform(patch("/api/accounts/" + id + "/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 775.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(775.5)));

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(775.5)));
    }
}

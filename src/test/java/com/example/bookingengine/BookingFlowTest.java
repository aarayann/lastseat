package com.example.bookingengine;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.example.bookingengine.repository.BookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private ObjectMapper objectMapper;

    private final LocalDate day = LocalDate.now().plusDays(2);

    @BeforeEach
    void startWithNoBookings() {
        bookings.deleteAll();
    }

    private static RequestPostProcessor asEmployee(String username) {
        return httpBasic(username, "password");
    }

    private static RequestPostProcessor asAdmin() {
        return httpBasic("admin", "admin123");
    }

    private String booking(long employeeId, String direction, String time) {
        return """
                {"employeeId": %d, "date": "%s", "direction": "%s", "requestedTime": "%s",
                 "pickup": "Home", "drop": "Office Tower B"}
                """.formatted(employeeId, day, direction, time);
    }

    @Test
    void goodExampleIsAllowed() throws Exception {
        mvc.perform(post("/api/bookings").with(asEmployee("aarav"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(1, "LOGIN", "09:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("ALLOW"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void badExampleIsDeniedForNightSafetyAndExplained() throws Exception {
        mvc.perform(post("/api/bookings/evaluate").with(asEmployee("meera"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(3, "LOGOUT", "23:30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENY"))
                .andExpect(jsonPath("$.reasons[0].code").value("NIGHT_SAFETY"))
                .andExpect(jsonPath("$.trace[2].outcome").value("FAIL"))
                .andExpect(jsonPath("$.trace[3].outcome").value("SKIPPED"));
    }

    @Test
    void secondIdenticalBookingIsADuplicate() throws Exception {
        mvc.perform(post("/api/bookings").with(asEmployee("aarav"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(1, "LOGIN", "09:00")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/bookings").with(asEmployee("aarav"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(1, "LOGIN", "09:00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons[0].code").value("DUPLICATE"));
    }

    @Test
    void cancellingFreesTheSeatForTheNextPerson() throws Exception {
        String kavyaResponse = mvc.perform(post("/api/bookings").with(asEmployee("kavya"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(5, "LOGIN", "09:00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/bookings").with(asEmployee("ishaan"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(6, "LOGIN", "09:00")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/bookings").with(asEmployee("rohan"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(4, "LOGIN", "22:00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons[0].code").value("QUOTA_EXCEEDED"));

        long kavyaBookingId = objectMapper.readTree(kavyaResponse).get("bookingId").asLong();
        mvc.perform(post("/api/bookings/" + kavyaBookingId + "/cancel").with(asEmployee("kavya")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(post("/api/bookings").with(asEmployee("rohan"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(4, "LOGIN", "22:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void ruleChangeTakesEffectWithoutRestartAndApproverCanConfirm() throws Exception {
        mvc.perform(put("/api/admin/rules/3").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"onFail\": \"NEEDS_APPROVAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onFail").value("NEEDS_APPROVAL"));
        try {
            String response = mvc.perform(post("/api/bookings").with(asEmployee("meera"))
                            .contentType(MediaType.APPLICATION_JSON).content(booking(3, "LOGOUT", "23:30")))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.decision").value("NEEDS_APPROVAL"))
                    .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                    .andReturn().getResponse().getContentAsString();

            long bookingId = objectMapper.readTree(response).get("bookingId").asLong();
            mvc.perform(post("/api/approvals/" + bookingId + "/approve").with(httpBasic("approver", "approver123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        } finally {
            mvc.perform(put("/api/admin/rules/3").with(asAdmin())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"onFail\": \"DENY\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void badRuleParamsAreRefused() throws Exception {
        mvc.perform(put("/api/admin/rules/2").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"params\": {\"minutesBefore\": -5}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RULE_PARAMS"));
    }

    @Test
    void employeeCannotBookForSomeoneElse() throws Exception {
        mvc.perform(post("/api/bookings").with(asEmployee("priya"))
                        .contentType(MediaType.APPLICATION_JSON).content(booking(1, "LOGIN", "09:00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void employeeCannotChangeRules() throws Exception {
        mvc.perform(put("/api/admin/rules/2").with(asEmployee("priya"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/bookings/evaluate").with(asEmployee("aarav"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void unknownEmployeeIsNotFound() throws Exception {
        mvc.perform(post("/api/bookings/evaluate").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(booking(999, "LOGIN", "09:00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void requestsWithoutLoginAreRejected() throws Exception {
        mvc.perform(post("/api/bookings/evaluate")
                        .contentType(MediaType.APPLICATION_JSON).content(booking(1, "LOGIN", "09:00")))
                .andExpect(status().isUnauthorized());
    }
}

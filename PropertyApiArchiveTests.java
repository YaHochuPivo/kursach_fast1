package com.example.project2;

import com.example.project2.model.AppUser;
import com.example.project2.model.Property;
import com.example.project2.model.PropertyStatus;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PropertyApiArchiveTests {

    @Autowired MockMvc mvc;
    @Autowired PropertyRepository propertyRepository;
    @Autowired AppUserRepository userRepository;

    private Property property;

    @BeforeEach
    void setup(){
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        AppUser admin = new AppUser();
        admin.setEmail("admin@realestate.com");
        admin.setFirstName("Admin");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        AppUser owner = new AppUser();
        owner.setEmail("owner@example.com");
        owner.setFirstName("Owner");
        owner.setRole(UserRole.USER);
        userRepository.save(owner);

        Property p = new Property();
        p.setUser(owner);
        p.setType("квартира");
        p.setCategory("продажа");
        p.setAddress("Тестовый адрес, 1");
        p.setCity("Москва");
        p.setArea(45.0);
        p.setPrice(10000000.0);
        p.setStatus(PropertyStatus.active);
        property = propertyRepository.save(p);
    }

    @Test
    @WithMockUser(username = "admin@realestate.com", roles = {"ADMIN"})
    void adminCanArchiveAndUnarchive() throws Exception {
        // archive
        mvc.perform(post("/v1/api/properties/" + property.getId() + "/archive")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        Property archived = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(PropertyStatus.archived);

        // unarchive
        mvc.perform(post("/v1/api/properties/" + property.getId() + "/unarchive")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        Property active = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(active.getStatus()).isEqualTo(PropertyStatus.active);
    }
}

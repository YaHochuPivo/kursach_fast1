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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PropertyCsvTests {

    @Autowired MockMvc mvc;
    @Autowired PropertyRepository propertyRepository;
    @Autowired AppUserRepository userRepository;

    private AppUser owner;

    @BeforeEach
    void setup(){
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        owner = new AppUser();
        owner.setEmail("owner@example.com");
        owner.setFirstName("Owner");
        owner.setRole(UserRole.USER);
        userRepository.save(owner);

        Property p = new Property();
        p.setUser(owner);
        p.setType("квартира");
        p.setCategory("продажа");
        p.setAddress("Тестовый адрес, 2");
        p.setCity("Москва");
        p.setArea(35.0);
        p.setPrice(8000000.0);
        p.setStatus(PropertyStatus.active);
        propertyRepository.save(p);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = {"USER"})
    void exportCsvOfCurrentUser() throws Exception {
        mvc.perform(get("/v1/api/properties/csv/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = {"USER"})
    void importCsvCreatesProperties() throws Exception {
        String csv = "type,category,address,city,area,price\n"+
                "квартира,продажа,Импортный адрес 1,Санкт-Петербург,40,7000000\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "props.csv", "text/csv", csv.getBytes()
        );
        mvc.perform(multipart("/v1/api/properties/csv/import").file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        assertThat(propertyRepository.findAll().size()).isGreaterThanOrEqualTo(2);
    }
}

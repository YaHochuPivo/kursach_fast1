package com.example.project2.controller;

import com.example.project2.model.AppUser;
import com.example.project2.model.Property;
import com.example.project2.model.PropertyStatus;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.PropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/v1/api/properties/csv")
public class PropertyCsvController {

    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;

    public PropertyCsvController(PropertyRepository propertyRepository, AppUserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    private Optional<AppUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return Optional.empty();
        return userRepository.findByEmail(auth.getName());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportMyProperties() {
        Optional<AppUser> userOpt = currentUser();
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
        AppUser user = userOpt.get();
        List<Property> list = propertyRepository.findByUserId(user.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("address,city,district,type,category,area,rooms,floor,floorsTotal,price,imageUrls,description\n");
        for (Property p : list) {
            sb.append(csv(p.getAddress())).append(',')
              .append(csv(p.getCity())).append(',')
              .append(csv(p.getDistrict())).append(',')
              .append(csv(p.getType())).append(',')
              .append(csv(p.getCategory())).append(',')
              .append(p.getArea() != null ? p.getArea() : "").append(',')
              .append(p.getRooms() != null ? p.getRooms() : "").append(',')
              .append(p.getFloor() != null ? p.getFloor() : "").append(',')
              .append(p.getFloorsTotal() != null ? p.getFloorsTotal() : "").append(',')
              .append(p.getPrice() != null ? p.getPrice() : "").append(',')
              .append(csv(p.getImageUrls())).append(',')
              .append(csv(p.getDescription()))
              .append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my_properties.csv")
                .contentType(new MediaType("text","csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private String csv(String v) {
        if (!StringUtils.hasText(v)) return "";
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"")) {
            return '"' + s + '"';
        }
        return s;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> importMyProperties(@RequestPart("file") MultipartFile file) {
        try {
            Optional<AppUser> userOpt = currentUser();
            if (userOpt.isEmpty()) return ResponseEntity.status(401).body(Map.of("error","unauthorized"));
            AppUser user = userOpt.get();
            List<Map<String, Object>> errors = new ArrayList<>();
            int created = 0;

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lines = content.split("\r?\n");
            if (lines.length <= 1) return ResponseEntity.ok(Map.of("created",0, "errors", List.of()));
            String[] header = lines[0].split(",");
            Map<String, Integer> idx = new HashMap<>();
            for (int i=0;i<header.length;i++) idx.put(header[i].trim().toLowerCase(Locale.ROOT), i);

            for (int r = 1; r < lines.length; r++) {
                String line = lines[r].trim();
                if (line.isEmpty()) continue;
                String[] parts = splitCsv(line, header.length);
                try {
                    Property p = new Property();
                    p.setUser(user);
                    if (user.getRole() == UserRole.REALTOR) p.setRealtor(user);
                    p.setCreatedDate(LocalDateTime.now());
                    p.setStatus(PropertyStatus.active);
                    p.setPromoted(Boolean.FALSE);

                    p.setAddress(getS(parts, idx, "address"));
                    p.setCity(getS(parts, idx, "city"));
                    p.setDistrict(getS(parts, idx, "district"));
                    p.setType(getS(parts, idx, "type"));
                    p.setCategory(getS(parts, idx, "category"));
                    p.setArea(getD(parts, idx, "area"));
                    p.setRooms(getI(parts, idx, "rooms"));
                    p.setFloor(getI(parts, idx, "floor"));
                    p.setFloorsTotal(getI(parts, idx, "floorsTotal"));
                    p.setPrice(getD(parts, idx, "price"));
                    p.setImageUrls(getS(parts, idx, "imageUrls"));
                    p.setDescription(getS(parts, idx, "description"));

                    // простая валидация
                    if (!StringUtils.hasText(p.getAddress()) || !StringUtils.hasText(p.getCity()) || p.getArea()==null || p.getPrice()==null) {
                        throw new IllegalArgumentException("address, city, area, price are required");
                    }

                    propertyRepository.save(p);
                    created++;
                } catch (Exception ex) {
                    errors.add(Map.of("line", r+1, "message", ex.getMessage()));
                }
            }
            return ResponseEntity.ok(Map.of("created", created, "errors", errors));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String[] splitCsv(String line, int minCols) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i=0;i<line.length();i++){
            char c = line.charAt(i);
            if (quoted) {
                if (c=='"'){
                    if (i+1<line.length() && line.charAt(i+1)=='"') { cur.append('"'); i++; }
                    else { quoted = false; }
                } else cur.append(c);
            } else {
                if (c==',') { cols.add(cur.toString()); cur.setLength(0); }
                else if (c=='"') { quoted = true; }
                else cur.append(c);
            }
        }
        cols.add(cur.toString());
        while (cols.size()<minCols) cols.add("");
        return cols.toArray(new String[0]);
    }

    private String getS(String[] parts, Map<String,Integer> idx, String name){
        Integer i = idx.get(name.toLowerCase(Locale.ROOT));
        return (i!=null && i<parts.length) ? parts[i].trim() : null;
    }
    private Float getD(String[] parts, Map<String,Integer> idx, String name){
        String s = getS(parts, idx, name);
        if (!StringUtils.hasText(s)) return null;
        // normalize decimal separator and parse as Float to match entity fields
        String norm = s.replace(',', '.');
        return Float.valueOf(norm);
        }
    private Integer getI(String[] parts, Map<String,Integer> idx, String name){
        String s = getS(parts, idx, name);
        if (!StringUtils.hasText(s)) return null;
        return Integer.valueOf(s);
    }
}

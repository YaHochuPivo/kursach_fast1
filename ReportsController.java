package com.example.project2.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    @GetMapping
    public String reportsPage() {
        return "reports/index";
    }
}

@RestController
@RequestMapping("/v1/api/reports")
class ReportsApiController {

    @PersistenceContext
    private EntityManager em;

    private LocalDateTime[] resolveRange(LocalDate from, LocalDate to) {
        LocalDateTime start;
        LocalDateTime end;
        if (from == null && to == null) {
            end = LocalDateTime.now();
            start = end.minusDays(90);
        } else {
            start = from != null ? from.atStartOfDay() : LocalDate.of(1970,1,1).atStartOfDay();
            end = to != null ? to.atTime(LocalTime.MAX) : LocalDateTime.now();
        }
        return new LocalDateTime[]{start, end};
    }

    @GetMapping("/properties/by-city")
    public Map<String, Object> byCity(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDateTime[] range = resolveRange(from, to);
        Query q = em.createQuery(
                "select coalesce(p.city,'—') as city, count(p.id) as cnt, avg(p.price) as avgPrice " +
                        "from Property p where p.createdDate between :from and :to group by coalesce(p.city,'—') order by cnt desc");
        q.setParameter("from", range[0]);
        q.setParameter("to", range[1]);
        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("city", r[0]);
            m.put("count", ((Number) Optional.ofNullable(r[1]).orElse(0)).longValue());
            m.put("avgPrice", r[2] != null ? ((Number) r[2]).doubleValue() : 0d);
            out.add(m);
        }
        Map<String, Object> ret = new HashMap<>();
        ret.put("rows", out);
        ret.put("from", range[0].toString());
        ret.put("to", range[1].toString());
        return ret;
    }

    @GetMapping(value = "/properties/by-city.csv", produces = "text/csv")
    public ResponseEntity<byte[]> byCityCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Map<String, Object> data = byCity(from, to);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        StringBuilder sb = new StringBuilder();
        sb.append("city,count,avgPrice\n");
        for (Map<String, Object> m : rows) {
            sb.append(m.get("city")).append(',').append(m.get("count")).append(',').append(m.get("avgPrice")).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=by_city.csv")
                .contentType(new MediaType("text","csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    @GetMapping("/properties/by-type")
    public Map<String, Object> byType(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDateTime[] range = resolveRange(from, to);
        Query q = em.createQuery(
                "select coalesce(p.type,'—') as type, count(p.id) as cnt, avg(p.price) as avgPrice " +
                        "from Property p where p.createdDate between :from and :to group by coalesce(p.type,'—') order by cnt desc");
        q.setParameter("from", range[0]);
        q.setParameter("to", range[1]);
        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", r[0]);
            m.put("count", ((Number) Optional.ofNullable(r[1]).orElse(0)).longValue());
            m.put("avgPrice", r[2] != null ? ((Number) r[2]).doubleValue() : 0d);
            out.add(m);
        }
        Map<String, Object> ret = new HashMap<>();
        ret.put("rows", out);
        ret.put("from", range[0].toString());
        ret.put("to", range[1].toString());
        return ret;
    }

    @GetMapping(value = "/properties/by-type.csv", produces = "text/csv")
    public ResponseEntity<byte[]> byTypeCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Map<String, Object> data = byType(from, to);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        StringBuilder sb = new StringBuilder();
        sb.append("type,count,avgPrice\n");
        for (Map<String, Object> m : rows) {
            sb.append(m.get("type")).append(',').append(m.get("count")).append(',').append(m.get("avgPrice")).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=by_type.csv")
                .contentType(new MediaType("text","csv", StandardCharsets.UTF_8))
                .body(bytes);
    }
}

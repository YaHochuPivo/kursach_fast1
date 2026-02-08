package com.example.project2.controller;

import com.example.project2.model.*;
import com.example.project2.repository.DealRepository;
import com.example.project2.repository.MortgageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/mortgage")
public class MortgageApiController {

    private final DealRepository dealRepository;
    private final MortgageRepository mortgageRepository;

    public MortgageApiController(DealRepository dealRepository, MortgageRepository mortgageRepository) {
        this.dealRepository = dealRepository;
        this.mortgageRepository = mortgageRepository;
    }

    private static boolean hasPassport(AppUser u) {
        return u != null && u.getPassportSeries() != null && !u.getPassportSeries().isBlank()
                && u.getPassportNumber() != null && !u.getPassportNumber().isBlank();
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody Map<String, String> req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).build();

        String dealIdStr = req.get("dealId");
        if (dealIdStr == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "dealId обязателен"));
        Long dealId;
        try { dealId = Long.parseLong(dealIdStr); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Неверный dealId")); }

        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Сделка не найдена"));
        Deal deal = dealOpt.get();
        if (deal.getBuyer() == null || !auth.getName().equals(deal.getBuyer().getEmail())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Оформить ипотеку может только покупатель"));
        }
        if (!hasPassport(deal.getBuyer())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Заполните серию и номер паспорта в профиле"));
        }

        String bankName = req.getOrDefault("bankName", "Банк");
        BigDecimal amount = new BigDecimal(req.getOrDefault("amount", "0"));
        BigDecimal downPayment = new BigDecimal(req.getOrDefault("downPayment", "0"));
        int termMonths = Integer.parseInt(req.getOrDefault("termMonths", "120"));
        BigDecimal rate = new BigDecimal(req.getOrDefault("rate", "10.0"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0 || termMonths <= 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Некорректные параметры ипотеки"));
        }

        // Расчет аннуитетного платежа
        BigDecimal monthlyRate = rate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP)
                                     .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRPowerN = (BigDecimal.ONE.add(monthlyRate)).pow(termMonths);
        BigDecimal numerator = monthlyRate.multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        BigDecimal monthlyPayment = denominator.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : amount.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);

        Mortgage mortgage = mortgageRepository.findByDeal(deal).orElseGet(Mortgage::new);
        mortgage.setDeal(deal);
        mortgage.setBankName(bankName);
        mortgage.setAmount(amount);
        mortgage.setDownPayment(downPayment);
        mortgage.setTermMonths(termMonths);
        mortgage.setRate(rate);
        mortgage.setMonthlyPayment(monthlyPayment);
        mortgage.setStatus(MortgageStatus.REQUESTED);
        mortgageRepository.save(mortgage);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("status", mortgage.getStatus());
        resp.put("monthlyPayment", mortgage.getMonthlyPayment());
        resp.put("bankName", mortgage.getBankName());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{dealId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable Long dealId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).build();
        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.notFound().build();
        Deal deal = dealOpt.get();
        String email = auth.getName();
        boolean participant = (deal.getBuyer() != null && email.equals(deal.getBuyer().getEmail()))
                || (deal.getSeller() != null && email.equals(deal.getSeller().getEmail()))
                || (deal.getRealtor() != null && email.equals(deal.getRealtor().getEmail()));
        if (!participant) return ResponseEntity.status(403).build();

        Optional<Mortgage> mOpt = mortgageRepository.findByDeal(deal);
        Map<String, Object> resp = new HashMap<>();
        resp.put("exists", mOpt.isPresent());
        mOpt.ifPresent(m -> {
            resp.put("status", m.getStatus());
            resp.put("monthlyPayment", m.getMonthlyPayment());
            resp.put("bankName", m.getBankName());
            resp.put("amount", m.getAmount());
            resp.put("termMonths", m.getTermMonths());
            resp.put("rate", m.getRate());
            resp.put("downPayment", m.getDownPayment());
        });
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{dealId}/set-status")
    public ResponseEntity<Map<String, Object>> setStatus(@PathVariable Long dealId, @RequestBody Map<String, String> req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).build();
        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.notFound().build();
        Deal deal = dealOpt.get();
        String email = auth.getName();
        boolean participant = (deal.getBuyer() != null && email.equals(deal.getBuyer().getEmail()))
                || (deal.getSeller() != null && email.equals(deal.getSeller().getEmail()))
                || (deal.getRealtor() != null && email.equals(deal.getRealtor().getEmail()));
        if (!participant) return ResponseEntity.status(403).build();

        Optional<Mortgage> mOpt = mortgageRepository.findByDeal(deal);
        if (mOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Заявка на ипотеку не найдена"));

        String statusStr = req.get("status");
        if (statusStr == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "status обязателен (APPROVED/REJECTED)"));
        MortgageStatus newStatus;
        try { newStatus = MortgageStatus.valueOf(statusStr); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Недопустимый статус")); }
        if (newStatus == MortgageStatus.REQUESTED) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Нельзя вернуть статус REQUESTED"));
        }

        Mortgage m = mOpt.get();
        m.setStatus(newStatus);
        mortgageRepository.save(m);
        return ResponseEntity.ok(Map.of("success", true, "status", m.getStatus()));
    }
}

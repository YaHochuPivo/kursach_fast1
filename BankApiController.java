package com.example.project2.controller;

import com.example.project2.model.*;
import com.example.project2.repository.BankApprovalRepository;
import com.example.project2.repository.DealRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/bank-approval")
public class BankApiController {

    private final DealRepository dealRepository;
    private final BankApprovalRepository bankApprovalRepository;

    public BankApiController(DealRepository dealRepository, BankApprovalRepository bankApprovalRepository) {
        this.dealRepository = dealRepository;
        this.bankApprovalRepository = bankApprovalRepository;
    }

    private static boolean hasPassport(AppUser u) {
        return u != null && u.getPassportSeries() != null && !u.getPassportSeries().isBlank()
                && u.getPassportNumber() != null && !u.getPassportNumber().isBlank();
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestApproval(@RequestBody Map<String, String> req) {
        Map<String, Object> resp = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).build();

        String dealIdStr = req.get("dealId");
        String bankName = req.getOrDefault("bankName", "Банк");
        if (dealIdStr == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "dealId обязателен"));
        Long dealId;
        try { dealId = Long.parseLong(dealIdStr); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Неверный dealId")); }

        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Сделка не найдена"));
        Deal deal = dealOpt.get();
        if (deal.getSeller() == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "В сделке нет продавца"));

        // Только продавец-не риелтор может запросить одобрение
        if (!auth.getName().equals(deal.getSeller().getEmail())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Доступно только продавцу"));
        }
        if (deal.getSeller().isRealtor()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Риелтору не требуется одобрение банка"));
        }
        if (!hasPassport(deal.getSeller())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Заполните серию и номер паспорта в профиле"));
        }

        BankApproval approval = bankApprovalRepository.findByDeal(deal).orElseGet(BankApproval::new);
        approval.setDeal(deal);
        approval.setBankName(bankName);
        // Если у обеих сторон заполнены паспортные данные — сразу одобряем
        boolean buyerHasPassport = hasPassport(deal.getBuyer());
        boolean sellerHasPassport = hasPassport(deal.getSeller());
        if (buyerHasPassport && sellerHasPassport) {
            approval.setStatus(BankApprovalStatus.APPROVED);
        } else {
            approval.setStatus(BankApprovalStatus.REQUESTED);
        }
        approval.setComment(null);
        bankApprovalRepository.save(approval);
        resp.put("success", true);
        resp.put("status", approval.getStatus());
        resp.put("bankName", approval.getBankName());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{dealId}")
    public ResponseEntity<Map<String, Object>> getApproval(@PathVariable Long dealId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ResponseEntity.status(401).build();
        Optional<Deal> dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty()) return ResponseEntity.notFound().build();
        Deal deal = dealOpt.get();
        // участник сделки может видеть статус
        String email = auth.getName();
        boolean participant = (deal.getBuyer() != null && email.equals(deal.getBuyer().getEmail()))
                || (deal.getSeller() != null && email.equals(deal.getSeller().getEmail()))
                || (deal.getRealtor() != null && email.equals(deal.getRealtor().getEmail()));
        if (!participant) return ResponseEntity.status(403).build();

        Map<String, Object> resp = new HashMap<>();
        Optional<BankApproval> ap = bankApprovalRepository.findByDeal(deal);
        resp.put("exists", ap.isPresent());
        ap.ifPresent(a -> {
            // Если статус ещё REQUESTED и у обеих сторон есть паспорт — авто-одобрение
            if (a.getStatus() == BankApprovalStatus.REQUESTED && hasPassport(deal.getBuyer()) && hasPassport(deal.getSeller())) {
                a.setStatus(BankApprovalStatus.APPROVED);
                bankApprovalRepository.save(a);
            }
            resp.put("status", a.getStatus());
            resp.put("bankName", a.getBankName());
        });
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{dealId}/set-status")
    public ResponseEntity<Map<String, Object>> setApprovalStatus(@PathVariable Long dealId, @RequestBody Map<String, String> req) {
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

        Optional<BankApproval> apOpt = bankApprovalRepository.findByDeal(deal);
        if (apOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Запрос одобрения не найден"));
        String statusStr = req.get("status");
        if (statusStr == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "status обязателен (APPROVED/REJECTED)"));
        BankApprovalStatus newStatus;
        try { newStatus = BankApprovalStatus.valueOf(statusStr); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Недопустимый статус")); }
        if (newStatus == BankApprovalStatus.REQUESTED) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Нельзя вернуть статус REQUESTED"));
        }
        BankApproval ap = apOpt.get();
        ap.setStatus(newStatus);
        bankApprovalRepository.save(ap);
        return ResponseEntity.ok(Map.of("success", true, "status", ap.getStatus()));
    }
}

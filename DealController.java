package com.example.project2.controller;

import com.example.project2.model.*;
import com.example.project2.repository.DealRepository;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.BankApprovalRepository;
import com.example.project2.repository.MortgageRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class DealController {

    private final DealRepository dealRepository;
    private final BankApprovalRepository bankApprovalRepository;
    private final MortgageRepository mortgageRepository;
    private final SpringTemplateEngine templateEngine;
    private final AppUserRepository userRepository;

    public DealController(DealRepository dealRepository, BankApprovalRepository bankApprovalRepository, MortgageRepository mortgageRepository, SpringTemplateEngine templateEngine, AppUserRepository userRepository) {
        this.dealRepository = dealRepository;
        this.bankApprovalRepository = bankApprovalRepository;
        this.mortgageRepository = mortgageRepository;
        this.templateEngine = templateEngine;
        this.userRepository = userRepository;
    }

    @GetMapping("/deal/{id}")
    @Transactional
    public String viewDeal(@PathVariable Long id, Model model, @org.springframework.web.bind.annotation.RequestParam(value = "readonly", required = false, defaultValue = "false") boolean readonly) {
        try {
            Optional<Deal> dealOpt = dealRepository.findById(id);
            if (dealOpt.isEmpty()) {
                model.addAttribute("error", "Договор не найден");
                return "error";
            }
            Deal deal = dealOpt.get();
            if (deal == null) {
                model.addAttribute("error", "Договор не найден");
                return "error";
            }
            if (deal.getProperty() == null) {
                model.addAttribute("error", "Объявление для договора не найдено");
                return "error";
            }
            // Инициализируем lazy-поля, которые будут использоваться в шаблоне
            deal.getProperty().getAddress();
            deal.getProperty().getCity();
            deal.getProperty().getDistrict();
            deal.getProperty().getFloor();
            deal.getProperty().getFloorsTotal();
            deal.getProperty().getArea();
            deal.getProperty().getRooms();
            deal.getProperty().getType();
            if (deal.getBuyer() != null) {
                deal.getBuyer().getEmail();
                deal.getBuyer().getFirstName();
                deal.getBuyer().getLastName();
                deal.getBuyer().getPassportSeries();
                deal.getBuyer().getPassportNumber();
            }
            if (deal.getSeller() != null) {
                deal.getSeller().getEmail();
                deal.getSeller().getFirstName();
                deal.getSeller().getLastName();
                deal.getSeller().getPassportSeries();
                deal.getSeller().getPassportNumber();
            }
            if (deal.getRealtor() != null) {
                deal.getRealtor().getEmail();
                deal.getRealtor().getFirstName();
                deal.getRealtor().getLastName();
            }
            model.addAttribute("deal", deal);
            model.addAttribute("property", deal.getProperty());
            model.addAttribute("buyer", deal.getBuyer());
            model.addAttribute("seller", deal.getSeller());
            model.addAttribute("realtor", deal.getRealtor());

            boolean sellerHasPassport = deal.getSeller() != null
                    && deal.getSeller().getPassportSeries() != null && !deal.getSeller().getPassportSeries().isBlank()
                    && deal.getSeller().getPassportNumber() != null && !deal.getSeller().getPassportNumber().isBlank();
            boolean buyerHasPassport = deal.getBuyer() != null
                    && deal.getBuyer().getPassportSeries() != null && !deal.getBuyer().getPassportSeries().isBlank()
                    && deal.getBuyer().getPassportNumber() != null && !deal.getBuyer().getPassportNumber().isBlank();

            boolean isPrivateSeller = deal.getSeller() != null && !deal.getSeller().isRealtor();
            model.addAttribute("isPrivateSeller", isPrivateSeller);

            String bankApprovalStatus = null;
            String bankName = null;
            if (isPrivateSeller) {
                var ap = bankApprovalRepository.findByDeal(deal);
                if (ap.isPresent()) {
                    // Если одобрение запрошено и у обеих сторон есть паспортные данные — автоутверждаем
                    if (ap.get().getStatus() == BankApprovalStatus.REQUESTED && sellerHasPassport && buyerHasPassport) {
                        ap.get().setStatus(BankApprovalStatus.APPROVED);
                        bankApprovalRepository.save(ap.get());
                    }
                    bankApprovalStatus = ap.get().getStatus().name();
                    bankName = ap.get().getBankName();
                }
            }
            model.addAttribute("bankApprovalStatus", bankApprovalStatus);
            model.addAttribute("bankName", bankName);

            boolean canGeneratePdf = sellerHasPassport && buyerHasPassport && (!isPrivateSeller || ("APPROVED".equals(bankApprovalStatus)));
            model.addAttribute("canGeneratePdf", canGeneratePdf);

            // Ипотека: информация для отображения
            String mortgageStatus = null;
            String mortgageBank = null;
            java.math.BigDecimal mortgageMonthly = null;
            var mOpt = mortgageRepository.findByDeal(deal);
            if (mOpt.isPresent()) {
                var m = mOpt.get();
                mortgageStatus = m.getStatus() != null ? m.getStatus().name() : null;
                mortgageBank = m.getBankName();
                mortgageMonthly = m.getMonthlyPayment();
            }
            model.addAttribute("mortgageStatus", mortgageStatus);
            model.addAttribute("mortgageBankName", mortgageBank);
            model.addAttribute("mortgageMonthlyPayment", mortgageMonthly);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth != null ? auth.getName() : null;
            model.addAttribute("currentUserEmail", email);
            model.addAttribute("readonly", readonly);
            return "deal/view";
        } catch (Exception ex) {
            System.err.println("[DealController] Ошибка рендеринга /deal/" + id + ": " + ex.getMessage());
            ex.printStackTrace();
            model.addAttribute("error", "Ошибка при открытии договора: " + ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/deals")
    @Transactional
    public String myDeals(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            model.addAttribute("error", "Необходима аутентификация");
            return "error";
        }
        String email = auth.getName();

        // Ищем пользователя и получаем сделки по userId (надежнее, чем по email)
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("deals", java.util.List.of());
            model.addAttribute("currentUserEmail", email);
            model.addAttribute("isRealtor", false);
            return "deal/list";
        }
        Long userId = userOpt.get().getId();
        var involved = dealRepository.findByUserInvolved(userId);

        // Инициализируем нужные поля для шаблона
        involved.forEach(d -> {
            if (d.getProperty() != null) {
                d.getProperty().getAddress();
                d.getProperty().getCity();
                d.getProperty().getStatus();
                d.getProperty().getType();
            }
            if (d.getBuyer() != null) d.getBuyer().getEmail();
            if (d.getSeller() != null) d.getSeller().getEmail();
            if (d.getRealtor() != null) d.getRealtor().getEmail();
        });

        model.addAttribute("deals", involved);
        model.addAttribute("currentUserEmail", email);
        boolean isRealtor = involved.stream().anyMatch(d -> {
            try { return d.getRealtor() != null && email.equals(d.getRealtor().getEmail()); } catch (Exception e) { return false; }
        });
        model.addAttribute("isRealtor", isRealtor);
        return "deal/list";
    }

    @GetMapping("/deal/{id}/pdf")
    @Transactional
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        Optional<Deal> dealOpt = dealRepository.findById(id);
        if (dealOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Deal deal = dealOpt.get();
        // Паспортные данные обязательны для договора
        boolean sellerHasPassport = deal.getSeller() != null
                && deal.getSeller().getPassportSeries() != null && !deal.getSeller().getPassportSeries().isBlank()
                && deal.getSeller().getPassportNumber() != null && !deal.getSeller().getPassportNumber().isBlank();
        boolean buyerHasPassport = deal.getBuyer() != null
                && deal.getBuyer().getPassportSeries() != null && !deal.getBuyer().getPassportSeries().isBlank()
                && deal.getBuyer().getPassportNumber() != null && !deal.getBuyer().getPassportNumber().isBlank();
        if (!sellerHasPassport || !buyerHasPassport) {
            String msg = "Отсутствуют паспортные данные у участника сделки. Заполните серию и номер паспорта в профиле.";
            return ResponseEntity.status(400).body(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        // Частный продавец — только с одобрением банка
        boolean isPrivateSeller = deal.getSeller() != null && !deal.getSeller().isRealtor();
        if (isPrivateSeller) {
            var ap = bankApprovalRepository.findByDeal(deal);
            if (ap.isEmpty() || ap.get().getStatus() != BankApprovalStatus.APPROVED) {
                String msg = "Для частного продавца требуется одобрение банка для отправки договора.";
                return ResponseEntity.status(403).body(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        // Инициализируем поля
        if (deal.getProperty() != null) {
            deal.getProperty().getAddress();
            deal.getProperty().getCity();
            deal.getProperty().getDistrict();
            deal.getProperty().getFloor();
            deal.getProperty().getFloorsTotal();
            deal.getProperty().getArea();
            deal.getProperty().getRooms();
            deal.getProperty().getType();
        }
        if (deal.getBuyer() != null) {
            deal.getBuyer().getEmail();
            deal.getBuyer().getFirstName();
            deal.getBuyer().getLastName();
            deal.getBuyer().getPassportSeries();
            deal.getBuyer().getPassportNumber();
        }
        if (deal.getSeller() != null) {
            deal.getSeller().getEmail();
            deal.getSeller().getFirstName();
            deal.getSeller().getLastName();
            deal.getSeller().getPassportSeries();
            deal.getSeller().getPassportNumber();
        }
        if (deal.getRealtor() != null) {
            deal.getRealtor().getEmail();
            deal.getRealtor().getFirstName();
            deal.getRealtor().getLastName();
        }
        Context context = new Context();
        context.setVariable("deal", deal);
        context.setVariable("property", deal.getProperty());
        context.setVariable("buyer", deal.getBuyer());
        context.setVariable("seller", deal.getSeller());
        context.setVariable("realtor", deal.getRealtor());
        context.setVariable("currentUserEmail", null);
        // Ипотека в PDF
        String mortgageStatus = null;
        String mortgageBank = null;
        java.math.BigDecimal mortgageMonthly = null;
        var mOpt = mortgageRepository.findByDeal(deal);
        if (mOpt.isPresent()) {
            var m = mOpt.get();
            mortgageStatus = m.getStatus() != null ? m.getStatus().name() : null;
            mortgageBank = m.getBankName();
            mortgageMonthly = m.getMonthlyPayment();
        }
        context.setVariable("mortgageStatus", mortgageStatus);
        context.setVariable("mortgageBankName", mortgageBank);
        context.setVariable("mortgageMonthlyPayment", mortgageMonthly);

        // Рендерим отдельный PDF-шаблон без кнопок и скриптов (без дополнительных обёрток)
        String htmlDoc = templateEngine.process("deal/pdf", context);
        try { System.out.println("[DealController] PDF HTML length: " + (htmlDoc != null ? htmlDoc.length() : 0)); } catch (Exception ignore) {}

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlDoc, null);
            // Встраиваем системные шрифты Windows для корректного отображения кириллицы
            try {
                java.io.File arial = new java.io.File("C:/Windows/Fonts/arial.ttf");
                if (arial.exists()) {
                    builder.useFont(arial, "Arial");
                }
                java.io.File arialBold = new java.io.File("C:/Windows/Fonts/arialbd.ttf");
                if (arialBold.exists()) {
                    builder.useFont(arialBold, "Arial");
                }
            } catch (Exception fe) {
                System.err.println("[DealController] Не удалось встроить шрифты Arial: " + fe.getMessage());
            }
            builder.toStream(os);
            builder.run();
            byte[] bytes = os.toByteArray();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=deal-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("PDF generation error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}

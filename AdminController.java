package com.example.project2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping({"/admin/dashboard", "/admin/users", "/admin/properties", "/admin/deals", "/admin/reports", "/admin/settings"})
    public String redirectAdminToHome() {
        return "redirect:/";
    }
}

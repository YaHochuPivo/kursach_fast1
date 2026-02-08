package com.example.project2.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RealtorController {

    @GetMapping("/realtor/dashboard")
    public String realtorDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "realtor/dashboard";
    }

    @GetMapping("/realtor/properties")
    public String realtorProperties(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "realtor/properties";
    }

    @GetMapping("/realtor/deals")
    public String realtorDeals(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "realtor/deals";
    }

    @GetMapping("/realtor/clients")
    public String realtorClients(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "realtor/clients";
    }

    @GetMapping("/realtor/reports")
    public String realtorReports(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "realtor/reports";
    }

    @GetMapping("/realtor/stats")
    public String realtorStats(Model model) {
        return "realtor/stats";
    }
}

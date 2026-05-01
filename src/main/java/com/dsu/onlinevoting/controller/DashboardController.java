package com.dsu.onlinevoting.controller;

import com.dsu.onlinevoting.model.Role;
import com.dsu.onlinevoting.model.User;
import com.dsu.onlinevoting.service.UserService;
import com.dsu.onlinevoting.service.VotingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserService userService;
    private final VotingService votingService;

    public DashboardController(UserService userService, VotingService votingService) {
        this.userService = userService;
        this.votingService = votingService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/how-it-works")
    public String howItWorks() {
        return "how-it-works";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);

        if (user.getRole() == Role.ADMIN) {
            model.addAttribute("results", votingService.getResults());
            return "admin/dashboard";
        } else if (user.getRole() == Role.FIELD_OFFICER) {
            model.addAttribute("pendingVoters", userService.getPendingVoterApplications());
            return "fieldofficer/dashboard";
        } else if (user.getRole() == Role.CANDIDATE) {
            model.addAttribute("profile", user.getCandidateProfile());
            return "candidate/dashboard";
        } else {
            model.addAttribute("profile", user.getVoterProfile());
            model.addAttribute("hasVoted", votingService.hasVoted(user));
            return "voter/dashboard";
        }
    }
}

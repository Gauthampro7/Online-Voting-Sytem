package com.dsu.onlinevoting.controller;

import com.dsu.onlinevoting.model.VerificationStatus;
import com.dsu.onlinevoting.service.UserService;
import com.dsu.onlinevoting.service.VotingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final VotingService votingService;

    public AdminController(UserService userService, VotingService votingService) {
        this.userService = userService;
        this.votingService = votingService;
    }

    @GetMapping("/candidates")
    public String manageCandidates(Model model) {
        model.addAttribute("pendingCandidates", userService.getPendingCandidateApplications());
        return "admin/candidates";
    }

    @PostMapping("/candidates/approve")
    public String approveCandidate(@RequestParam Long profileId) {
        userService.updateCandidateStatus(profileId, VerificationStatus.APPROVED);
        return "redirect:/admin/candidates?success";
    }

    @PostMapping("/candidates/reject")
    public String rejectCandidate(@RequestParam Long profileId) {
        userService.updateCandidateStatus(profileId, VerificationStatus.REJECTED);
        return "redirect:/admin/candidates?rejected";
    }
    
    @GetMapping("/results")
    public String viewResults(Model model) {
        model.addAttribute("results", votingService.getResults());
        return "admin/results";
    }
}

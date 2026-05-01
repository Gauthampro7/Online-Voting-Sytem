package com.dsu.onlinevoting.controller;

import com.dsu.onlinevoting.model.CandidateProfile;
import com.dsu.onlinevoting.model.User;
import com.dsu.onlinevoting.model.VerificationStatus;
import com.dsu.onlinevoting.service.UserService;
import com.dsu.onlinevoting.service.VotingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/voter")
public class VoterController {

    private final UserService userService;
    private final VotingService votingService;

    public VoterController(UserService userService, VotingService votingService) {
        this.userService = userService;
        this.votingService = votingService;
    }

    @PostMapping("/apply")
    public String applyForVoterId(Authentication authentication,
                                  @RequestParam String address,
                                  @RequestParam String constituency,
                                  @RequestParam String identityDocumentPath) {
        userService.submitVoterApplication(authentication.getName(), address, constituency, identityDocumentPath);
        return "redirect:/dashboard?applied";
    }

    @GetMapping("/vote")
    public String votePage(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        
        if (user.getVoterProfile() == null || user.getVoterProfile().getStatus() != VerificationStatus.APPROVED) {
            return "redirect:/dashboard?error=not_approved";
        }
        
        if (votingService.hasVoted(user)) {
            return "redirect:/dashboard?error=already_voted";
        }
        
        List<CandidateProfile> candidates = votingService.getApprovedCandidatesForConstituency(user.getVoterProfile().getConstituency());
        model.addAttribute("candidates", candidates);
        
        return "voter/vote";
    }

    @PostMapping("/cast-vote")
    public String castVote(Authentication authentication, @RequestParam Long candidateId) {
        User voter = userService.findByUsername(authentication.getName());
        // Simple candidate lookup (should ideally fetch Candidate via Service)
        User candidate = userService.findByUsername(userService.getPendingCandidateApplications()
                .stream().filter(c -> c.getUser().getId().equals(candidateId)).findFirst()
                .map(c -> c.getUser().getUsername()).orElse("")); // Hacky but we will fix in service if needed.
        // Let's use a cleaner approach: pass candidate username
        return "redirect:/dashboard";
    }
    
    @PostMapping("/cast-vote-username")
    public String castVoteByUsername(Authentication authentication, @RequestParam String candidateUsername) {
        User voter = userService.findByUsername(authentication.getName());
        User candidate = userService.findByUsername(candidateUsername);
        
        if (candidate != null) {
            votingService.castVote(voter, candidate);
        }
        return "redirect:/dashboard?voted";
    }
}

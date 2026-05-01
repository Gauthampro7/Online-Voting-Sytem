package com.dsu.onlinevoting.controller;

import com.dsu.onlinevoting.model.VerificationStatus;
import com.dsu.onlinevoting.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/field-officer")
public class FieldOfficerController {

    private final UserService userService;

    public FieldOfficerController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/voters/approve")
    public String approveVoter(@RequestParam Long profileId) {
        userService.updateVoterStatus(profileId, VerificationStatus.APPROVED);
        return "redirect:/dashboard?success";
    }

    @PostMapping("/voters/reject")
    public String rejectVoter(@RequestParam Long profileId) {
        userService.updateVoterStatus(profileId, VerificationStatus.REJECTED);
        return "redirect:/dashboard?rejected";
    }
}

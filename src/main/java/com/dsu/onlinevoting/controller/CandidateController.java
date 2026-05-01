package com.dsu.onlinevoting.controller;

import com.dsu.onlinevoting.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/candidate")
public class CandidateController {

    private final UserService userService;

    public CandidateController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/apply")
    public String applyForCandidacy(Authentication authentication,
                                  @RequestParam String party,
                                  @RequestParam String constituency,
                                  @RequestParam String manifesto) {
        userService.submitCandidateApplication(authentication.getName(), party, constituency, manifesto);
        return "redirect:/dashboard?applied";
    }
}

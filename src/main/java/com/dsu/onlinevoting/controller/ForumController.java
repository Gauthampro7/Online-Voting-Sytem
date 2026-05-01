package com.dsu.onlinevoting.controller;

import com.dsu.onlinevoting.model.User;
import com.dsu.onlinevoting.service.DiscussionService;
import com.dsu.onlinevoting.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/forums")
public class ForumController {

    private final DiscussionService discussionService;
    private final UserService userService;

    public ForumController(DiscussionService discussionService, UserService userService) {
        this.discussionService = discussionService;
        this.userService = userService;
    }

    @GetMapping
    public String viewForums(Model model) {
        model.addAttribute("posts", discussionService.getAllPosts());
        return "forums";
    }

    @PostMapping("/post")
    public String addPost(Authentication authentication, @RequestParam String content) {
        User user = userService.findByUsername(authentication.getName());
        discussionService.addPost(user, content);
        return "redirect:/forums";
    }
}

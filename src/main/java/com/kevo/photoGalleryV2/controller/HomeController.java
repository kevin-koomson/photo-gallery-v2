package com.kevo.photoGalleryV2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@SessionAttributes("userName")
public class HomeController {
    @GetMapping("/")
    public String index(Model model, @SessionAttribute(value = "userName", required = false) String userName) {
        if (userName != null && !userName.isEmpty()) {
            model.addAttribute("userName", userName);
            return "dashboard";
        }
        return "index";
    }

    @PostMapping("/welcome")
    public String welcome(@RequestParam("name") String name, Model model) {
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Please enter your name");
            return "index";
        }
        model.addAttribute("userName", name.trim());
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@SessionAttribute(value = "userName", required = false) String userName, Model model) {
        if (userName == null || userName.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("userName", userName);
        return "dashboard";
    }
    @GetMapping("/name-change")
    public String showNameChange(@SessionAttribute(value = "userName", required = false) String userName, Model model) {
        if (userName == null || userName.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("currentName", userName);
        return "name-change";
    }

    @PostMapping("/name-change")
    public String processNameChange(@RequestParam("newName") String newName,
                                    @SessionAttribute(value = "userName", required = false) String userName,
                                    Model model) {
        if (userName == null || userName.isEmpty()) {
            return "redirect:/";
        }

        if (newName == null || newName.trim().isEmpty()) {
            model.addAttribute("currentName", userName);
            model.addAttribute("error", "Please enter a new name");
            return "name-change";
        }

        model.addAttribute("userName", newName.trim());
        model.addAttribute("success", "Name changed successfully!");
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(SessionStatus sessionStatus) {
        sessionStatus.setComplete();
        return "redirect:/";
    }
}
package com.example.ProjetoSD;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@SessionAttributes("user")
public class ProjectController {
    
    private List<Project> projects = new ArrayList<>();
    private int applicationCounter = 0;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login-submit")
    public String loginSubmit(@ModelAttribute User user, HttpSession session) {
        // Aqui você deve implementar a lógica real de autenticação
        // Este é apenas um exemplo simples
        if ("admin".equals(user.getUsername()) && "admin".equals(user.getPassword())) {
            user.setLoggedIn(true);
            session.setAttribute("user", user);
            return "redirect:/create-project";
        }
        return "redirect:/login?error=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/create-project")
    public String createProjectForm(Model model) {
        model.addAttribute("project", new Project());
        return "create-project";
    }

    @PostMapping("/save-project")
    public String saveProjectSubmission(@ModelAttribute Project project, Model model) {
        projects.add(project);
        model.addAttribute("project", project);
        return "result";
    }

    @GetMapping("/projects")
    public String listProjects(Model model) {
        model.addAttribute("projects", projects);
        return "projects";
    }

    @GetMapping("/counters")
    public String counters(Model model, HttpSession session) {
        // Request Scope
        int requestCounter = 0;
        
        // Session Scope - agora usando um objeto User
        User user = (User) session.getAttribute("user");
        if (user == null) {
            user = new User();
            session.setAttribute("user", user);
        }
        
        // Application Scope
        applicationCounter++;
        
        model.addAttribute("requestCounter", requestCounter);
        model.addAttribute("user", user);
        model.addAttribute("applicationCounter", applicationCounter);
        
        return "counters";
    }
} 
package com.example.ProjetoSD; 

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Arrays;
import java.util.List;

@Controller
public class GreetingController {

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="Mundo") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting"; // Ele vai procurar o template greeting.html
    }

    @GetMapping("/redirect")
    public String redirect() {
        return "redirect:/greeting";
    }

    @GetMapping("/table")
    public String table(Model model) {
        List<Employee> employees = Arrays.asList(
            new Employee(1, "João Silva", "Desenvolvedor"),
            new Employee(2, "Maria Santos", "Designer"),
            new Employee(3, "Pedro Oliveira", "Gerente")
        );
        model.addAttribute("employees", employees);
        return "table";
    }

}
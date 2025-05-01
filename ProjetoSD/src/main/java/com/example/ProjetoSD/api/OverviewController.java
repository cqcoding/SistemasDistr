package com.example.ProjetoSD.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/overview")
public class OverviewController {

    @GetMapping
    public String getOverview() {
        return "Visão geral da aplicação!";
    }

}

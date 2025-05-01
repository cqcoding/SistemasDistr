package com.example.ProjetoSD.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @GetMapping
    public String getCluster() {
        return "Cluster encontrado!";
    }

}

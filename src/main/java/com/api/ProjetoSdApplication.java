package com.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;

@SpringBootApplication(exclude = {
    LiquibaseAutoConfiguration.class,
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class
})
 // diz ao Spring Boot para não configurar automaticamente o Liquibase e o R2DBC
// Liquibase: ferramenta de migração de banco de dados que ajuda a gerenciar as mudanças no schema do BD.
public class ProjetoSdApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}

}
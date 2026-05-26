package br.mil.sfpc.indicadores;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import br.mil.sfpc.indicadores.config.ModuloProperties;

/**
 * Ponto de entrada da aplicação Spring Boot.
 * Carrega módulos definidos em {@code modulos.yml} e conecta ao PostgreSQL configurado em {@code application.yml}.
 */
@SpringBootApplication
@EnableConfigurationProperties(ModuloProperties.class)
public class IndicadoresApplication {

    /** Inicia o servidor web (porta 8080 por padrão). */
    public static void main(String[] args) {
        SpringApplication.run(IndicadoresApplication.class, args);
    }
}

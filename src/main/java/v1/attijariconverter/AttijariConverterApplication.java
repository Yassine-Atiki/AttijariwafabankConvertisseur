package v1.attijariconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale Spring Boot.
 * Démarre le contexte Spring (web, sécurité, Mongo...).
 */
@SpringBootApplication
public class AttijariConverterApplication {

    /**
     * Point d'entrée JVM.
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        SpringApplication.run(AttijariConverterApplication.class, args);
    }

}

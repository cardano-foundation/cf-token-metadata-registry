package org.cardanofoundation.metadatatools.metafides;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;


@SpringBootApplication(exclude = {R2dbcAutoConfiguration.class})
@OpenAPIDefinition(info = @Info(title = "APIs", version = "1.0", description = "Metafides APIs v1.0"))
public class MetafidesApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetafidesApplication.class, args);
    }
}

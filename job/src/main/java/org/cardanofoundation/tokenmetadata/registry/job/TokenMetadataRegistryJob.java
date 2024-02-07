package org.cardanofoundation.tokenmetadata.registry.job;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMetadataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "org.cardanofoundation.tokenmetadata.registry")
@EnableJpaRepositories("org.cardanofoundation.tokenmetadata.registry.repository")
@EntityScan(basePackages = "org.cardanofoundation.tokenmetadata.registry.entity")
@Slf4j
public class TokenMetadataRegistryJob implements CommandLineRunner {

    @Autowired
    private TokenMetadataSyncService tokenMetadataSyncService;


    public static void main(String[] args) {
        SpringApplication.run(TokenMetadataRegistryJob.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("Started...");
        tokenMetadataSyncService.synchronizeDatabase();
        log.info("Complete...");
    }

}

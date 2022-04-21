package org.cardanofoundation.metadatatools.api;

import lombok.*;

@Data
@NoArgsConstructor
public class DatabaseConnectionParameters {
    private String username;
    private String password;
    private String url;
    private String driverClassName;
}

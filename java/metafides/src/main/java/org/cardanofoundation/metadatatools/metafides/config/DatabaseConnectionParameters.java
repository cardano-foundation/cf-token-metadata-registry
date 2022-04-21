package org.cardanofoundation.metadatatools.metafides.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DatabaseConnectionParameters {
    private String username;
    private String password;
    private String host;
    private Integer port;
    private String databaseName;
}

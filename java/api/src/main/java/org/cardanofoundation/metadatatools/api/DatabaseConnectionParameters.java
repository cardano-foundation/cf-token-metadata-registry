package org.cardanofoundation.metadatatools.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DatabaseConnectionParameters {
    private String username;
    private String password;
    private String url;
    private String driverClassName;
}

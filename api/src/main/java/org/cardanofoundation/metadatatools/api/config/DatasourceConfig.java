package org.cardanofoundation.metadatatools.api.config;

import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class DatasourceConfig {
  private static final String DEFAULT_DB_DRIVER_NAME = "org.postgresql.Driver";
  private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/cf_metadata";

  private DatabaseConnectionParameters getConnectionParametersFromEnvironment() {
    final DatabaseConnectionParameters params = new DatabaseConnectionParameters();
    params.setUsername(System.getProperty("dbUser"));
    params.setPassword(System.getProperty("dbSecret"));
    params.setDriverClassName(System.getProperty("dbDriverName", DEFAULT_DB_DRIVER_NAME));
    params.setUrl(System.getProperty("dbUrl", DEFAULT_DB_URL));
    return params;
  }

  @Bean
  public DataSource getDataSource() {
    final DatabaseConnectionParameters databaseConnectionParams =
        getConnectionParametersFromEnvironment();
    final DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName(databaseConnectionParams.getDriverClassName());
    dataSourceBuilder.url(databaseConnectionParams.getUrl());
    dataSourceBuilder.username(databaseConnectionParams.getUsername());
    dataSourceBuilder.password(databaseConnectionParams.getPassword());
    log.info(
        String.format(
            "Trying to connect to database %s with driver %s",
            databaseConnectionParams.getUrl(), databaseConnectionParams.getDriverClassName()));
    return dataSourceBuilder.build();
  }
}

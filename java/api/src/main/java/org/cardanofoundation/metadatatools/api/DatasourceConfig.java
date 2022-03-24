package org.cardanofoundation.metadatatools.api;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

import javax.sql.DataSource;

@Log4j2
@Configuration
public class DatasourceConfig {
    private enum DatabaseConnectionParametersProviderType {
        ENVIRONMENT, AWS_SSM
    }

    private static final String DEFAULT_DB_DRIVER_NAME = "org.postgresql.Driver";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/cf_metadata";
    private static final String DEFAULT_DB_CONNECTION_PARAMS_PROVIDER_TYPE = DatabaseConnectionParametersProviderType.ENVIRONMENT.name();

    private DatabaseConnectionParameters getConnectionParametersFromEnvironment() {
        final DatabaseConnectionParameters params = new DatabaseConnectionParameters();
        params.setUsername(System.getProperty("dbUser"));
        params.setPassword(System.getProperty("dbSecret"));
        params.setDriverClassName(System.getProperty("dbDriverName", DEFAULT_DB_DRIVER_NAME));
        params.setUrl(System.getProperty("dbUrl", DEFAULT_DB_URL));
        return params;
    }

    private DatabaseConnectionParameters getConnectionParametersFromAwsSsm() {
        final Region region = Region.of(System.getProperty("region"));
        final DatabaseConnectionParameters params = new DatabaseConnectionParameters();
        params.setUsername(getEncryptedSsmParameter(System.getProperty("rdsUsernameSsmParameterName"), region));
        params.setPassword(getEncryptedSsmParameter(System.getProperty("rdsPasswordSsmParameterName"), region));
        params.setUrl(getEncryptedSsmParameter(System.getProperty("rdsUrlSsmParameterName"), region));
        params.setDriverClassName(getEncryptedSsmParameter(System.getProperty("rdsDriverClassNameSsmParameterName"), region));
        return params;
    }

    @Bean
    public DataSource getDataSource() {
        final DatabaseConnectionParametersProviderType secretsProviderType = DatabaseConnectionParametersProviderType.valueOf(System.getProperty("dbConnectionParamsProviderType", DEFAULT_DB_CONNECTION_PARAMS_PROVIDER_TYPE));
        final DatabaseConnectionParameters databaseConnectionParams = switch (secretsProviderType) {
            case ENVIRONMENT -> getConnectionParametersFromEnvironment();
            case AWS_SSM -> getConnectionParametersFromAwsSsm();
        };

        final DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(databaseConnectionParams.getDriverClassName());
        dataSourceBuilder.url(databaseConnectionParams.getUrl());
        dataSourceBuilder.username(databaseConnectionParams.getUsername());
        dataSourceBuilder.password(databaseConnectionParams.getPassword());
        return dataSourceBuilder.build();
    }

    private static String getEncryptedSsmParameter(final String parameterName, final Region region) {
        try (final SsmClient ssmClient = SsmClient.builder().region(region).build()) {
            final GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            final GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            return parameterResponse.parameter().value();
        } catch (final SsmException e) {
            log.error("Could not fetch database connection parameter information from AWS SSM.", e);
            return "";
        }
    }
}
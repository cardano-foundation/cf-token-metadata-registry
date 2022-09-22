package org.cardanofoundation.metadatatools.api.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${dbUser}")
    private String dbUser;
    @Value("${dbSecret}")
    private String dbSecret;
    @Value("${spring.datasource.url}")
    private String dbUrl;

    private static final String DEFAULT_DB_DRIVER_NAME = "org.postgresql.Driver";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/cf_metadata";
    private static final String DEFAULT_DB_CONNECTION_PARAMS_PROVIDER_TYPE = DatabaseConnectionParametersProviderType.ENVIRONMENT.name();

    private DatabaseConnectionParameters getConnectionParametersFromEnvironment() {
        final DatabaseConnectionParameters params = new DatabaseConnectionParameters();
        System.out.println("getConnectionParametersFromEnvironment");
        System.out.println("dbUser: " + System.getProperty("dbUser"));
        System.out.println("dbSecret: " + System.getProperty("dbSecret"));
        System.out.println("dbDriverName: " + System.getProperty("DEFAULT_DB_DRIVER_NAME"));
        System.out.println("dbUrl: " + System.getProperty("dbUrl"));
        params.setUsername(dbUser);
        params.setPassword(dbSecret);
        params.setDriverClassName(System.getProperty("dbDriverName", DEFAULT_DB_DRIVER_NAME));
        params.setUrl(System.getProperty("dbUrl", dbUrl));
        return params;
    }

    private DatabaseConnectionParameters getConnectionParametersFromAwsSsm() {
        System.out.println("getConnectionParametersFromAwsSsm");
        System.out.println("Region: " + System.getProperty("region"));
        System.out.println("rdsUsernameSsmParameterName: " + System.getProperty("rdsUsernameSsmParameterName"));
        System.out.println("rdsPasswordSsmParameterName: " + System.getProperty("rdsPasswordSsmParameterName"));
        System.out.println("rdsUrlSsmParameterName: " + System.getProperty("rdsUrlSsmParameterName"));
        System.out.println("rdsDriverClassNameSsmParameterName: " + System.getProperty("rdsDriverClassNameSsmParameterName"));
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
        System.out.println("secretsProviderType: " + secretsProviderType);
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
        try (SsmClient ssmClient = SsmClient.builder().region(region).build()) {
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
package org.cardanofoundation.metadatatools.metafides.config;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;


@Log4j2
@Configuration
@EnableR2dbcRepositories
public class DatasourceConfig extends AbstractR2dbcConfiguration {
    private enum DatabaseConnectionParametersProviderType {
        ENVIRONMENT, AWS_SSM
    }

    private static final String DEFAULT_DB_CONNECTION_PARAMS_PROVIDER_TYPE = DatabaseConnectionParametersProviderType.ENVIRONMENT.name();

    private DatabaseConnectionParameters getConnectionParametersFromEnvironment() {
        final DatabaseConnectionParameters params = new DatabaseConnectionParameters();
        params.setUsername(System.getProperty("dbUser"));
        params.setPassword(System.getProperty("dbSecret"));
        params.setHost(System.getProperty("dbHost"));
        params.setPort(Integer.parseInt(System.getProperty("dbPort")));
        params.setDatabaseName(System.getProperty("dbName"));
        return params;
    }

    private DatabaseConnectionParameters getConnectionParametersFromAwsSsm() {
        final Region region = Region.of(System.getProperty("region"));
        final DatabaseConnectionParameters params = new DatabaseConnectionParameters();
        params.setUsername(getEncryptedSsmParameter(System.getProperty("rdsUsernameSsmParameterName"), region));
        params.setPassword(getEncryptedSsmParameter(System.getProperty("rdsPasswordSsmParameterName"), region));
        params.setHost(getEncryptedSsmParameter(System.getProperty("rdsHostSsmParameterName"), region));
        params.setPort(Integer.parseInt(getEncryptedSsmParameter(System.getProperty("rdsPortSsmParameterName"), region)));
        params.setDatabaseName(getEncryptedSsmParameter(System.getProperty("rdsDatabaseNameSsmParameterName"), region));
        return params;
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

    @Override
    public ConnectionFactory connectionFactory() {
        final DatabaseConnectionParametersProviderType secretsProviderType = DatabaseConnectionParametersProviderType.valueOf(System.getProperty("dbConnectionParamsProviderType", DEFAULT_DB_CONNECTION_PARAMS_PROVIDER_TYPE));
        final DatabaseConnectionParameters databaseConnectionParams = switch (secretsProviderType) {
            case ENVIRONMENT -> getConnectionParametersFromEnvironment();
            case AWS_SSM -> getConnectionParametersFromAwsSsm();
        };

        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .username(databaseConnectionParams.getUsername())
                        .password(databaseConnectionParams.getPassword())
                        .database(databaseConnectionParams.getDatabaseName())
                        .port(databaseConnectionParams.getPort())
                        .host(databaseConnectionParams.getHost())
                        .build()
        );
    }
}
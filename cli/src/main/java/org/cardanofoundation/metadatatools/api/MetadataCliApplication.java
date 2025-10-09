package org.cardanofoundation.metadatatools.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.cardanofoundation.metadatatools.core.cip26.model.Metadata;
import org.cardanofoundation.metadatatools.core.cip26.model.MetadataProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@SpringBootApplication
public class MetadataCliApplication implements CommandLineRunner {

    public static void main(final String[] args) {
        SpringApplication.run(MetadataCliApplication.class, args);
    }

    private static ObjectMapper getObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

    private boolean validateSubject(final String subject) {
        return subject.matches("[0-9A-Fa-f]+") && ((subject.length() % 2) == 0);
    }

    private void runInit(final String[] args) {
        final Options options = new Options();
        options.addOption(Option.builder()
                .longOpt("subject")
                .desc("Specify the subject of the metadata entry.")
                .hasArg()
                .type(String.class).build());
        options.addOption(Option.builder()
                .longOpt("policy")
                .desc("Specify the CBOR hex representation of the monetary policy script of asset this metadata entry relates to.")
                .hasArg()
                .type(String.class).build());
        options.addOption(Option.builder()
                .longOpt("assetname")
                .desc("Specify the name of the asset that was specified in the minting operation.")
                .hasArg()
                .type(String.class).build());
		options.addOption(Option.builder()
                .longOpt("outfile")
                .desc("If specified the output is written to the specified file or following the CIP-26 conventions. Otherwise output is written to stdout.")
                .optionalArg(true).build());

        try {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("subject")) {
                final String subject = cmd.getOptionValue("subject").toLowerCase(Locale.ROOT).strip();
                if (validateSubject(subject)) {
                    final Metadata tokenMetadata = new Metadata();
                    tokenMetadata.setSubject(subject);
                    tokenMetadata.addProperty("name", new MetadataProperty<>("", 0, List.of()));
                    tokenMetadata.addProperty("description", new MetadataProperty<>("", 0, List.of()));
                    final ObjectMapper objectMapper = getObjectMapper();
                    if (cmd.hasOption("outfile")) {
                        final String outfilePath;
                        if (cmd.getOptionValue("outfile") != null) {
                            outfilePath = cmd.getOptionValue("outfile");
                        } else {
                            outfilePath = String.format("%s.json", subject);
                        }
                        objectMapper.writeValue(new File(outfilePath), tokenMetadata);
                    } else {
                        System.out.println(objectMapper.writeValueAsString(tokenMetadata));
                    }
                } else {
                    System.out.println("The given subject is not a hexadecimal string or its length is not even.");
                }
            } else if (cmd.hasOption("policy") && cmd.hasOption("assetname")) {
                
            } else {
                System.out.println("You have to either specify a subject or a policy and assetname.");
                printHelp(options);
            }
        } catch (final ParseException e) {
            printHelp(options);
        } catch (final JsonProcessingException e) {
            log.error("Unexpected error: Was not able to serialize output.", e);
        } catch (IOException e) {
            log.error("Unexpected error: Was not able to write result to file.", e);
        }
    }

    private void runEntry(final String[] args) {

    }

    private void runValidate(final String[] args) {

    }

    private void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("metadata-tool", options, true);
    }

    private void printHelp() {
        System.err.println("Invalid number of arguments: metadata-tool [entry|init|validate]");
    }

    @Override
    public void run(String... args) {
        if (args.length < 1) {
            printHelp();
            return;
        }

        switch (args[0]) {
            case "init" -> runInit(Arrays.copyOfRange(args, 1, args.length));
            case "entry" -> runEntry(Arrays.copyOfRange(args, 1, args.length));
            case "validate" -> runValidate(Arrays.copyOfRange(args, 1, args.length));
            default -> printHelp();
        }

        // cip26 init --subject (optional: subject itself) --policy (optional: cborhex of the policy) --assetname (name of the native asset) --outfile
        // needs subject or policy+assetname
		// cip26 entry --subject (mandatory: the subject itself) --name,ticker,url,decimals,description,logo (wellknown properties) --key:value (other properties)
		// looks for an existing json file, adds or updates the corresponding entries
        // cip26 entry --sign --subject (mandatory: the subject itself) --key (mandatory: the signing key file)
        // cip26 validate --file (mandatory: the file that shall be validated) --old-file (optional: predecessor
    }
}
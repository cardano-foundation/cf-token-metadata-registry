package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
class MetadataV2ParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void parseFLDTDatumTest() throws JsonProcessingException {

        List<String> priorities = Stream.of(QueryPriority.values()).map(QueryPriority::name).toList();

        Metadata metadata = new Metadata(new StringProperty("name", QueryPriority.CIP_68.name()),
                new StringProperty("description", QueryPriority.CIP_68.name()),
                new StringProperty("ticker", QueryPriority.CIP_68.name()),
                new LongProperty(6L, QueryPriority.CIP_26.name()),
                new StringProperty("logo", QueryPriority.CIP_26.name()),
                new StringProperty("http://url.com", QueryPriority.CIP_26.name()),
                new LongProperty(1L, QueryPriority.CIP_26.name()));

        Subject subject = new Subject("subject", metadata, new Standards(null, null), null);
        Response response = new Response(subject, priorities);
        log.info("response: {}", OBJECT_MAPPER.writeValueAsString(response));

        List<Subject> subjects = List.of(subject);
        BatchResponse batchResponse = new BatchResponse(subjects, priorities);
        log.info("batchResponse: {}", OBJECT_MAPPER.writeValueAsString(batchResponse));


    }


}
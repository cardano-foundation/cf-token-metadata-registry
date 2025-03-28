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
    public void parseFLDTDatumTest() throws JsonProcessingException {

        var priorities = Stream.of(QueryPriority.values()).map(QueryPriority::name).toList();

        var metadata = new Metadata(new StringProperty("name", QueryPriority.CIP_68.name()),
                new StringProperty("description", QueryPriority.CIP_68.name()),
                new StringProperty("ticker", QueryPriority.CIP_68.name()),
                new LongProperty(6L, QueryPriority.CIP_26.name()),
                new StringProperty("logo", QueryPriority.CIP_26.name()),
                new StringProperty("http://url.com", QueryPriority.CIP_26.name()),
                new LongProperty(1L, QueryPriority.CIP_26.name()));

        var subject = new Subject("subject", metadata, new Standards(null, "foo"));
        var response = new Response(subject, priorities);
        log.info("response: {}", OBJECT_MAPPER.writeValueAsString(response));

        var subjects = List.of(subject);
        var batchResponse = new BatchResponse(subjects, priorities);
        log.info("batchResponse: {}", OBJECT_MAPPER.writeValueAsString(batchResponse));


    }


}
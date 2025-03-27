package org.cardanofoundation.tokenmetadata.registry.api.controller;

import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface V2Api {

    @GetMapping(path = "/subjects/{subject}", produces = {"application/json;charset=utf-8"})
    ResponseEntity<Response> getSubject(@PathVariable("subject") final String subject,
                                        @RequestParam(value = "property", required = false) final List<String> properties,
                                        @RequestParam(value = "query_priority", required = false) final List<QueryPriority> priorities);


}








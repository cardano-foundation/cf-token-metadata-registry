package org.cardanofoundation.metadatatools.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("${openapi.metadataServer.base-path:}")
public class MetadataApiController implements MetadataApi {

    private final MetadataApiDelegate delegate;

    public MetadataApiController(@Autowired(required = false) MetadataApiDelegate delegate) {
        this.delegate = Optional.ofNullable(delegate).orElse(new MetadataApiDelegate() {});
    }

    @Override
    public MetadataApiDelegate getDelegate() {
        return delegate;
    }

}

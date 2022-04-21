package org.cardanofoundation.metadatatools.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Generated;
import java.util.Optional;

@Controller
@RequestMapping("${openapi.metadataServer.base-path:}")
public class V2ApiController implements V2Api {

    private final V2ApiDelegate delegate;

    public V2ApiController(@Autowired(required = false) V2ApiDelegate delegate) {
        this.delegate = Optional.ofNullable(delegate).orElse(new V2ApiDelegate() {});
    }

    @Override
    public V2ApiDelegate getDelegate() {
        return delegate;
    }
}

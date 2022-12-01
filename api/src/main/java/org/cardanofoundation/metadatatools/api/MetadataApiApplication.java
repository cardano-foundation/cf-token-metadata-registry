package org.cardanofoundation.metadatatools.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.DispatcherType;
import java.util.List;

import static javax.servlet.DispatcherType.*;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@SpringBootApplication
public class MetadataApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(MetadataApiApplication.class, args);
	}

	@Bean
	public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
		final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
		final FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setDispatcherTypes(REQUEST, ASYNC, ERROR);
		registration.setOrder(HIGHEST_PRECEDENCE);
		registration.setUrlPatterns(List.of("/**"));

		return registration;
	}

}

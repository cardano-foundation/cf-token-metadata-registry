package org.cardanofoundation.metadatatools.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ForwardedHeaderFilter;

import jakarta.servlet.DispatcherType;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class MetadataApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(MetadataApiApplication.class, args);
	}

	@Bean
	public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
		final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
		final FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		registration.setUrlPatterns(List.of("/**"));
		return registration;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}

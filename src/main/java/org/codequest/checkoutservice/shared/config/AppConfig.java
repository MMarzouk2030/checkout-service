package org.codequest.checkoutservice.shared.config;

import org.codequest.checkoutservice.shared.rest.PaymentClient;
import org.codequest.checkoutservice.shared.rest.PaymentProviderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Bean
    public PaymentClient paymentClient() {
        return buildClient(PaymentClient.class);
    }

    @Bean
    public PaymentProviderClient paymentProviderClient() {
        return buildClient(PaymentProviderClient.class);
    }

    private <T> T buildClient(Class<T> clientType) {
        RestClient restClient = RestClient.create(appBaseUrl);
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientType);
    }
}

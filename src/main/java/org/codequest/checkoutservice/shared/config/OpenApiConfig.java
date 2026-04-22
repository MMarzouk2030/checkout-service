package org.codequest.checkoutservice.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI checkoutServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Checkout Service API")
                        .version("1.0.0")
                        .description("""
                                REST API for the checkout-service — manages the full e-commerce
                                checkout flow: **cart → order → payment → paid**.

                                ## Flow summary
                                1. Create a cart and add items (`/carts`)
                                2. Checkout the cart — creates an Order in `CREATED` state
                                3. Start payment — order moves to `PENDING_PAYMENT`
                                4. Payment provider delivers a webhook — order moves to `PAID` or `PAYMENT_FAILED`
                                5. On failure, retry via `/orders/{id}/payment/start`; cancel via `/orders/{id}/cancel`

                                ## State machines
                                - **Order states:** `CREATED → PENDING_PAYMENT → PAID | PAYMENT_FAILED → PENDING_PAYMENT | CANCELLED`
                                - **Payment states:** `PENDING → CONFIRMED | FAILED`
                                """)
                        .contact(new Contact()
                                .name("CodeQuest")
                                .email("dev@codequest.org"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Local / default server")
                ));
    }
}

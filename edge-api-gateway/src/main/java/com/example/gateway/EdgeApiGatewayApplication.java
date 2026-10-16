package com.example.gateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EdgeApiGatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(EdgeApiGatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("agent_orchestrator_route", r -> r
						.path("/api/v1/support/**")
						.uri("http://localhost:8080"))
				.build();
	}
}

@RestController
class DevTokenController {

	@Value("${app.security.jwt-secret}")
	private String jwtSecret;

	@GetMapping("/dev/token")
	public Map<String, String> generateToken(
			@RequestParam(defaultValue = "CUST_001") String customerId,
			@RequestParam(defaultValue = "PRIVILEGED") String tier) {
		try {
			byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
			JWSSigner signer = new MACSigner(secretBytes);

			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
					.subject(customerId)
					.claim("customer_tier", tier)
					.issueTime(new Date())
					.expirationTime(new Date(System.currentTimeMillis() + 86400000L)) // 1 day
					.build();

			SignedJWT signedJWT = new SignedJWT(
					new JWSHeader(JWSAlgorithm.HS256),
					claimsSet);
			signedJWT.sign(signer);

			String token = signedJWT.serialize();
			return Map.of(
					"accessToken", token,
					"tokenType", "Bearer",
					"curlExample",
					"curl -X POST http://localhost:8000/api/v1/support/chat -H \"Authorization: Bearer " + token
							+ "\" -H \"Content-Type: application/json\" -d '{\"sessionId\":\"sess_01\",\"message\":\"What is my balance?\"}'");
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate dev token", e);
		}
	}
}
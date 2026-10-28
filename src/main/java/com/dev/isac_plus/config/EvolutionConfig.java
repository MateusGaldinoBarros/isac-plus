package com.dev.isac_plus.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(EvolutionProperties.class)
public class EvolutionConfig {

	@Bean
	public RestClient evolutionRestClient(EvolutionProperties props) {
		return configurar(RestClient.builder(), props).build();
	}

	/** Separado para os testes trocarem so o transporte HTTP e manterem o resto igual. */
	public static RestClient.Builder configurar(RestClient.Builder builder, EvolutionProperties props) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		// Sem limite de leitura uma Evolution travada prenderia o processo de download para sempre
		fabrica.setReadTimeout(props.timeout());
		return builder
				.baseUrl(props.url())
				.defaultHeader("apikey", props.apiKey())
				.requestFactory(fabrica);
	}
}

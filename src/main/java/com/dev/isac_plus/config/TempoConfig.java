package com.dev.isac_plus.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Relogio injetavel (os testes avancam o tempo) e agendamento do processamento de imagens. */
@Configuration
@EnableScheduling
public class TempoConfig {

	@Bean
	public Clock relogio() {
		return Clock.systemUTC();
	}
}

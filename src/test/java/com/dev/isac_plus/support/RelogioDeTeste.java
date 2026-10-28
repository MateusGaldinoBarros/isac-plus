package com.dev.isac_plus.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relogio real que os testes podem adiantar (o banco grava datas com o relogio de verdade). */
public class RelogioDeTeste extends Clock {

	private volatile Duration adiantamento = Duration.ZERO;

	public void zerar() {
		adiantamento = Duration.ZERO;
	}

	public void adiantar(Duration tempo) {
		adiantamento = adiantamento.plus(tempo);
	}

	@Override
	public Instant instant() {
		return Instant.now().plus(adiantamento);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}
}

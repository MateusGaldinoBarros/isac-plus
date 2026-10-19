package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dev.isac_plus.dto.AnuncioRequest;
import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.mapper.AnuncioMapper;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.repository.AnuncioRepository;

@ExtendWith(MockitoExtension.class)
class AnuncioServiceTest {

	@Mock
	private AnuncioRepository repository;

	private AnuncioService service;

	@BeforeEach
	void setUp() {
		service = new AnuncioService(repository, new AnuncioMapper());
	}

	@Test
	void listarConverteTodosParaResponse() {
		when(repository.findAll()).thenReturn(List.of(new Anuncio("Bike", "Usada", "1"), new Anuncio("Moto", null, "2")));

		List<AnuncioResponse> resultado = service.listar();

		assertThat(resultado).extracting(AnuncioResponse::titulo).containsExactly("Bike", "Moto");
	}

	@Test
	void buscarPorIdRetornaOAnuncio() {
		when(repository.findById(1L)).thenReturn(Optional.of(new Anuncio("Bike", "Usada", "1")));

		assertThat(service.buscarPorId(1L).titulo()).isEqualTo("Bike");
	}

	@Test
	void buscarPorIdInexistenteLancaExcecao() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscarPorId(99L))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Anuncio com id 99 nao encontrado");
	}

	@Test
	void criarSalvaAEntidadeComOsDadosDoRequest() {
		when(repository.save(any(Anuncio.class))).thenAnswer(inv -> inv.getArgument(0));

		AnuncioResponse resposta = service.criar(new AnuncioRequest("Bike", "Usada", "1"));

		ArgumentCaptor<Anuncio> captor = ArgumentCaptor.forClass(Anuncio.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getTitulo()).isEqualTo("Bike");
		assertThat(resposta.contato()).isEqualTo("1");
	}

	@Test
	void atualizarAlteraOAnuncioExistente() {
		Anuncio existente = new Anuncio("Bike", "Usada", "1");
		when(repository.findById(1L)).thenReturn(Optional.of(existente));
		when(repository.save(existente)).thenReturn(existente);

		AnuncioResponse resposta = service.atualizar(1L, new AnuncioRequest("Moto", "Nova", "2"));

		assertThat(resposta.titulo()).isEqualTo("Moto");
		assertThat(existente.getDescricao()).isEqualTo("Nova");
	}

	@Test
	void atualizarInexistenteNaoSalva() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(99L, new AnuncioRequest("Moto", null, "2")))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void excluirRemoveOAnuncio() {
		Anuncio existente = new Anuncio("Bike", "Usada", "1");
		when(repository.findById(1L)).thenReturn(Optional.of(existente));

		service.excluir(1L);

		verify(repository).delete(existente);
	}

	@Test
	void excluirInexistenteLancaExcecao() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
		verify(repository, never()).delete(any());
	}
}

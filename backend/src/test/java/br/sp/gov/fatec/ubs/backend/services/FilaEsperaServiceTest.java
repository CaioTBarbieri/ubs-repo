package br.sp.gov.fatec.ubs.backend.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FilaEsperaServiceTest {

    private final LocalDateTime horario = LocalDateTime.of(2030, 1, 10, 9, 0);

    @Test
    void atendeNaOrdemDeChegada() {
        FilaEsperaService fila = new FilaEsperaService();

        assertEquals(1, fila.entrar(1L, horario, 10L));
        assertEquals(2, fila.entrar(1L, horario, 20L));
        assertEquals(3, fila.entrar(1L, horario, 30L));

        assertEquals(10L, fila.proximo(1L, horario).orElseThrow());
        assertEquals(20L, fila.proximo(1L, horario).orElseThrow());
        assertEquals(1, fila.tamanho(1L, horario));
    }

    @Test
    void filasSaoIndependentesPorMedicoEHorario() {
        FilaEsperaService fila = new FilaEsperaService();
        fila.entrar(1L, horario, 10L);

        assertEquals(0, fila.tamanho(2L, horario));
        assertEquals(0, fila.tamanho(1L, horario.plusHours(1)));
        assertTrue(fila.proximo(2L, horario).isEmpty());
    }

    @Test
    void naoPermiteMesmoPacienteDuasVezes() {
        FilaEsperaService fila = new FilaEsperaService();
        fila.entrar(1L, horario, 10L);

        assertThrows(IllegalArgumentException.class, () -> fila.entrar(1L, horario, 10L));
    }

    @Test
    void filaVaziaDevolveVazio() {
        FilaEsperaService fila = new FilaEsperaService();
        assertTrue(fila.proximo(1L, horario).isEmpty());
    }
}

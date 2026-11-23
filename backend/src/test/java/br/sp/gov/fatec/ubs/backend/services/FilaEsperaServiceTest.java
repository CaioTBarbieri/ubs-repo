package br.sp.gov.fatec.ubs.backend.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilaEsperaServiceTest {

    private final LocalDate dia = LocalDate.of(2030, 1, 10);

    @Test
    void atendeNaOrdemDeChegada() {
        FilaEsperaService fila = new FilaEsperaService();

        assertEquals(1, fila.entrar(1L, dia, 10L));
        assertEquals(2, fila.entrar(1L, dia, 20L));
        assertEquals(3, fila.entrar(1L, dia, 30L));

        assertEquals(10L, fila.proximo(1L, dia).orElseThrow());
        assertEquals(20L, fila.proximo(1L, dia).orElseThrow());
        assertEquals(1, fila.tamanho(1L, dia));
    }

    @Test
    void filasSaoIndependentesPorMedicoEDia() {
        FilaEsperaService fila = new FilaEsperaService();
        fila.entrar(1L, dia, 10L);

        assertEquals(0, fila.tamanho(2L, dia));
        assertEquals(0, fila.tamanho(1L, dia.plusDays(1)));
        assertTrue(fila.proximo(2L, dia).isEmpty());
    }

    @Test
    void naoPermiteMesmoPacienteDuasVezes() {
        FilaEsperaService fila = new FilaEsperaService();
        fila.entrar(1L, dia, 10L);

        assertThrows(IllegalArgumentException.class, () -> fila.entrar(1L, dia, 10L));
    }

    @Test
    void filaVaziaDevolveVazio() {
        FilaEsperaService fila = new FilaEsperaService();
        assertTrue(fila.proximo(1L, dia).isEmpty());
    }
}

package br.sp.gov.fatec.ubs.backend.services;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Fila de espera de pacientes por um atendimento com um médico em um determinado DIA.
 *
 * Na UBS nem sempre existe horário marcado, por isso a fila é por médico + dia
 * (e não por horário). Funciona igual com ou sem horário definido.
 *
 * Estrutura de dados principal: FILA (Queue, política FIFO).
 * Quem entra primeiro na fila é o primeiro a ser atendido quando uma vaga for liberada.
 * - offer(): entra no final da fila  -> O(1)
 * - poll():  remove o primeiro       -> O(1)
 *
 * Um Map é usado apenas como índice para localizar a fila certa (uma por médico e dia);
 * a ordem de atendimento vem da fila.
 *
 * Observação: a fila fica em memória (não é gravada no banco), então é zerada se a
 * aplicação for reiniciada.
 */
@Service
public class FilaEsperaService {

    private final Map<String, Queue<Long>> filas = new ConcurrentHashMap<>();

    private String chave(Long medicoId, LocalDate data) {
        return medicoId + "|" + data;
    }

    /** Coloca o paciente no final da fila e devolve a posição dele (1 = primeiro). */
    public int entrar(Long medicoId, LocalDate data, Long pacienteId) {
        Queue<Long> fila = filas.computeIfAbsent(chave(medicoId, data), k -> new ConcurrentLinkedQueue<>());
        if (fila.contains(pacienteId)) {
            throw new IllegalArgumentException("Paciente já está na fila de espera deste médico neste dia");
        }
        fila.offer(pacienteId);
        return fila.size();
    }

    /** Remove e devolve o próximo paciente da fila (ou vazio se ninguém espera). */
    public Optional<Long> proximo(Long medicoId, LocalDate data) {
        String chave = chave(medicoId, data);
        Queue<Long> fila = filas.get(chave);
        if (fila == null) {
            return Optional.empty();
        }
        Long pacienteId = fila.poll();
        if (fila.isEmpty()) {
            filas.remove(chave, fila);
        }
        return Optional.ofNullable(pacienteId);
    }

    /** Quantas pessoas estão esperando por esse médico nesse dia. */
    public int tamanho(Long medicoId, LocalDate data) {
        Queue<Long> fila = filas.get(chave(medicoId, data));
        return fila == null ? 0 : fila.size();
    }
}

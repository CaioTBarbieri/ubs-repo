package br.sp.gov.fatec.ubs.backend.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Fila de espera de pacientes para um horário já ocupado de um médico.
 *
 * Estrutura de dados principal: FILA (Queue, política FIFO).
 * Quem entra primeiro na fila é o primeiro a ser agendado quando o horário for liberado.
 * - offer(): entra no final da fila  -> O(1)
 * - poll():  remove o primeiro       -> O(1)
 *
 * Existe uma fila para cada combinação (médico + horário). Um Map é usado apenas como
 * índice para localizar a fila certa; a regra de ordem de atendimento vem da fila.
 *
 * Observação: a fila fica em memória (não é gravada no banco), então é zerada se a
 * aplicação for reiniciada.
 */
@Service
public class FilaEsperaService {

    private final Map<String, Queue<Long>> filas = new ConcurrentHashMap<>();

    private String chave(Long medicoId, LocalDateTime dataHora) {
        return medicoId + "|" + dataHora;
    }

    /** Coloca o paciente no final da fila e devolve a posição dele (1 = primeiro). */
    public int entrar(Long medicoId, LocalDateTime dataHora, Long pacienteId) {
        Queue<Long> fila = filas.computeIfAbsent(chave(medicoId, dataHora), k -> new ConcurrentLinkedQueue<>());
        if (fila.contains(pacienteId)) {
            throw new IllegalArgumentException("Paciente já está na fila de espera deste horário");
        }
        fila.offer(pacienteId);
        return fila.size();
    }

    /** Remove e devolve o próximo paciente da fila (ou vazio se ninguém espera). */
    public Optional<Long> proximo(Long medicoId, LocalDateTime dataHora) {
        String chave = chave(medicoId, dataHora);
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

    /** Quantas pessoas estão esperando por esse horário. */
    public int tamanho(Long medicoId, LocalDateTime dataHora) {
        Queue<Long> fila = filas.get(chave(medicoId, dataHora));
        return fila == null ? 0 : fila.size();
    }
}

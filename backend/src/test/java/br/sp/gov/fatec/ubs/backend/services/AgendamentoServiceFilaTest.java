package br.sp.gov.fatec.ubs.backend.services;

import br.sp.gov.fatec.ubs.backend.model.Agendamento;
import br.sp.gov.fatec.ubs.backend.model.Medico;
import br.sp.gov.fatec.ubs.backend.model.Paciente;
import br.sp.gov.fatec.ubs.backend.repositories.AgendamentoRepository;
import br.sp.gov.fatec.ubs.backend.repositories.MedicoRepository;
import br.sp.gov.fatec.ubs.backend.repositories.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoServiceFilaTest {

    @Mock AgendamentoRepository agendamentoRepository;
    @Mock MedicoRepository medicoRepository;
    @Mock PacienteRepository pacienteRepository;
    @Spy FilaEsperaService filaEsperaService = new FilaEsperaService();
    @InjectMocks AgendamentoService service;

    private final LocalDateTime horario = LocalDateTime.now().plusDays(3).withNano(0);
    private Medico medico;
    private Paciente pacienteA;
    private Paciente pacienteB;
    private Agendamento agendamentoDoA;

    @BeforeEach
    void preparar() {
        medico = mock(Medico.class);
        when(medico.getId()).thenReturn(1L);
        when(medico.isAtivo()).thenReturn(true);
        pacienteA = mock(Paciente.class);
        pacienteB = mock(Paciente.class);

        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(pacienteA));
        when(pacienteRepository.findById(20L)).thenReturn(Optional.of(pacienteB));

        agendamentoDoA = new Agendamento(pacienteA, medico, horario, "Clínico geral");
        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoDoA));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void naoEntraNaFilaSeHorarioEstaLivre() {
        when(agendamentoRepository.countByMedicoAndDataHora(1L, horario)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> service.entrarNaFilaDeEspera(20L, 1L, horario));
    }

    @Test
    void entraNaFilaQuandoHorarioOcupado() {
        when(agendamentoRepository.countByMedicoAndDataHora(1L, horario)).thenReturn(1L);

        assertEquals(1, service.entrarNaFilaDeEspera(20L, 1L, horario));
        assertEquals(1, service.tamanhoFilaEspera(1L, horario));
    }

    @Test
    void cancelamentoAgendaOPrimeiroDaFila() {
        when(agendamentoRepository.countByMedicoAndDataHora(1L, horario)).thenReturn(1L);
        service.entrarNaFilaDeEspera(20L, 1L, horario);

        service.cancelarAgendamento(100L);

        ArgumentCaptor<Agendamento> salvos = ArgumentCaptor.forClass(Agendamento.class);
        verify(agendamentoRepository, times(2)).save(salvos.capture());
        Agendamento promovido = salvos.getAllValues().get(1);
        assertSame(pacienteB, promovido.getPaciente());
        assertEquals(horario, promovido.getDataHoraConsulta());
        assertEquals(Agendamento.StatusAgendamento.AGENDADO, promovido.getStatus());
        assertEquals(0, service.tamanhoFilaEspera(1L, horario));
    }

    @Test
    void cancelamentoSemFilaNaoCriaNovoAgendamento() {
        service.cancelarAgendamento(100L);

        verify(agendamentoRepository, times(1)).save(any(Agendamento.class));
    }
}

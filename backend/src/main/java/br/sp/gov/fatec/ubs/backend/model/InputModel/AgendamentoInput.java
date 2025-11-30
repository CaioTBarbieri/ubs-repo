package br.sp.gov.fatec.ubs.backend.model.InputModel;

import java.time.LocalDateTime;
 
import com.fasterxml.jackson.annotation.JsonFormat;

import br.sp.gov.fatec.ubs.backend.model.Agendamento.StatusAgendamento;

public class AgendamentoInput {

    private Long id;
    
    private Long pacienteId; 
    
    private Long medicoId; 
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraConsulta;
    
    private String tipoConsulta;
    
    private String observacoes;

    private StatusAgendamento status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

    public Long getMedicoId() { return medicoId; }
    public void setMedicoId(Long medicoId) { this.medicoId = medicoId; }

    public LocalDateTime getDataHoraConsulta() { return dataHoraConsulta; }
    public void setDataHoraConsulta(LocalDateTime dataHoraConsulta) { this.dataHoraConsulta = dataHoraConsulta; }

    public String getTipoConsulta() { return tipoConsulta; }
    public void setTipoConsulta(String tipoConsulta) { this.tipoConsulta = tipoConsulta; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
}
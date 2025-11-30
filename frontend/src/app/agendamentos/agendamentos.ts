import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Observable, map, startWith } from 'rxjs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MAT_DATE_LOCALE, MatOptionModule } from '@angular/material/core';
import { MomentDateAdapter, MAT_MOMENT_DATE_ADAPTER_OPTIONS } from '@angular/material-moment-adapter';
import { DateAdapter, MAT_DATE_FORMATS } from '@angular/material/core';
import { HttpClient } from '@angular/common/http'; 

import { AgendamentoService } from '../service/agendamento.service';
import { PacienteService } from '../service/paciente.service';
import { MedicoService } from '../service/medico.service';
import { Agendamento, AgendamentoRequest, StatusAgendamento, TIPOS_CONSULTA } from '../model/agendamento.model';
import { Paciente } from '../model/paciente.model';
import { Medico } from '../model/medico.model';

export const MY_FORMATS = {
  parse: { dateInput: 'DD/MM/YYYY' },
  display: {
    dateInput: 'DD/MM/YYYY',
    monthYearLabel: 'MMMM YYYY',
    dateA11yLabel: 'LL',
    monthYearA11yLabel: 'MMMM YYYY',
  },
};

@Component({
  selector: 'app-agendamentos',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, 
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatOptionModule, 
    MatAutocompleteModule, MatButtonModule, MatTableModule, MatIconModule, MatChipsModule,
    MatDatepickerModule, MatSnackBarModule, MatPaginatorModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
    { provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS] },
    { provide: MAT_DATE_FORMATS, useValue: MY_FORMATS },
  ],
  templateUrl: './agendamentos.html',
  styleUrls: ['./agendamentos.scss']
})
export class Agendamentos implements OnInit, AfterViewInit {
  agendamentoForm: FormGroup;
  filtroForm: FormGroup;
  
  pacientes: Paciente[] = [];
  medicos: Medico[] = [];
  pacientesFiltrados!: Observable<Paciente[]>;
  medicosFiltrados: Medico[] = [];
  
  tiposConsulta = TIPOS_CONSULTA;
  statusOptions = Object.values(StatusAgendamento);
  
  idEdicao: number | null = null; 

  dataSource = new MatTableDataSource<Agendamento>();
  displayedColumns = [ 'paciente', 'medico', 'dataHora', 'tipoConsulta', 'status', 'acoes' ];

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private agendamentoService: AgendamentoService,
    private pacienteService: PacienteService,
    private medicoService: MedicoService,
    private snackBar: MatSnackBar
  ) {
    this.agendamentoForm = this.fb.group({
      id: [null], 
      pacienteInput: [''], 
      pacienteId: ['', Validators.required],
      tipoConsulta: ['', Validators.required], 
      medicoId: ['', Validators.required],
      dataConsulta: ['', Validators.required], 
      horaConsulta: [''], 
      observacoes: ['']
    });

    this.filtroForm = this.fb.group({
      termoBusca: [''], dataFiltro: [''], statusFiltro: ['']
    });
  }

  ngOnInit(): void {
    this.carregarDados();
    this.pacientesFiltrados = this.agendamentoForm.get('pacienteInput')!.valueChanges.pipe(
      startWith(''),
      map(value => {
        const name = typeof value === 'string' ? value : value?.nomeCompleto;
        return name ? this._filtrarPacientes(name) : this.pacientes.slice();
      })
    );
    this.agendamentoForm.get('tipoConsulta')!.valueChanges.subscribe(tipo => {
      this._filtrarMedicosPorTipo(tipo);
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  // --- LÓGICA DE COMPARAÇÃO (CORREÇÃO DO ERRO NG5002) ---
  compararMedicos(o1: any, o2: any): boolean {
    return o1 && o2 ? o1.id === o2.id : o1 === o2;
  }

  // --- LÓGICA DE EDIÇÃO ---
  iniciarEdicao(id: number): void {
    this.idEdicao = id;
  }

  cancelarEdicao(): void {
    this.idEdicao = null;
    this.carregarAgendamentos();
  }

  salvarEdicao(ag: Agendamento): void {
    const dataObj = new Date(ag.dataHoraConsulta);
    const dataISO = dataObj.toISOString().slice(0, 10);
    const dataHoraEnvio = `${dataISO}T00:00:00`; 

    const request = {
      pacienteId: ag.paciente.id,
      medicoId: ag.medico?.id,
      dataHoraConsulta: dataHoraEnvio,
      tipoConsulta: ag.tipoConsulta,
      observacoes: ag.observacoes,
      status: ag.status
    };

     const requestEditAgendamento = {
      id: ag.id,
      pacienteId: ag.paciente.id,
      medicoId: ag.medico?.id,
      dataHoraConsulta: dataHoraEnvio,
      tipoConsulta: ag.tipoConsulta,
      status: ag.status
    };

    const url = `http://localhost:8090/api/agendamentos`;

    this.http.put(url, requestEditAgendamento).subscribe({
      next: () => {
        this.mostrarMensagem('Atualizado com sucesso!');
        this.idEdicao = null;
        this.carregarAgendamentos();
      },
      error: (err) => {
        console.error(err);
        this.mostrarMensagem('Erro ao salvar edição.');
      }
    });
  }

  filtrarMedicosPorLinha(tipoConsulta: string): Medico[] {
    if (!tipoConsulta) return this.medicos;
    const especialidadeChave = tipoConsulta.replace('Consulta ', '').toLowerCase();
    return this.medicos.filter(m => {
       const esp = Array.isArray(m.especialidade) ? m.especialidade.join(' ') : m.especialidade;
       return esp && esp.toLowerCase().includes(especialidadeChave);
    });
  }

  // --- CARREGAMENTO ---
  private carregarDados(): void {
    this.carregarAgendamentos();
    this.pacienteService.listarPacientes().subscribe({ next: pcs => this.pacientes = pcs });
    this.medicoService.listarTodos().subscribe({ 
      next: mds => {
        this.medicos = mds;
        this.medicosFiltrados = [...mds];
      } 
    });
  }

  private carregarAgendamentos(): void {
    this.agendamentoService.listarTodos().subscribe({
      next: ags => {
        this.dataSource.data = ags;
        this.configurarFiltro();
      },
      error: () => this.mostrarMensagem('Erro ao carregar')
    });
  }

  // --- SUBMIT ---
  onSubmit(): void {
    if (this.agendamentoForm.invalid) return;
    const req = this._montarRequest();
    const temHora = !!this.agendamentoForm.get('horaConsulta')?.value;

    if (temHora) {
      this.agendamentoService.verificarDisponibilidade(req.medicoId, req.dataHoraConsulta).subscribe({
        next: (res: any) => { if (res.disponivel) this.salvarNovo(req); else this.mostrarMensagem('Horário indisponível.'); },
        error: () => this.mostrarMensagem('Erro ao verificar.')
      });
    } else {
      this.salvarNovo(req);
    }
  }

  private salvarNovo(req: AgendamentoRequest): void {
    this.agendamentoService.criarAgendamento(req).subscribe({
      next: () => {
        this.mostrarMensagem('Criado com sucesso!');
        this.agendamentoForm.reset();
        Object.keys(this.agendamentoForm.controls).forEach(key => this.agendamentoForm.get(key)?.setErrors(null));
        this.carregarAgendamentos();
      },
      error: () => this.mostrarMensagem('Erro ao criar.')
    });
  }

  private _montarRequest(): AgendamentoRequest {
    const f = this.agendamentoForm.value;
    const dataSelecionada = new Date(f.dataConsulta);
    const hora = f.horaConsulta;
    dataSelecionada.setMinutes(dataSelecionada.getMinutes() - dataSelecionada.getTimezoneOffset());
    const dataISO = dataSelecionada.toISOString().slice(0, 10);
    
    let dataHoraParaBackend = null;
    if (hora) { dataHoraParaBackend = `${dataISO}T${hora}`; } 
    else { dataHoraParaBackend = `${dataISO}T00:00:00`; }

    return {
      pacienteId: f.pacienteId,
      medicoId: f.medicoId,
      dataHoraConsulta: dataHoraParaBackend!,
      tipoConsulta: f.tipoConsulta,
      observacoes: f.observacoes
    };
  }

  configurarFiltro(): void {
    this.dataSource.filterPredicate = (data: Agendamento, filter: string): boolean => {
      const searchString = JSON.parse(filter);
      const termoBusca = searchString.termoBusca?.toLowerCase() || '';
      const statusFiltro = searchString.statusFiltro;
      const dataFiltro = searchString.dataFiltro ? new Date(searchString.dataFiltro) : null;
      if (dataFiltro) dataFiltro.setMinutes(dataFiltro.getMinutes() + dataFiltro.getTimezoneOffset());

      const matchNome = termoBusca === '' || (data.paciente?.nomeCompleto?.toLowerCase().includes(termoBusca) || data.medico?.nomeCompleto?.toLowerCase().includes(termoBusca));
      const matchStatus = !statusFiltro || data.status === statusFiltro;
      const matchData = !dataFiltro || new Date(data.dataHoraConsulta).toDateString() === dataFiltro.toDateString();
      return matchNome && matchStatus && matchData;
    };
  }
  aplicarFiltro(): void { this.dataSource.filter = JSON.stringify(this.filtroForm.value); if (this.dataSource.paginator) this.dataSource.paginator.firstPage(); }
  limparFiltro(): void { this.filtroForm.reset({ termoBusca: '', dataFiltro: '', statusFiltro: '' }); this.aplicarFiltro(); }
  
  displayPaciente(paciente: Paciente): string { return paciente && paciente.nomeCompleto ? paciente.nomeCompleto : ''; }
  private _filtrarPacientes(valor: string): Paciente[] { const v = valor.toLowerCase(); return this.pacientes.filter(p => p.nomeCompleto.toLowerCase().includes(v) || p.cpf.replace(/\D/g, '').includes(v.replace(/\D/g, ''))); }
  onPacienteSelecionado(p: Paciente): void { this.agendamentoForm.patchValue({ pacienteId: p.id, pacienteInput: p }); }
  private _filtrarMedicosPorTipo(tipo: string): void {
    if (!tipo || tipo === 'Consulta Clínica Geral' || tipo === 'Exame de Rotina' || tipo === 'Consulta de Retorno') { this.medicosFiltrados = [...this.medicos]; } else {
      const especialidadeChave = tipo.replace('Consulta ', '').toLowerCase();
      this.medicosFiltrados = this.medicos.filter(medico => { if (typeof medico.especialidade === 'string') { return medico.especialidade.toLowerCase().includes(especialidadeChave); } return false; });
    }
    const medicoIdAtual = this.agendamentoForm.get('medicoId')!.value;
    if (!this.medicosFiltrados.some(m => m.id === medicoIdAtual)) { this.agendamentoForm.patchValue({ medicoId: null }); }
  }
  formatarEspecialidade(m: Medico): string { const esp = m.especialidade; return Array.isArray(esp) ? esp.join(', ') : esp; }
  formatarDataHora(s: string): string { return new Date(s).toLocaleString('pt-BR'); }
  getStatusColor(status: StatusAgendamento): string {
    switch (status) { case StatusAgendamento.AGENDADO: return 'primary'; case StatusAgendamento.CONFIRMADO: return 'accent'; case StatusAgendamento.CANCELADO: return 'warn'; case StatusAgendamento.REALIZADO: return 'primary'; case StatusAgendamento.FALTOU: return 'warn'; default: return 'primary'; }
  }
  
  cancelarAgendamento(id: number): void { if (!confirm('Tem certeza?')) return; this.agendamentoService.cancelarAgendamento(id).subscribe({ next: () => { this.mostrarMensagem('Cancelado'); this.carregarAgendamentos(); }, error: () => this.mostrarMensagem('Erro ao cancelar') }); }
  confirmarAgendamento(id: number): void { this.agendamentoService.confirmarAgendamento(id).subscribe({ next: () => { this.mostrarMensagem('Confirmado'); this.carregarAgendamentos(); }, error: () => this.mostrarMensagem('Erro ao confirmar') }); }
  
  private mostrarMensagem(msg: string): void { this.snackBar.open(msg, 'Fechar', { duration: 4000, horizontalPosition: 'center', verticalPosition: 'top' }); }
}
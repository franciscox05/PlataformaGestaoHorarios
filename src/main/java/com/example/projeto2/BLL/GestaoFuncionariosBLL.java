package com.example.projeto2.BLL;

import com.example.projeto2.Enums.EstadoUtilizador;
import com.example.projeto2.Modules.Cargo;
import com.example.projeto2.Modules.Loja;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.CargoRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class GestaoFuncionariosBLL {

    private static final Set<String> CARGOS_COM_GESTAO_FUNCIONARIOS = Set.of("gerente", "subgerente");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final CargoRepository cargoRepository;
    private final SegurancaBLL segurancaBLL;
    private final AuditoriaBLL auditoriaBLL;
    private final SessaoBLL sessaoBLL;

    public GestaoFuncionariosBLL(LojautilizadorRepository lojautilizadorRepository,
                                 UtilizadorRepository utilizadorRepository,
                                 CargoRepository cargoRepository,
                                 SegurancaBLL segurancaBLL,
                                 AuditoriaBLL auditoriaBLL,
                                 SessaoBLL sessaoBLL) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.cargoRepository = cargoRepository;
        this.segurancaBLL = segurancaBLL;
        this.auditoriaBLL = auditoriaBLL;
        this.sessaoBLL = sessaoBLL;
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeGerirFuncionarios(Integer idUtilizador) {
        if (idUtilizador == null) {
            return false;
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(Cargo::getTipo)
                .map(tipo -> tipo != null ? tipo.toLowerCase() : "")
                .filter(CARGOS_COM_GESTAO_FUNCIONARIOS::contains)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public GestaoFuncionariosResumo obterResumo(Integer idUtilizadorGestor) {
        Lojautilizador ligacaoGestor = obterLigacaoAtivaComPermissao(idUtilizadorGestor);
        Loja loja = ligacaoGestor.getIdLoja();

        List<CargoResumo> cargosDisponiveis = cargoRepository.findAllByOrderByNomeAsc().stream()
                .map(cargo -> new CargoResumo(
                        cargo.getId(),
                        valorOuTraco(cargo.getNome()),
                        cargo.getTipo()
                ))
                .toList();

        List<ColaboradorResumo> colaboradores = construirResumoColaboradores(loja.getId());

        return new GestaoFuncionariosResumo(
                loja.getId(),
                valorOuTraco(loja.getNome()),
                valorOuTraco(loja.getLocalizacao()),
                ligacaoGestor.getIdCargo() != null ? valorOuTraco(ligacaoGestor.getIdCargo().getNome()) : "-",
                cargosDisponiveis,
                colaboradores
        );
    }

    @Transactional
    public Integer guardarColaborador(Integer idUtilizadorGestor, ColaboradorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Os dados do colaborador sao obrigatorios.");
        }

        Lojautilizador ligacaoGestor = obterLigacaoAtivaComPermissao(idUtilizadorGestor);
        Cargo cargoSelecionado = obterCargo(request.idCargo());
        String nome = normalizarNome(request.nome());
        String email = normalizarEmail(request.email());
        String telemovel = normalizarTelemovel(request.telemovel());
        String estado = normalizarEstado(request.estado());

        if (request.idUtilizador() == null) {
            String password = segurancaBLL.prepararPasswordParaPersistencia(request.password(), true);
            Integer idCriado = criarColaborador(ligacaoGestor.getIdLoja(), cargoSelecionado, nome, email, telemovel, password, estado);
            auditoriaBLL.registarEventoSensivel(
                    "colaborador_criado",
                    ligacaoGestor.getIdUtilizador(),
                    sessaoBLL.obterIdentificadorSessao(),
                    "gestao_funcionarios",
                    "Colaborador " + email + " criado na loja " + valorOuTraco(ligacaoGestor.getIdLoja().getNome()) + "."
            );
            return idCriado;
        }

        String password = segurancaBLL.prepararPasswordParaPersistencia(request.password(), false);
        Integer idAtualizado = atualizarColaborador(
                ligacaoGestor,
                request.idUtilizador(),
                cargoSelecionado,
                nome,
                email,
                telemovel,
                password,
                estado
        );
        auditoriaBLL.registarEventoSensivel(
                "colaborador_atualizado",
                ligacaoGestor.getIdUtilizador(),
                sessaoBLL.obterIdentificadorSessao(),
                "gestao_funcionarios",
                "Colaborador " + email + " atualizado na loja " + valorOuTraco(ligacaoGestor.getIdLoja().getNome()) + "."
        );
        return idAtualizado;
    }

    @Transactional
    public void desativarColaborador(Integer idUtilizadorGestor, Integer idColaborador) {
        Lojautilizador ligacaoGestor = obterLigacaoAtivaComPermissao(idUtilizadorGestor);

        if (idColaborador == null) {
            throw new IllegalArgumentException("Seleciona um colaborador valido.");
        }

        if (Objects.equals(idUtilizadorGestor, idColaborador)) {
            throw new IllegalArgumentException("Usa o teu perfil para gerir o teu proprio registo.");
        }

        Utilizador colaborador = obterUtilizadorPersistido(idColaborador);
        Lojautilizador ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizadorAndIdLoja(
                        idColaborador,
                        ligacaoGestor.getIdLoja().getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Este colaborador ja nao tem um registo ativo nesta loja."));

        desativarLigacaoDaLoja(ligacaoGestor.getIdLoja(), colaborador, ligacaoAtiva);
        utilizadorRepository.save(colaborador);
        auditoriaBLL.registarEventoSensivel(
                "colaborador_desativado",
                ligacaoGestor.getIdUtilizador(),
                sessaoBLL.obterIdentificadorSessao(),
                "gestao_funcionarios",
                "Colaborador " + valorOuTraco(colaborador.getEmail()) + " desativado na loja " + valorOuTraco(ligacaoGestor.getIdLoja().getNome()) + "."
        );
    }

    @Transactional
    public void resetarPasswordColaborador(Integer idUtilizadorGestor, Integer idColaborador, String novaPassword) {
        if (idColaborador == null) {
            throw new IllegalArgumentException("Seleciona um colaborador valido.");
        }
        if (Objects.equals(idUtilizadorGestor, idColaborador)) {
            throw new IllegalArgumentException("Usa o teu perfil para alterar a tua propria password.");
        }

        Lojautilizador ligacaoGestor = obterLigacaoAtivaComPermissao(idUtilizadorGestor);

        // Verifica que o colaborador pertence à loja gerida
        lojautilizadorRepository.findHistoricoByIdLojaAndIdUtilizador(
                ligacaoGestor.getIdLoja().getId(), idColaborador)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Este colaborador nao pertence a loja que estas a gerir."));

        Utilizador colaborador = obterUtilizadorPersistido(idColaborador);
        String passwordHash = segurancaBLL.prepararPasswordParaPersistencia(novaPassword, true);
        colaborador.setPasswordHash(passwordHash);
        utilizadorRepository.save(colaborador);

        auditoriaBLL.registarEventoSensivel(
                "alteracao_password",
                ligacaoGestor.getIdUtilizador(),
                sessaoBLL.obterIdentificadorSessao(),
                "gestao_funcionarios",
                "Password do colaborador " + valorOuTraco(colaborador.getEmail()) + " redefinida pelo gestor."
        );
    }

    private Integer criarColaborador(Loja loja,
                                     Cargo cargoSelecionado,
                                     String nome,
                                     String email,
                                     String telemovel,
                                     String password,
                                     String estado) {
        if (!"ativo".equals(estado)) {
            throw new IllegalArgumentException("Novos colaboradores devem ser criados com estado ativo.");
        }

        if (password == null) {
            throw new IllegalArgumentException("Indica uma password inicial para o novo colaborador.");
        }

        if (utilizadorRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Este email ja esta registado noutra conta.");
        }

        if (telemovel != null && utilizadorRepository.existsByTelemovel(telemovel)) {
            throw new IllegalArgumentException("Este telemovel ja esta registado noutra conta.");
        }

        Utilizador colaborador = new Utilizador();
        colaborador.setNome(nome);
        colaborador.setEmail(email);
        colaborador.setTelemovel(telemovel);
        colaborador.setPasswordHash(password);
        colaborador.setEstado(EstadoUtilizador.ativo);
        Utilizador colaboradorGuardado = utilizadorRepository.save(colaborador);

        criarNovaLigacaoLoja(colaboradorGuardado, loja, cargoSelecionado);
        return colaboradorGuardado.getId();
    }

    private Integer atualizarColaborador(Lojautilizador ligacaoGestor,
                                         Integer idColaborador,
                                         Cargo cargoSelecionado,
                                         String nome,
                                         String email,
                                         String telemovel,
                                         String password,
                                         String estado) {
        if (Objects.equals(ligacaoGestor.getIdUtilizador().getId(), idColaborador)) {
            throw new IllegalArgumentException("Usa o teu perfil para gerir o teu proprio registo.");
        }

        Utilizador colaborador = obterUtilizadorPersistido(idColaborador);
        Integer idLoja = ligacaoGestor.getIdLoja().getId();

        List<Lojautilizador> historicoNaLoja = lojautilizadorRepository.findHistoricoByIdLojaAndIdUtilizador(idLoja, idColaborador);
        if (historicoNaLoja.isEmpty()) {
            throw new IllegalArgumentException("Este colaborador nao pertence a loja que estas a gerir.");
        }

        Optional<Lojautilizador> ligacaoAtivaNaLoja = historicoNaLoja.stream()
                .filter(ligacao -> ligacao.getDataFim() == null)
                .findFirst();

        Optional<Lojautilizador> ligacaoAtivaGlobal = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idColaborador);
        if (ligacaoAtivaNaLoja.isEmpty()
                && ligacaoAtivaGlobal.isPresent()
                && !Objects.equals(ligacaoAtivaGlobal.get().getIdLoja().getId(), idLoja)) {
            throw new IllegalArgumentException("Este colaborador tem uma ligacao ativa noutra loja e nao pode ser gerido a partir daqui.");
        }

        if (utilizadorRepository.existsByEmailIgnoreCaseAndIdNot(email, idColaborador)) {
            throw new IllegalArgumentException("Este email ja esta registado noutra conta.");
        }

        if (telemovel != null && utilizadorRepository.existsByTelemovelAndIdNot(telemovel, idColaborador)) {
            throw new IllegalArgumentException("Este telemovel ja esta registado noutra conta.");
        }

        colaborador.setNome(nome);
        colaborador.setEmail(email);
        colaborador.setTelemovel(telemovel);
        if (password != null) {
            colaborador.setPasswordHash(password);
        }

        if ("ativo".equals(estado)) {
            ativarOuAtualizarColaborador(
                    ligacaoGestor.getIdLoja(),
                    colaborador,
                    cargoSelecionado,
                    ligacaoAtivaNaLoja,
                    ligacaoAtivaGlobal
            );
            colaborador.setEstado(EstadoUtilizador.ativo);
        } else {
            manterOuDesativarColaborador(
                    ligacaoGestor.getIdLoja(),
                    colaborador,
                    cargoSelecionado,
                    historicoNaLoja,
                    ligacaoAtivaNaLoja
            );
        }

        utilizadorRepository.save(colaborador);
        return colaborador.getId();
    }

    private void ativarOuAtualizarColaborador(Loja loja,
                                              Utilizador colaborador,
                                              Cargo cargoSelecionado,
                                              Optional<Lojautilizador> ligacaoAtivaNaLoja,
                                              Optional<Lojautilizador> ligacaoAtivaGlobal) {
        if (ligacaoAtivaNaLoja.isPresent()) {
            Lojautilizador ligacaoAtual = ligacaoAtivaNaLoja.get();
            validarLojaNaoFicaSemGestor(loja.getId(), ligacaoAtual.getIdCargo(), cargoSelecionado, false);
            ligacaoAtual.setIdCargo(cargoSelecionado);
            lojautilizadorRepository.save(ligacaoAtual);
            return;
        }

        if (ligacaoAtivaGlobal.isPresent()
                && !Objects.equals(ligacaoAtivaGlobal.get().getIdLoja().getId(), loja.getId())) {
            throw new IllegalArgumentException("Este colaborador ja tem uma ligacao ativa noutra loja.");
        }

        criarNovaLigacaoLoja(colaborador, loja, cargoSelecionado);
    }

    private void manterOuDesativarColaborador(Loja loja,
                                              Utilizador colaborador,
                                              Cargo cargoSelecionado,
                                              List<Lojautilizador> historicoNaLoja,
                                              Optional<Lojautilizador> ligacaoAtivaNaLoja) {
        if (ligacaoAtivaNaLoja.isPresent()) {
            desativarLigacaoDaLoja(loja, colaborador, ligacaoAtivaNaLoja.get());
            return;
        }

        Lojautilizador ultimaLigacao = historicoNaLoja.get(0);
        Integer idCargoAtual = ultimaLigacao.getIdCargo() != null ? ultimaLigacao.getIdCargo().getId() : null;
        if (!Objects.equals(idCargoAtual, cargoSelecionado.getId())) {
            throw new IllegalArgumentException("Para alterar o cargo de um colaborador inativo, ativa-o novamente primeiro.");
        }

        colaborador.setEstado(EstadoUtilizador.inativo);
    }

    private void desativarLigacaoDaLoja(Loja loja, Utilizador colaborador, Lojautilizador ligacaoAtiva) {
        validarLojaNaoFicaSemGestor(loja.getId(), ligacaoAtiva.getIdCargo(), ligacaoAtiva.getIdCargo(), true);

        long ligacoesAtivas = lojautilizadorRepository.countByIdUtilizadorIdAndDataFimIsNull(colaborador.getId());

        ligacaoAtiva.setDataFim(LocalDate.now());
        lojautilizadorRepository.save(ligacaoAtiva);

        colaborador.setEstado(ligacoesAtivas <= 1 ? EstadoUtilizador.inativo : EstadoUtilizador.ativo);
    }

    private void validarLojaNaoFicaSemGestor(Integer idLoja, Cargo cargoAtual, Cargo cargoNovo, boolean vaiFicarInativo) {
        boolean cargoAtualGerencia = cargoAtual != null
                && cargoAtual.getTipo() != null
                && CARGOS_COM_GESTAO_FUNCIONARIOS.contains(cargoAtual.getTipo().toLowerCase());

        boolean cargoNovoGerencia = cargoNovo != null
                && cargoNovo.getTipo() != null
                && CARGOS_COM_GESTAO_FUNCIONARIOS.contains(cargoNovo.getTipo().toLowerCase());

        if (!cargoAtualGerencia) {
            return;
        }

        if (!vaiFicarInativo && cargoNovoGerencia) {
            return;
        }

        long totalGestoresAtivos = lojautilizadorRepository.countByIdLojaIdAndIdCargoTipoInAndDataFimIsNull(
                idLoja,
                CARGOS_COM_GESTAO_FUNCIONARIOS
        );

        if (totalGestoresAtivos <= 1) {
            throw new IllegalArgumentException("A loja tem de manter pelo menos um gerente ou subgerente ativo.");
        }
    }

    private void criarNovaLigacaoLoja(Utilizador colaborador, Loja loja, Cargo cargoSelecionado) {
        Lojautilizador novaLigacao = new Lojautilizador();
        novaLigacao.setIdUtilizador(colaborador);
        novaLigacao.setIdLoja(loja);
        novaLigacao.setIdCargo(cargoSelecionado);
        novaLigacao.setDataInicio(LocalDate.now());
        novaLigacao.setDataFim(null);
        lojautilizadorRepository.save(novaLigacao);
    }

    private List<ColaboradorResumo> construirResumoColaboradores(Integer idLoja) {
        Map<Integer, Lojautilizador> registosPorColaborador = new LinkedHashMap<>();

        for (Lojautilizador ligacao : lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja)) {
            if (ligacao.getIdUtilizador() == null || ligacao.getIdUtilizador().getId() == null) {
                continue;
            }

            Integer idColaborador = ligacao.getIdUtilizador().getId();
            registosPorColaborador.merge(idColaborador, ligacao, this::selecionarLigacaoMaisRelevante);
        }

        return registosPorColaborador.values().stream()
                .sorted(Comparator.comparing(
                        ligacao -> valorOuTraco(ligacao.getIdUtilizador() != null ? ligacao.getIdUtilizador().getNome() : null),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(this::criarResumoColaborador)
                .toList();
    }

    private Lojautilizador selecionarLigacaoMaisRelevante(Lojautilizador atual, Lojautilizador candidato) {
        Comparator<Lojautilizador> comparador = Comparator
                .comparing((Lojautilizador ligacao) -> ligacao.getDataFim() == null ? 1 : 0)
                .thenComparing(Lojautilizador::getDataInicio, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Lojautilizador::getDataFim, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Lojautilizador::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        return comparador.compare(atual, candidato) >= 0 ? atual : candidato;
    }

    private ColaboradorResumo criarResumoColaborador(Lojautilizador ligacao) {
        Utilizador colaborador = ligacao.getIdUtilizador();
        Cargo cargo = ligacao.getIdCargo();

        return new ColaboradorResumo(
                colaborador.getId(),
                valorOuTraco(colaborador.getNome()),
                valorOuTraco(colaborador.getEmail()),
                valorOuTraco(colaborador.getTelemovel()),
                cargo != null ? cargo.getId() : null,
                cargo != null ? valorOuTraco(cargo.getNome()) : "-",
                cargo != null ? cargo.getTipo() : null,
                calcularEstadoNaLoja(colaborador, ligacao),
                ligacao.getDataInicio(),
                ligacao.getDataFim()
        );
    }

    private String calcularEstadoNaLoja(Utilizador colaborador, Lojautilizador ligacao) {
        boolean utilizadorAtivo = colaborador.getEstado() == EstadoUtilizador.ativo;
        boolean ligacaoAtiva = ligacao.getDataFim() == null;
        return utilizadorAtivo && ligacaoAtiva ? "ativo" : "inativo";
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizadorGestor) {
        if (idUtilizadorGestor == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        Lojautilizador ligacaoGestor = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizadorGestor)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa a uma loja."));

        String tipoCargo = ligacaoGestor.getIdCargo() != null && ligacaoGestor.getIdCargo().getTipo() != null
                ? ligacaoGestor.getIdCargo().getTipo().toLowerCase()
                : "";

        if (!CARGOS_COM_GESTAO_FUNCIONARIOS.contains(tipoCargo)) {
            throw new IllegalArgumentException("Nao tens permissao para gerir colaboradores.");
        }

        return ligacaoGestor;
    }

    private Cargo obterCargo(Integer idCargo) {
        if (idCargo == null) {
            throw new IllegalArgumentException("Seleciona um cargo valido para o colaborador.");
        }

        return cargoRepository.findById(idCargo)
                .orElseThrow(() -> new IllegalArgumentException("Foi selecionado um cargo invalido."));
    }

    private Utilizador obterUtilizadorPersistido(Integer idUtilizador) {
        return utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar o colaborador selecionado."));
    }

    private String normalizarNome(String nome) {
        String nomeNormalizado = normalizarTexto(nome);

        if (nomeNormalizado == null) {
            throw new IllegalArgumentException("O nome do colaborador e obrigatorio.");
        }

        if (nomeNormalizado.length() < 3) {
            throw new IllegalArgumentException("O nome deve ter pelo menos 3 caracteres.");
        }

        if (nomeNormalizado.length() > 100) {
            throw new IllegalArgumentException("O nome nao pode ter mais de 100 caracteres.");
        }

        return nomeNormalizado;
    }

    private String normalizarEmail(String email) {
        String emailNormalizado = normalizarTexto(email);

        if (emailNormalizado == null) {
            throw new IllegalArgumentException("O email do colaborador e obrigatorio.");
        }

        if (emailNormalizado.length() > 150) {
            throw new IllegalArgumentException("O email nao pode ter mais de 150 caracteres.");
        }

        if (!EMAIL_PATTERN.matcher(emailNormalizado).matches()) {
            throw new IllegalArgumentException("Indica um email valido.");
        }

        return emailNormalizado.toLowerCase();
    }

    private String normalizarTelemovel(String telemovel) {
        String telemovelNormalizado = normalizarTexto(telemovel);

        if (telemovelNormalizado == null) {
            return null;
        }

        if (!telemovelNormalizado.matches("\\d{9}")) {
            throw new IllegalArgumentException("O telemovel deve ter exatamente 9 digitos.");
        }

        return telemovelNormalizado;
    }

    private String normalizarEstado(String estado) {
        String estadoNormalizado = normalizarTexto(estado);

        if (estadoNormalizado == null) {
            return "ativo";
        }

        if (!"ativo".equalsIgnoreCase(estadoNormalizado) && !"inativo".equalsIgnoreCase(estadoNormalizado)) {
            throw new IllegalArgumentException("Foi indicado um estado invalido para o colaborador.");
        }

        return estadoNormalizado.toLowerCase();
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    public record GestaoFuncionariosResumo(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            String cargoGestor,
            List<CargoResumo> cargosDisponiveis,
            List<ColaboradorResumo> colaboradores
    ) {
    }

    public record CargoResumo(
            Integer idCargo,
            String nome,
            String tipo
    ) {
    }

    public record ColaboradorResumo(
            Integer idUtilizador,
            String nome,
            String email,
            String telemovel,
            Integer idCargo,
            String cargoNome,
            String cargoTipo,
            String estado,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
    }

    public record ColaboradorRequest(
            Integer idUtilizador,
            String nome,
            String email,
            String telemovel,
            String password,
            Integer idCargo,
            String estado
    ) {
    }
}

package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.UtilizadorApiModels.CriarUtilizadorRequest;
import com.example.projeto2.API.Modules.UtilizadorApiModels.UtilizadorResponse;
import com.example.projeto2.API.Services.GestaoFuncionariosService;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilizadoresApiService {

    private final GestaoFuncionariosService gestaoFuncionariosBLL;
    private final UtilizadorRepository utilizadorRepository;

    public UtilizadoresApiService(GestaoFuncionariosService gestaoFuncionariosBLL,
                                  UtilizadorRepository utilizadorRepository) {
        this.gestaoFuncionariosBLL = gestaoFuncionariosBLL;
        this.utilizadorRepository = utilizadorRepository;
    }

    @Transactional(readOnly = true)
    public UtilizadorResponse obterUtilizador(Integer idUtilizador) {
        Utilizador utilizador = utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador nao encontrado."));

        return toResponse(utilizador);
    }

    @Transactional
    public UtilizadorResponse criarUtilizador(Integer idGestor, CriarUtilizadorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Os dados do utilizador sao obrigatorios.");
        }

        Integer idCriado = gestaoFuncionariosBLL.guardarColaborador(
                idGestor,
                new GestaoFuncionariosService.ColaboradorRequest(
                        null,
                        request.nome(),
                        request.email(),
                        request.telemovel(),
                        request.password(),
                        request.idCargo(),
                        request.estado()
                )
        );

        return obterUtilizador(idCriado);
    }

    private UtilizadorResponse toResponse(Utilizador utilizador) {
        return new UtilizadorResponse(
                utilizador.getId(),
                utilizador.getNome(),
                utilizador.getEmail(),
                utilizador.getTelemovel(),
                utilizador.getEstado() != null ? utilizador.getEstado().name() : null
        );
    }
}

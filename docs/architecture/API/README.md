# API

Camada HTTP do projeto. A implementacao compilavel fica em:

```text
src/main/java/com/example/projeto2/API
```

Estrutura usada:

- `Controllers`: endpoints REST, por exemplo `POST /api/users`
- `Modules`: DTOs/contratos expostos pela API
- `Services`: adaptadores entre endpoints e regras de negocio existentes
- `Repositories`: ponto de integracao com os repositories JPA do projeto

Os services da API reutilizam a logica de negocio ja existente na aplicacao desktop, evitando duplicar regras.

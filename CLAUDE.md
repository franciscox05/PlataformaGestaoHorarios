# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PlataformaGestaoHorarios** is a hybrid desktop + web schedule management platform for a Levi's store. A single Maven project produces two runnable applications that share a common business logic layer (API/) and a PostgreSQL database.

## Commands

### Build
```powershell
.\mvnw.cmd clean package
```

### Run
```powershell
# Desktop app (JavaFX)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.mainClass=com.example.projeto2.AppLauncher"

# Web server (port 8081)
java -jar target\Projeto2-0.0.1-SNAPSHOT-web.jar --server.port=8081

# Both simultaneously via helper script
.\scripts\iniciar-dev.ps1 -Modo ambas -JavaHome 'C:\path\to\jdk'
```

### Tests
```powershell
.\mvnw.cmd test
# Or a single test class:
.\mvnw.cmd test -Dtest=ClassName
```

### Database setup
```powershell
# Populate with demo data
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -d gestaohorarios -f .\sql\demo-entrega.sql
```

### Demo accounts (password: `123456`)
| Email | Role |
|---|---|
| `francisco@levis.com` | Full access / admin |
| `francisco.gomes@levis.com` | Store manager |
| `henrique.siano@levis.com` | Employee |

## Architecture

### Three-module structure inside one Maven project

```
API/          – shared business logic (Services, Repositories, Modules/entities, Enums)
DESKTOP/      – JavaFX GUI; controllers inject API services directly via Spring
WEB/          – Spring MVC + Thymeleaf; controllers also inject API services directly
```

Both DESKTOP and WEB consume the **same service classes** in `API/Services/`. There is no REST boundary between the desktop and the database; the desktop uses Spring's application context directly (`WebApplicationType.NONE`).

### Entry points

| App | Main class |
|---|---|
| Desktop | `AppLauncher` → `Projeto2Application` |
| Web | `Projeto2WebApplication` |

Desktop startup: `AppLauncher.main()` boots Spring (no web server), loads `login/login-view.fxml`, then transitions to `DashboardController` on successful auth.

### Key services

- **`GeracaoHorariosService`** – orchestrates the schedule generation engine (`API/Services/geracao/`)
- **`HorarioGeneratorEngine`** – backtracking constraint solver for shift assignment
- **`LojautilizadorHelper`** – centralised permission checks; used by every controller
- **`SessaoService`** / **`WebSession`** – session state for desktop and web respectively
- **`ExportacaoPdfService`** – shared PDF export (used by both desktop and web)
- **`DayOffService`**, **`PermutaService`**, **`PreferenciaService`** – core HR workflow services

### FXML ↔ Controller mapping (Desktop)

FXML files live under `src/main/resources/com/example/projeto2/`. Each screen has a paired controller in `DESKTOP/`. The dashboard shell (`DashboardController`) loads sub-views dynamically into a center pane.

### Web routing

Thymeleaf templates live under `src/main/resources/templates/web/`. All web routes are session-guarded; `WebLoginController` redirects unauthenticated requests. Permission enforcement mirrors the desktop using the same `LojautilizadorHelper`.

## Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 25 |
| Build | Maven 3.9 (wrapper included) |
| Framework | Spring Boot 4.0.3 |
| Desktop UI | JavaFX 21.0.2 + FXML |
| Web UI | Thymeleaf (Spring MVC) |
| Persistence | Spring Data JPA / Hibernate → PostgreSQL 18 |
| Icons | Ikonli Material Design 2 (12.3.1) |
| PDF | Apache PDFBox 3.0.3 |

## Configuration

`src/main/resources/application.properties` – database URL, JPA settings, server port. No `.env` file; credentials are set directly in `application.properties` or overridden via environment variables at launch.

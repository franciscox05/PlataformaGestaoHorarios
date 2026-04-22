param(
    [string]$Database = "gestaohorarios",
    [string]$Username = "postgres",
    [string]$Password,
    [string]$PsqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
)

$scriptPath = Join-Path $PSScriptRoot "..\sql\issue-56-dados-teste-operacionais.sql"
$scriptPath = [System.IO.Path]::GetFullPath($scriptPath)

if (-not (Test-Path $PsqlPath)) {
    throw "Nao foi encontrado o executavel psql em '$PsqlPath'."
}

if (-not (Test-Path $scriptPath)) {
    throw "Nao foi encontrado o script SQL em '$scriptPath'."
}

$previousPassword = $env:PGPASSWORD

try {
    if ($PSBoundParameters.ContainsKey("Password")) {
        $env:PGPASSWORD = $Password
    }

    & $PsqlPath -v ON_ERROR_STOP=1 -U $Username -d $Database -f $scriptPath

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao carregar os dados de teste operacionais."
    }
}
finally {
    if ($PSBoundParameters.ContainsKey("Password")) {
        if ($null -ne $previousPassword) {
            $env:PGPASSWORD = $previousPassword
        } else {
            Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        }
    }
}

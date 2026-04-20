param(
    [string]$BrowserPath,
    [string]$HtmlPath = ".\docs\relatorio-final-entrega.html",
    [string]$PdfPath = ".\Projeto2_EI_33400_33397_relatorio_atualizado.pdf"
)

$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $BrowserPath) {
    $candidatos = @(
        "C:\Program Files\Google\Chrome\Application\chrome.exe",
        "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
        "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
        "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
    )

    foreach ($candidato in $candidatos) {
        if (Test-Path $candidato) {
            $BrowserPath = $candidato
            break
        }
    }
}

if (-not $BrowserPath -or -not (Test-Path $BrowserPath)) {
    throw "Nao foi encontrado Chrome ou Edge. Indica o executavel com -BrowserPath."
}

$htmlResolvido = Resolve-Path (Join-Path $repoRoot $HtmlPath)
$pdfResolvido = Join-Path $repoRoot $PdfPath

$htmlUri = [System.Uri]::new($htmlResolvido.Path).AbsoluteUri

& $BrowserPath `
    "--headless=new" `
    "--disable-gpu" `
    "--allow-file-access-from-files" `
    "--print-to-pdf=$pdfResolvido" `
    $htmlUri

if (-not (Test-Path $pdfResolvido)) {
    throw "O PDF nao foi encontrado em $pdfResolvido."
}

if ($LASTEXITCODE -ne 0) {
    Write-Warning "O browser devolveu codigo de saida $LASTEXITCODE, mas o PDF foi gerado com sucesso."
}

Write-Host ""
Write-Host "PDF gerado com sucesso:" -ForegroundColor Green
Write-Host $pdfResolvido -ForegroundColor Green

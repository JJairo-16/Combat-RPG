param(
    [string]$LibsFolder = "libs",
    [string]$ConfigFile = "artifacts.json",
    [switch]$StopOnError
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Text)
    Write-Host ""
    Write-Host ("=" * 70) -ForegroundColor DarkCyan
    Write-Host ("  " + $Text) -ForegroundColor Cyan
    Write-Host ("=" * 70) -ForegroundColor DarkCyan
}

function Write-Info {
    param([string]$Text)
    Write-Host "[INFO] $Text" -ForegroundColor Gray
}

function Write-Ok {
    param([string]$Text)
    Write-Host "[OK]   $Text" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Text)
    Write-Host "[AVÍS] $Text" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Text)
    Write-Host "[ERROR] $Text" -ForegroundColor Red
}

function Test-CommandExists {
    param([string]$CommandName)

    return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Resolve-LibsPath {
    param([string]$RelativePath)

    return Join-Path $PSScriptRoot $RelativePath
}

function Load-ArtifactConfig {
    param([string]$ConfigPath)

    if (-not (Test-Path $ConfigPath)) {
        throw "No s'ha trobat el fitxer de configuració: $ConfigPath"
    }

    $jsonRaw = Get-Content $ConfigPath -Raw -Encoding UTF8
    $config = $jsonRaw | ConvertFrom-Json

    if ($null -eq $config) {
        throw "El JSON està buit o no és vàlid: $ConfigPath"
    }

    if ($null -eq $config.defaults) {
        Write-Warn "No s'ha trobat la secció 'defaults'. S'utilitzaran només els valors per artefacte."
    }

    if ($null -eq $config.artifacts -or $config.artifacts.Count -eq 0) {
        throw "No s'han trobat artefactes a la propietat 'artifacts'."
    }

    return $config
}

function Get-ArtifactMap {
    param($Artifacts)

    $map = @{}
    foreach ($artifact in $Artifacts) {
        if ([string]::IsNullOrWhiteSpace($artifact.file)) {
            Write-Warn "S'ha omès una entrada sense la propietat 'file'."
            continue
        }

        $map[$artifact.file] = $artifact
    }

    return $map
}

function Install-Jar {
    param(
        [string]$JarPath,
        [object]$Artifact,
        [object]$Defaults
    )

    $fileName = Split-Path $JarPath -Leaf

    $groupId    = if ($Artifact.groupId)    { $Artifact.groupId }    elseif ($Defaults.groupId)    { $Defaults.groupId }    else { $null }
    $artifactId = if ($Artifact.artifactId) { $Artifact.artifactId } elseif ($Defaults.artifactId) { $Defaults.artifactId } else { [System.IO.Path]::GetFileNameWithoutExtension($fileName) }
    $version    = if ($Artifact.version)    { $Artifact.version }    elseif ($Defaults.version)    { $Defaults.version }    else { $null }
    $packaging  = if ($Artifact.packaging)  { $Artifact.packaging }  elseif ($Defaults.packaging)  { $Defaults.packaging }  else { "jar" }

    if ([string]::IsNullOrWhiteSpace($groupId)) {
        throw "Falta groupId per a '$fileName'."
    }

    if ([string]::IsNullOrWhiteSpace($artifactId)) {
        throw "Falta artifactId per a '$fileName'."
    }

    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "Falta version per a '$fileName'."
    }

    Write-Info "Instal·lant $fileName"
    Write-Host "       groupId    = $groupId" -ForegroundColor DarkGray
    Write-Host "       artifactId = $artifactId" -ForegroundColor DarkGray
    Write-Host "       version    = $version" -ForegroundColor DarkGray
    Write-Host "       packaging  = $packaging" -ForegroundColor DarkGray

    $mvnArgs = @(
        "install:install-file"
        "-Dfile=$JarPath"
        "-DgroupId=$groupId"
        "-DartifactId=$artifactId"
        "-Dversion=$version"
        "-Dpackaging=$packaging"
    )

    if ($Artifact.generatePom -eq $true -or ($Defaults.generatePom -eq $true -and $null -eq $Artifact.generatePom)) {
        $mvnArgs += "-DgeneratePom=true"
    }

    & mvn @mvnArgs

    if ($LASTEXITCODE -ne 0) {
        throw "Maven ha retornat el codi $LASTEXITCODE en instal·lar '$fileName'."
    }

    Write-Ok "$fileName instal·lat correctament."
}

try {
    Write-Section "Instal·lador de JARs a Maven"

    if (-not (Test-CommandExists "mvn")) {
        throw "No s'ha trobat la comanda 'mvn'. Assegura't que Maven està instal·lat i al PATH."
    }

    $libsPath = Resolve-LibsPath $LibsFolder
    $configPath = Join-Path $libsPath $ConfigFile

    Write-Info "Directori de llibreries: $libsPath"
    Write-Info "Fitxer de configuració: $configPath"

    if (-not (Test-Path $libsPath)) {
        throw "No existeix el directori de llibreries: $libsPath"
    }

    $config = Load-ArtifactConfig -ConfigPath $configPath
    $artifactMap = Get-ArtifactMap -Artifacts $config.artifacts

    $jarFiles = Get-ChildItem -Path $libsPath -Filter "*.jar" -File | Sort-Object Name

    if ($jarFiles.Count -eq 0) {
        Write-Warn "No s'han trobat fitxers .jar a '$libsPath'."
        exit 0
    }

    Write-Info "JARs trobats: $($jarFiles.Count)"

    $successCount = 0
    $errorCount = 0
    $index = 0

    foreach ($jar in $jarFiles) {
        $index++
        $percent = [int](($index / $jarFiles.Count) * 100)
        Write-Progress -Activity "Instal·lant JARs" -Status "$($jar.Name) ($index/$($jarFiles.Count))" -PercentComplete $percent

        try {
            if (-not $artifactMap.ContainsKey($jar.Name)) {
                throw "No hi ha configuració per a '$($jar.Name)' al JSON."
            }

            $artifact = $artifactMap[$jar.Name]
            Install-Jar -JarPath $jar.FullName -Artifact $artifact -Defaults $config.defaults
            $successCount++
        }
        catch {
            $errorCount++
            Write-Err $_.Exception.Message

            if ($StopOnError) {
                throw
            }
        }
    }

    Write-Progress -Activity "Instal·lant JARs" -Completed

    Write-Section "Resum"
    Write-Ok   "Instal·lats correctament: $successCount"
    if ($errorCount -gt 0) {
        Write-Warn "Errors: $errorCount"
        exit 1
    } else {
        Write-Ok "Procés completat sense errors."
    }
}
catch {
    Write-Host ""
    Write-Err $_.Exception.Message
    exit 1
}
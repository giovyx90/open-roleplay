param(
    [string]$OutputDir
)

$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $RootDir "target/resource-packs"
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-ZipRelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )

    $base = (Resolve-Path -LiteralPath $BasePath).Path.TrimEnd("\", "/")
    $full = (Resolve-Path -LiteralPath $FullPath).Path
    return $full.Substring($base.Length).TrimStart("\", "/").Replace("\", "/")
}

$foundPack = $false

Get-ChildItem -Path $RootDir -Directory | ForEach-Object {
    $moduleDir = $_.FullName
    $moduleName = $_.Name
    $packDir = Join-Path $moduleDir "assets/resource-pack"

    if (-not (Test-Path -LiteralPath $packDir -PathType Container)) {
        return
    }

    $foundPack = $true

    if (-not (Test-Path -LiteralPath (Join-Path $packDir "pack.mcmeta") -PathType Leaf)) {
        Write-Error "Errore: $moduleName non contiene pack.mcmeta"
    }

    if (-not (Test-Path -LiteralPath (Join-Path $packDir "assets") -PathType Container)) {
        Write-Error "Errore: $moduleName non contiene assets/"
    }

    $zipPath = Join-Path $OutputDir "$moduleName-resource-pack.zip"
    $tmpZip = "$zipPath.tmp"
    Remove-Item -LiteralPath $tmpZip, $zipPath -Force -ErrorAction SilentlyContinue

    $zip = [System.IO.Compression.ZipFile]::Open($tmpZip, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        $entries = @(
            (Join-Path $packDir "pack.mcmeta"),
            (Join-Path $packDir "assets")
        )

        $packPng = Join-Path $packDir "pack.png"
        if (Test-Path -LiteralPath $packPng -PathType Leaf) {
            $entries += $packPng
        }

        foreach ($entry in $entries) {
            if (Test-Path -LiteralPath $entry -PathType Container) {
                Get-ChildItem -LiteralPath $entry -File -Recurse | ForEach-Object {
                    if ($_.Name -eq ".DS_Store" -or $_.FullName -like "*\__MACOSX\*") {
                        return
                    }

                    $relativePath = Get-ZipRelativePath $packDir $_.FullName
                    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $relativePath) | Out-Null
                }
            } else {
                $relativePath = Get-ZipRelativePath $packDir $entry
                [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $entry, $relativePath) | Out-Null
            }
        }
    } finally {
        $zip.Dispose()
    }

    Move-Item -LiteralPath $tmpZip -Destination $zipPath -Force
    Write-Host "Creato: $zipPath"
}

if (-not $foundPack) {
    Write-Error "Errore: nessun resource pack trovato in */assets/resource-pack"
}

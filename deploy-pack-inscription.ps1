#!/usr/bin/env powershell
# ============================================================
# Pack Inscription System - PowerShell Deployment Script
# EcoAdventure v1.0.0 - May 12, 2026
# ============================================================

param(
    [string]$ProjectRoot = $PSScriptRoot
)

# Color functions
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

function Write-Title {
    param([string]$Message)
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Blue
    Write-Host "║ $Message" -ForegroundColor Blue
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Blue
    Write-Host ""
}

# Main script
Write-Title "Pack Inscription System - PowerShell Deployment Helper"
Write-Info "Project Root: $ProjectRoot"
Write-Host ""

# Define files to copy
$filesToCopy = @(
    @{
        Source = "src/main/java/controllers/PackInscriptionViewController.java"
        Destination = "$ProjectRoot/src/main/java/controllers/PackInscriptionViewController.java"
        Description = "PackInscriptionViewController.java"
        Type = "Java"
    },
    @{
        Source = "src/main/java/Services/PackInscriptionService.java"
        Destination = "$ProjectRoot/src/main/java/Services/PackInscriptionService.java"
        Description = "PackInscriptionService.java"
        Type = "Java"
    },
    @{
        Source = "src/main/java/config/PackInscriptionConfig.java"
        Destination = "$ProjectRoot/src/main/java/config/PackInscriptionConfig.java"
        Description = "PackInscriptionConfig.java"
        Type = "Java"
    },
    @{
        Source = "src/main/resources/fxml/PackInscriptionView.fxml"
        Destination = "$ProjectRoot/src/main/resources/fxml/PackInscriptionView.fxml"
        Description = "PackInscriptionView.fxml"
        Type = "FXML"
    },
    @{
        Source = "src/main/resources/css/pack-inscription.css"
        Destination = "$ProjectRoot/src/main/resources/css/pack-inscription.css"
        Description = "pack-inscription.css"
        Type = "CSS"
    },
    @{
        Source = "src/main/resources/css/pack-inscription-animations.css"
        Destination = "$ProjectRoot/src/main/resources/css/pack-inscription-animations.css"
        Description = "pack-inscription-animations.css"
        Type = "CSS"
    },
    @{
        Source = "PACK_INSCRIPTION_IMPLEMENTATION.md"
        Destination = "$ProjectRoot/docs/PACK_INSCRIPTION_IMPLEMENTATION.md"
        Description = "Implementation Guide"
        Type = "Documentation"
    },
    @{
        Source = "PACK_INSCRIPTION_QUICKSTART.html"
        Destination = "$ProjectRoot/docs/PACK_INSCRIPTION_QUICKSTART.html"
        Description = "Quick Start Guide"
        Type = "Documentation"
    }
)

# Create directories
Write-Host "📁 Creating directories..." -ForegroundColor Blue
$directories = @(
    "$ProjectRoot/src/main/java/controllers",
    "$ProjectRoot/src/main/java/Services",
    "$ProjectRoot/src/main/java/config",
    "$ProjectRoot/src/main/resources/fxml",
    "$ProjectRoot/src/main/resources/css",
    "$ProjectRoot/docs"
)

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}
Write-Success "Directories created"

# Copy files
Write-Host ""
Write-Host "📦 Copying files..." -ForegroundColor Blue

$copiedCount = 0
$failedCount = 0

foreach ($file in $filesToCopy) {
    if (Test-Path $file.Source) {
        Copy-Item -Path $file.Source -Destination $file.Destination -Force
        Write-Success "$($file.Description)"
        $copiedCount++
    }
    else {
        Write-Error "Source not found: $($file.Source)"
        $failedCount++
    }
}

# Display results
Write-Title "Installation Complete"
Write-Host "📊 Results:" -ForegroundColor Yellow
Write-Host "   Copied:  $copiedCount files" -ForegroundColor Green
if ($failedCount -gt 0) {
    Write-Host "   Failed:  $failedCount files" -ForegroundColor Red
}

Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 📖 Read the Quick Start Guide:"
Write-Host "   Open: $ProjectRoot\docs\PACK_INSCRIPTION_QUICKSTART.html"
Write-Host ""
Write-Host "2. 🔧 Update Navigation:"
Write-Host "   Edit: Your Menu/Dashboard controller"
Write-Host "   Add:  @FXML method goToPackInscription()"
Write-Host ""
Write-Host "3. 🧪 Test the System:"
Write-Host "   Run:  Your application"
Write-Host "   Test: Pack selection & payment flow"
Write-Host ""
Write-Host "4. 🚀 Deploy:"
Write-Host "   Build:  gradle build"
Write-Host "   Deploy: Push to production"
Write-Host ""

Write-Host "📞 For Support:" -ForegroundColor Yellow
Write-Host "   See: $ProjectRoot\docs\PACK_INSCRIPTION_IMPLEMENTATION.md"
Write-Host "   Or:  $ProjectRoot\docs\PACK_INSCRIPTION_QUICKSTART.html"
Write-Host ""

Write-Host "Ready to go! Good luck! 🚀" -ForegroundColor Green
Write-Host ""

@echo off
REM ============================================================
REM Pack Inscription System - Windows Deployment Script
REM EcoAdventure v1.0.0 - May 12, 2026
REM ============================================================

setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════════════════════╗
echo ║  Pack Inscription System - Windows Deployment Helper  ║
echo ╚════════════════════════════════════════════════════════╝
echo.

REM Define project path (customize if needed)
set PROJECT_ROOT=%CD%

echo 📍 Project Root: %PROJECT_ROOT%
echo.

REM Create directories
echo 📁 Creating directories...
if not exist "%PROJECT_ROOT%\src\main\java\controllers" mkdir "%PROJECT_ROOT%\src\main\java\controllers"
if not exist "%PROJECT_ROOT%\src\main\java\Services" mkdir "%PROJECT_ROOT%\src\main\java\Services"
if not exist "%PROJECT_ROOT%\src\main\java\config" mkdir "%PROJECT_ROOT%\src\main\java\config"
if not exist "%PROJECT_ROOT%\src\main\resources\fxml" mkdir "%PROJECT_ROOT%\src\main\resources\fxml"
if not exist "%PROJECT_ROOT%\src\main\resources\css" mkdir "%PROJECT_ROOT%\src\main\resources\css"
if not exist "%PROJECT_ROOT%\docs" mkdir "%PROJECT_ROOT%\docs"
echo ✓ Directories created

REM Copy Java files
echo.
echo 📦 Copying Java files...

copy "src\main\java\controllers\PackInscriptionViewController.java" ^
      "%PROJECT_ROOT%\src\main\java\controllers\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ PackInscriptionViewController.java
) else (
    echo ✗ Failed to copy PackInscriptionViewController.java
)

copy "src\main\java\Services\PackInscriptionService.java" ^
      "%PROJECT_ROOT%\src\main\java\Services\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ PackInscriptionService.java
) else (
    echo ✗ Failed to copy PackInscriptionService.java
)

copy "src\main\java\config\PackInscriptionConfig.java" ^
      "%PROJECT_ROOT%\src\main\java\config\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ PackInscriptionConfig.java
) else (
    echo ✗ Failed to copy PackInscriptionConfig.java
)

REM Copy FXML
echo.
echo 🎨 Copying FXML files...

copy "src\main\resources\fxml\PackInscriptionView.fxml" ^
      "%PROJECT_ROOT%\src\main\resources\fxml\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ PackInscriptionView.fxml
) else (
    echo ✗ Failed to copy PackInscriptionView.fxml
)

REM Copy CSS
echo.
echo 🎨 Copying CSS files...

copy "src\main\resources\css\pack-inscription.css" ^
      "%PROJECT_ROOT%\src\main\resources\css\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ pack-inscription.css
) else (
    echo ✗ Failed to copy pack-inscription.css
)

copy "src\main\resources\css\pack-inscription-animations.css" ^
      "%PROJECT_ROOT%\src\main\resources\css\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ pack-inscription-animations.css
) else (
    echo ✗ Failed to copy pack-inscription-animations.css
)

REM Copy Documentation
echo.
echo 📚 Copying documentation...

copy "PACK_INSCRIPTION_IMPLEMENTATION.md" ^
      "%PROJECT_ROOT%\docs\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ Implementation Guide
) else (
    echo ✗ Failed to copy Implementation Guide
)

copy "PACK_INSCRIPTION_QUICKSTART.html" ^
      "%PROJECT_ROOT%\docs\" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ Quick Start Guide
) else (
    echo ✗ Failed to copy Quick Start Guide
)

REM Display completion message
echo.
echo ╔════════════════════════════════════════════════════════╗
echo ║              Installation Complete!                   ║
echo ╚════════════════════════════════════════════════════════╝
echo.

echo ✓ All files deployed successfully!
echo.

echo 📋 Next Steps:
echo.
echo 1. 📖 Read the Quick Start Guide:
echo    Open: "%PROJECT_ROOT%\docs\PACK_INSCRIPTION_QUICKSTART.html"
echo.
echo 2. 🔧 Update Navigation:
echo    Edit: Your Menu/Dashboard controller
echo    Add:  @FXML method goToPackInscription()
echo.
echo 3. 🧪 Test the System:
echo    Run:  Your application
echo    Test: Pack selection ^& payment flow
echo.
echo 4. 🚀 Deploy:
echo    Build:  gradle build
echo    Deploy: Push to production
echo.

echo 📞 For Support:
echo    See: "%PROJECT_ROOT%\docs\PACK_INSCRIPTION_IMPLEMENTATION.md"
echo    Or:  "%PROJECT_ROOT%\docs\PACK_INSCRIPTION_QUICKSTART.html"
echo.

echo Ready to go! Good luck! 🚀
echo.

pause

#!/bin/bash
# 🚀 Quick Setup Script for Pack Inscription System
# Run this script to verify all files are in place

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║   Pack Inscription System - Installation Verification    ║"
echo "║                    EcoAdventure v1.0.0                   ║"
echo "╚═══════════════════════════════════════════════════════════╝"

echo ""
echo "🔍 Checking file structure..."
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter
TOTAL=0
FOUND=0

# Function to check file
check_file() {
    local file=$1
    local description=$2
    TOTAL=$((TOTAL + 1))
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $description"
        FOUND=$((FOUND + 1))
    else
        echo -e "${RED}✗${NC} $description"
        echo "  Expected at: $file"
    fi
}

# Check Java files
echo "📦 Checking Java files..."
check_file "src/main/java/controllers/PackInscriptionViewController.java" "PackInscriptionViewController.java"
check_file "src/main/java/Services/PackInscriptionService.java" "PackInscriptionService.java"
check_file "src/main/java/config/PackInscriptionConfig.java" "PackInscriptionConfig.java"
check_file "src/main/java/controllers/examples/MenuIntegrationExample.java" "MenuIntegrationExample.java"

echo ""
echo "🎨 Checking Frontend files..."
check_file "src/main/resources/fxml/PackInscriptionView.fxml" "PackInscriptionView.fxml"
check_file "src/main/resources/css/pack-inscription.css" "pack-inscription.css"
check_file "src/main/resources/css/pack-inscription-animations.css" "pack-inscription-animations.css"

echo ""
echo "📚 Checking Documentation..."
check_file "PACK_INSCRIPTION_IMPLEMENTATION.md" "Implementation Guide"
check_file "PACK_INSCRIPTION_QUICKSTART.html" "Quick Start Guide"
check_file "PACK_INSCRIPTION_SUMMARY.md" "Summary Document"
check_file "PACK_INSCRIPTION_FILE_INDEX.md" "File Index"
check_file "PACK_INSCRIPTION_TESTING.md" "Testing Guide"

echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║                    Installation Summary                  ║"
echo "╚═══════════════════════════════════════════════════════════╝"

# Calculate percentage
if [ $TOTAL -gt 0 ]; then
    PERCENTAGE=$((FOUND * 100 / TOTAL))
else
    PERCENTAGE=0
fi

echo ""
echo "Files Found:    $FOUND / $TOTAL"
echo "Completion:     $PERCENTAGE%"

if [ $FOUND -eq $TOTAL ]; then
    echo -e "${GREEN}✓ All files installed successfully!${NC}"
    echo ""
    echo "🚀 Next Steps:"
    echo "1. Open PACK_INSCRIPTION_QUICKSTART.html in your browser"
    echo "2. Follow the integration steps"
    echo "3. Update your application routing"
    echo "4. Test all features"
    exit 0
else
    echo -e "${RED}✗ Some files are missing!${NC}"
    echo ""
    echo "Please copy the missing files to their correct locations."
    echo "See PACK_INSCRIPTION_FILE_INDEX.md for file structure."
    exit 1
fi

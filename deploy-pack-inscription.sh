#!/bin/bash

# ============================================================
# Pack Inscription System - Deployment Script
# EcoAdventure v1.0.0 - May 12, 2026
# ============================================================

set -e

echo "╔════════════════════════════════════════════════════════╗"
echo "║  Pack Inscription System - Deployment Helper         ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Define colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Define paths
PROJECT_ROOT="${1:-.}"

echo -e "${BLUE}📍 Project Root: $PROJECT_ROOT${NC}"
echo ""

# Function to copy file
copy_file() {
    local source=$1
    local destination=$2
    local description=$3
    
    if [ -f "$source" ]; then
        mkdir -p "$(dirname "$destination")"
        cp "$source" "$destination"
        echo -e "${GREEN}✓${NC} $description"
    else
        echo -e "${RED}✗${NC} Source not found: $source"
    fi
}

# Copy Java files
echo -e "${BLUE}📦 Copying Java files...${NC}"
copy_file \
    "src/main/java/controllers/PackInscriptionViewController.java" \
    "$PROJECT_ROOT/src/main/java/controllers/PackInscriptionViewController.java" \
    "PackInscriptionViewController.java"

copy_file \
    "src/main/java/Services/PackInscriptionService.java" \
    "$PROJECT_ROOT/src/main/java/Services/PackInscriptionService.java" \
    "PackInscriptionService.java"

copy_file \
    "src/main/java/config/PackInscriptionConfig.java" \
    "$PROJECT_ROOT/src/main/java/config/PackInscriptionConfig.java" \
    "PackInscriptionConfig.java"

# Copy FXML
echo ""
echo -e "${BLUE}🎨 Copying FXML files...${NC}"
copy_file \
    "src/main/resources/fxml/PackInscriptionView.fxml" \
    "$PROJECT_ROOT/src/main/resources/fxml/PackInscriptionView.fxml" \
    "PackInscriptionView.fxml"

# Copy CSS
echo ""
echo -e "${BLUE}🎨 Copying CSS files...${NC}"
copy_file \
    "src/main/resources/css/pack-inscription.css" \
    "$PROJECT_ROOT/src/main/resources/css/pack-inscription.css" \
    "pack-inscription.css"

copy_file \
    "src/main/resources/css/pack-inscription-animations.css" \
    "$PROJECT_ROOT/src/main/resources/css/pack-inscription-animations.css" \
    "pack-inscription-animations.css"

# Copy documentation
echo ""
echo -e "${BLUE}📚 Copying documentation...${NC}"
copy_file \
    "PACK_INSCRIPTION_IMPLEMENTATION.md" \
    "$PROJECT_ROOT/docs/PACK_INSCRIPTION_IMPLEMENTATION.md" \
    "Implementation Guide"

copy_file \
    "PACK_INSCRIPTION_QUICKSTART.html" \
    "$PROJECT_ROOT/docs/PACK_INSCRIPTION_QUICKSTART.html" \
    "Quick Start Guide"

# Display next steps
echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║              Installation Complete!                   ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

echo -e "${GREEN}✓ All files deployed successfully!${NC}"
echo ""

echo "📋 Next Steps:"
echo ""
echo "1. 📖 Read the Quick Start Guide:"
echo "   Open: $PROJECT_ROOT/docs/PACK_INSCRIPTION_QUICKSTART.html"
echo ""
echo "2. 🔧 Update Navigation:"
echo "   Edit: Your Menu/Dashboard controller"
echo "   Add:  @FXML method goToPackInscription()"
echo ""
echo "3. 🧪 Test the System:"
echo "   Run:  Your application"
echo "   Test: Pack selection & payment flow"
echo ""
echo "4. 🚀 Deploy:"
echo "   Build:  gradle build"
echo "   Deploy: Push to production"
echo ""

echo "📞 For Support:"
echo "   See: $PROJECT_ROOT/docs/PACK_INSCRIPTION_IMPLEMENTATION.md"
echo "   Or:  $PROJECT_ROOT/docs/PACK_INSCRIPTION_QUICKSTART.html"
echo ""

echo -e "${GREEN}Ready to go! Good luck! 🚀${NC}"

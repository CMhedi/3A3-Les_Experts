#!/usr/bin/env python3
"""
Pack Inscription System - Installation Verification Script
EcoAdventure v1.0.0 - May 12, 2026

Usage:
    python3 verify-installation.py
    python3 verify-installation.py --project /path/to/project
"""

import os
import sys
from pathlib import Path
from datetime import datetime

# Color codes
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
BLUE = '\033[94m'
END = '\033[0m'

def print_header(text):
    print(f"\n{BLUE}╔═══════════════════════════════════════════════════╗{END}")
    print(f"{BLUE}║ {text:<50} ║{END}")
    print(f"{BLUE}╚═══════════════════════════════════════════════════╝{END}\n")

def print_success(text):
    print(f"{GREEN}✓{END} {text}")

def print_error(text):
    print(f"{RED}✗{END} {text}")

def print_info(text):
    print(f"{YELLOW}ℹ{END} {text}")

def check_file(file_path, description):
    """Check if file exists and return status."""
    if os.path.exists(file_path):
        print_success(description)
        return True
    else:
        print_error(f"{description} - NOT FOUND at {file_path}")
        return False

def main():
    # Get project root
    if len(sys.argv) > 2 and sys.argv[1] == "--project":
        project_root = sys.argv[2]
    else:
        project_root = os.getcwd()
    
    print_header("Pack Inscription System - Installation Verification")
    print_info(f"Project Root: {project_root}\n")
    
    # Define files to check
    files_to_check = [
        # Java files
        ("src/main/java/controllers/PackInscriptionViewController.java", "Java: PackInscriptionViewController"),
        ("src/main/java/Services/PackInscriptionService.java", "Java: PackInscriptionService"),
        ("src/main/java/config/PackInscriptionConfig.java", "Java: PackInscriptionConfig"),
        
        # FXML files
        ("src/main/resources/fxml/PackInscriptionView.fxml", "FXML: PackInscriptionView"),
        
        # CSS files
        ("src/main/resources/css/pack-inscription.css", "CSS: pack-inscription styles"),
        ("src/main/resources/css/pack-inscription-animations.css", "CSS: Animations"),
        
        # Documentation
        ("PACK_INSCRIPTION_IMPLEMENTATION.md", "Documentation: Implementation Guide"),
        ("PACK_INSCRIPTION_QUICKSTART.html", "Documentation: Quick Start"),
        ("PACK_INSCRIPTION_SUMMARY.md", "Documentation: Summary"),
        ("PACK_INSCRIPTION_FILE_INDEX.md", "Documentation: File Index"),
        ("PACK_INSCRIPTION_TESTING.md", "Documentation: Testing Guide"),
        ("PACK_INSCRIPTION_README.md", "Documentation: Main README"),
    ]
    
    print(f"{BLUE}📦 Checking Files...{END}\n")
    
    total = len(files_to_check)
    found = 0
    
    for file_rel_path, description in files_to_check:
        file_path = os.path.join(project_root, file_rel_path)
        if check_file(file_path, description):
            found += 1
    
    # Calculate percentage
    percentage = (found / total) * 100 if total > 0 else 0
    
    # Summary
    print_header("Verification Summary")
    print(f"Files Found:     {found}/{total}")
    print(f"Completion:      {percentage:.1f}%\n")
    
    if found == total:
        print_success("✅ All files installed successfully!\n")
        print(f"{YELLOW}📋 Next Steps:{END}")
        print("1. Open PACK_INSCRIPTION_QUICKSTART.html in your browser")
        print("2. Follow the integration steps")
        print("3. Update your application navigation")
        print("4. Configure payment settings in PackInscriptionConfig.java")
        print("5. Test all features using PACK_INSCRIPTION_TESTING.md")
        print()
        return 0
    else:
        missing = total - found
        print_error(f"❌ {missing} file(s) missing!\n")
        print(f"{YELLOW}📋 Action Items:{END}")
        print("1. Copy missing files to their correct locations")
        print("2. Reference PACK_INSCRIPTION_FILE_INDEX.md for file structure")
        print("3. See PACK_INSCRIPTION_README.md for complete file list")
        print()
        return 1

if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)

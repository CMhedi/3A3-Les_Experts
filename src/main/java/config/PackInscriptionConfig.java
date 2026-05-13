package config;

/**
 * Pack Inscription Configuration
 * Centralized settings for pack subscription system
 */
public class PackInscriptionConfig {

    // ==================== PAYMENT CONFIGURATION ====================
    
    /**
     * Payment Gateway Settings
     */
    public static final class PaymentConfig {
        // Card Payment
        public static final String CARD_GATEWAY_NAME = "CARD_DEMO";
        public static final String CARD_GATEWAY_API_KEY = "demo_key_123456";
        public static final boolean CARD_REQUIRE_3D_SECURE = false;
        public static final String CARD_CURRENCY = "TND";
        
        // Mobile Payment (Tunisie Telecom)
        public static final String MOBILE_PROVIDER = "TUNISIE_TELECOM";
        public static final String MOBILE_API_URL = "https://api.tt.tn/payment/";
        public static final String MOBILE_API_KEY = "mobile_key_123456";
        
        // Bank Transfer
        public static final String BANK_NAME = "BankOfTunisia";
        public static final String BANK_ACCOUNT = "TN59 0342 0001 0000 0000 0000 000";
        public static final String BANK_IBAN = "TN59034200010000000000000000";
        
        // Transaction limits
        public static final double MIN_TRANSACTION = 5.0;
        public static final double MAX_TRANSACTION = 10000.0;
        
        // Retry policy
        public static final int MAX_RETRIES = 3;
        public static final long RETRY_DELAY_MS = 1000;
    }

    // ==================== EMAIL CONFIGURATION ====================
    
    /**
     * Email Notification Settings
     */
    public static final class EmailConfig {
        public static final String SMTP_HOST = "smtp.gmail.com";
        public static final int SMTP_PORT = 587;
        public static final boolean SMTP_USE_TLS = true;
        public static final String SMTP_USERNAME = "noreply@ecoadventure.tn";
        public static final String SMTP_PASSWORD = "${SMTP_PASSWORD}"; // Use environment variable
        
        // Email templates
        public static final String TEMPLATE_INSCRIPTION = "inscription_confirmation.html";
        public static final String TEMPLATE_PAYMENT = "payment_confirmation.html";
        public static final String TEMPLATE_RECEIPT = "payment_receipt.html";
        
        // Email settings
        public static final String FROM_EMAIL = "noreply@ecoadventure.tn";
        public static final String FROM_NAME = "EcoAdventure";
        public static final String SUPPORT_EMAIL = "support@ecoadventure.tn";
        
        // Notifications
        public static final boolean SEND_INSCRIPTION_EMAIL = true;
        public static final boolean SEND_PAYMENT_EMAIL = true;
        public static final boolean SEND_RECEIPT_EMAIL = true;
        public static final boolean SEND_ADMIN_NOTIFICATION = true;
    }

    // ==================== PROMO CODE CONFIGURATION ====================
    
    /**
     * Promo Code & Loyalty Settings
     */
    public static final class PromoConfig {
        // Loyalty points
        public static final int POINTS_PER_100_TND = 10;
        public static final double POINT_REDEMPTION_RATE = 0.1; // 1 point = 0.1 TND
        
        // Promo codes
        public static final double MAX_DISCOUNT_PERCENTAGE = 50.0;
        public static final int PROMO_CODE_LENGTH = 8;
        public static final boolean CASE_SENSITIVE = false;
        public static final long PROMO_EXPIRY_DAYS = 90;
        
        // Discount rules
        public static final double BULK_DISCOUNT_3_PEOPLE = 5.0;  // %
        public static final double BULK_DISCOUNT_5_PEOPLE = 10.0; // %
        public static final double BULK_DISCOUNT_10_PEOPLE = 15.0; // %
        
        // Referral
        public static final int REFERRAL_BONUS_POINTS = 50;
        public static final double REFERRAL_DISCOUNT = 5.0; // %
    }

    // ==================== VALIDATION CONFIGURATION ====================
    
    /**
     * Form Validation Rules
     */
    public static final class ValidationConfig {
        // Name validation
        public static final int NAME_MIN_LENGTH = 2;
        public static final int NAME_MAX_LENGTH = 50;
        public static final String NAME_PATTERN = "^[a-zA-Zàâäæçéèêëíìîïñóòôöœúùûü\\s'-]+$";
        
        // Email validation
        public static final String EMAIL_PATTERN = 
            "^[A-Za-z0-9+_.-]+@(.+)$";
        
        // Phone validation
        public static final String PHONE_PATTERN = 
            "^(\\+216|0)([0-9]{8})$";
        
        // Card validation
        public static final int CARD_MIN_LENGTH = 13;
        public static final int CARD_MAX_LENGTH = 19;
        public static final String CARD_PATTERN = "^[0-9]{13,19}$";
        
        // CVV validation
        public static final String CVV_PATTERN = "^[0-9]{3,4}$";
        
        // Number of people
        public static final int MIN_PEOPLE = 1;
        public static final int MAX_PEOPLE = 100;
    }

    // ==================== UI CONFIGURATION ====================
    
    /**
     * User Interface Settings
     */
    public static final class UIConfig {
        // Colors
        public static final String COLOR_PRIMARY = "#10B981";
        public static final String COLOR_FOREST = "#14532D";
        public static final String COLOR_SKY = "#38BDF8";
        public static final String COLOR_ORANGE = "#F97316";
        public static final String COLOR_SLATE = "#0F172A";
        public static final String COLOR_MUTED = "#64748B";
        public static final String COLOR_ERROR = "#EF4444";
        public static final String COLOR_SUCCESS = "#10B981";
        public static final String COLOR_WARNING = "#F97316";
        
        // Animation durations (ms)
        public static final long ANIMATION_FADE = 300;
        public static final long ANIMATION_SLIDE = 400;
        public static final long ANIMATION_SCALE = 300;
        public static final long ANIMATION_BOUNCE = 500;
        
        // Responsive breakpoints
        public static final int BREAKPOINT_MOBILE = 600;
        public static final int BREAKPOINT_TABLET = 900;
        public static final int BREAKPOINT_DESKTOP = 1200;
        
        // Font sizes
        public static final int FONT_TITLE = 28;
        public static final int FONT_SUBTITLE = 16;
        public static final int FONT_BODY = 13;
        public static final int FONT_SMALL = 12;
    }

    // ==================== SECURITY CONFIGURATION ====================
    
    /**
     * Security & Encryption Settings
     */
    public static final class SecurityConfig {
        // Encryption
        public static final String ENCRYPTION_ALGORITHM = "AES";
        public static final int ENCRYPTION_KEY_SIZE = 256;
        public static final String HASH_ALGORITHM = "SHA-256";
        
        // Rate limiting
        public static final int MAX_LOGIN_ATTEMPTS = 5;
        public static final long LOGIN_ATTEMPT_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
        public static final int MAX_PAYMENT_ATTEMPTS = 3;
        public static final long PAYMENT_ATTEMPT_TIMEOUT_MS = 60 * 60 * 1000; // 1 hour
        
        // Token settings
        public static final long SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000; // 24 hours
        public static final long PAYMENT_TOKEN_EXPIRY_MS = 30 * 60 * 1000; // 30 minutes
        
        // Security headers
        public static final boolean REQUIRE_HTTPS = true;
        public static final boolean ENABLE_CSRF_PROTECTION = true;
        public static final boolean ENABLE_XSS_PROTECTION = true;
    }

    // ==================== DATABASE CONFIGURATION ====================
    
    /**
     * Database Settings
     */
    public static final class DatabaseConfig {
        // Connection pool
        public static final int CONNECTION_POOL_SIZE = 10;
        public static final int CONNECTION_TIMEOUT_SECONDS = 30;
        
        // Query settings
        public static final int QUERY_TIMEOUT_SECONDS = 60;
        public static final boolean AUTO_COMMIT = true;
        
        // Batch settings
        public static final int BATCH_SIZE = 100;
    }

    // ==================== LOGGING CONFIGURATION ====================
    
    /**
     * Logging Settings
     */
    public static final class LoggingConfig {
        public static final String LOG_LEVEL = "INFO";
        public static final String LOG_FILE_PATH = "logs/pack-inscription.log";
        public static final long LOG_FILE_MAX_SIZE = 10 * 1024 * 1024; // 10 MB
        public static final int LOG_FILE_BACKUP_COUNT = 5;
        
        // Log details
        public static final boolean LOG_DATABASE_QUERIES = false;
        public static final boolean LOG_PAYMENT_DATA = false; // Never log sensitive data
        public static final boolean LOG_EMAIL_CONTENT = false; // Never log email content
    }

    // ==================== UTILITY METHODS ====================
    
    /**
     * Get payment method display name
     */
    public static String getPaymentMethodName(String method) {
        return switch (method.toUpperCase()) {
            case "CARD" -> "💳 Paiement par Carte";
            case "MOBILE" -> "📱 Paiement Mobile";
            case "BANK" -> "🏦 Virement Bancaire";
            default -> "Méthode Inconnue";
        };
    }

    /**
     * Get status display badge
     */
    public static String getStatusBadge(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMEE" -> "✅ Confirmée";
            case "EN_ATTENTE" -> "⏳ En Attente";
            case "PAYEE" -> "💰 Payée";
            case "ANNULEE" -> "❌ Annulée";
            case "REFUNDUE" -> "🔄 Remboursée";
            default -> "❓ " + status;
        };
    }

    /**
     * Format price
     */
    public static String formatPrice(java.math.BigDecimal amount) {
        return String.format("TND %.2f", amount.doubleValue());
    }

    /**
     * Format phone number
     */
    public static String formatPhone(String phone) {
        if (phone == null) return "";
        phone = phone.replaceAll("[^0-9]", "");
        
        if (phone.length() == 8) {
            return "+216 " + phone.substring(0, 2) + " " + 
                   phone.substring(2, 5) + " " + phone.substring(5);
        }
        return phone;
    }

    /**
     * Validate configuration
     */
    public static void validateConfiguration() {
        try {
            // Validate email settings
            if (EmailConfig.SMTP_PASSWORD.contains("${")) {
                System.err.println("⚠️ WARNING: Environment variables not set. Using defaults.");
            }
            
            // Validate payment settings
            if (PaymentConfig.MIN_TRANSACTION >= PaymentConfig.MAX_TRANSACTION) {
                throw new IllegalStateException("Invalid transaction limits");
            }
            
            // Validate promo settings
            if (PromoConfig.POINTS_PER_100_TND <= 0) {
                throw new IllegalStateException("Invalid loyalty points configuration");
            }
            
            System.out.println("✅ Configuration validated successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Configuration error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Print configuration summary
     */
    public static void printSummary() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║      Pack Inscription System Configuration Summary         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        
        System.out.println("\n💳 Payment Configuration:");
        System.out.println("  - Gateway: " + PaymentConfig.CARD_GATEWAY_NAME);
        System.out.println("  - Currency: " + PaymentConfig.CARD_CURRENCY);
        System.out.println("  - Min/Max: " + PaymentConfig.MIN_TRANSACTION + "/" + PaymentConfig.MAX_TRANSACTION);
        
        System.out.println("\n📧 Email Configuration:");
        System.out.println("  - SMTP: " + EmailConfig.SMTP_HOST + ":" + EmailConfig.SMTP_PORT);
        System.out.println("  - From: " + EmailConfig.FROM_EMAIL);
        
        System.out.println("\n🎟️ Promo Configuration:");
        System.out.println("  - Loyalty Rate: " + PromoConfig.POINTS_PER_100_TND + " points/100 TND");
        System.out.println("  - Max Discount: " + PromoConfig.MAX_DISCOUNT_PERCENTAGE + "%");
        
        System.out.println("\n🎨 UI Configuration:");
        System.out.println("  - Primary Color: " + UIConfig.COLOR_PRIMARY);
        System.out.println("  - Animation Duration: " + UIConfig.ANIMATION_FADE + "ms");
        
        System.out.println("\n✅ Configuration ready!\n");
    }
}

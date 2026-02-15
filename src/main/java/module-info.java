module org.example.demo {

    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires java.desktop;
    requires javafx.base;
    requires javafx.graphics;

    opens org.example.demo to javafx.fxml;
    opens Models to javafx.base, javafx.fxml;

    exports org.example.demo;

    opens Controllers to javafx.fxml;
    exports Controllers;
}

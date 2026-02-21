module org.example.demo {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;

    requires java.sql;
    requires java.net.http;
    requires com.google.gson;


    requires org.apache.pdfbox;
    requires javafx.swing;
    requires java.desktop;



    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    opens Controllers to javafx.fxml;
    opens Models to javafx.base, javafx.fxml;

    exports Controllers;
    exports org.example.demo;
}
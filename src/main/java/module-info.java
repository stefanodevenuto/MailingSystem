module progetto {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;
    requires opencsv;
    requires java.sql; // Required by OpenCSV

    opens progetto.server to javafx.fxml, javafx.graphics, javafx.base;

    opens progetto.client to javafx.fxml;
    opens progetto.client.controller to javafx.fxml;
    opens progetto.client.model to javafx.fxml;

    opens progetto.common to javafx.fxml, org.apache.commons.lang3; // Required by OpenCSV

    exports progetto.server;

    exports progetto.client;
    exports progetto.client.controller;
    exports progetto.client.model;

    exports progetto.common;

}
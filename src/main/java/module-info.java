module progetto {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencsv;
    requires java.sql; // Required by OpenCSV

    opens progetto.server to javafx.fxml;
    opens progetto.client to javafx.fxml;
    opens progetto.common to javafx.fxml, org.apache.commons.lang3;

    exports progetto.server;
    exports progetto.client;
    exports progetto.common;
}
module com.kamotomo.pos {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires mysql.connector.j;

    opens com.kamotomo.pos.models to javafx.base;
    opens com.kamotomo.pos to javafx.fxml;
    exports com.kamotomo.pos;
}
module com.kamotomo.pos {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.kamotomo.pos to javafx.fxml;
    exports com.kamotomo.pos;

    opens com.kamotomo.pos.models to javafx.base;
    exports com.kamotomo.pos.models;
}
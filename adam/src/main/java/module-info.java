module com.adam {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive javafx.graphics;

    opens com.adam to javafx.fxml;

    exports com.adam;
}
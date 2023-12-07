module net.jomity.typeracer.typeracerproject {
    requires javafx.controls;
    requires javafx.fxml;


    opens net.jomity.typeracer.typeracerproject to javafx.fxml;
    exports net.jomity.typeracer.typeracerproject;
}
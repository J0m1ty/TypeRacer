module net.jomity.typeracer {
    requires javafx.controls;
    requires javafx.fxml;

    exports net.jomity.typeracer.client;
    opens net.jomity.typeracer.client to javafx.fxml;
    exports net.jomity.typeracer.server;
    opens net.jomity.typeracer.server to javafx.fxml;
    exports net.jomity.typeracer.shared.network;
    opens net.jomity.typeracer.shared.network to javafx.fxml;
    exports net.jomity.typeracer.shared.constants;
    opens net.jomity.typeracer.shared.constants to javafx.fxml;
    exports net.jomity.typeracer.shared.network.packets;
    opens net.jomity.typeracer.shared.network.packets to javafx.fxml;
}
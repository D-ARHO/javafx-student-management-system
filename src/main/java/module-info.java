module net.darho {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    requires org.postgresql.jdbc;

    
    opens net.darho to javafx.graphics, javafx.fxml;
    exports net.darho;
}

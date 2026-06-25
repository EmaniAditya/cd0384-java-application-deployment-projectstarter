module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires com.miglayout.swing;
    requires com.google.gson;
    requires com.google.common;
    requires org.slf4j;
    requires java.desktop;
    requires java.prefs;

    exports com.udacity.catpoint.data;
    exports com.udacity.catpoint.service;
    exports com.udacity.catpoint.application;

    opens com.udacity.catpoint.data;
    opens com.udacity.catpoint.service;
    opens com.udacity.catpoint.application;
}

package org.xefix.efis;

import javafx.beans.Observable;

/**
 * Central hub for all telemetry and configuration settings. Back systems are managed by the SystemsManager.
 *
 * Among other things, the Registry creates an intermediary binding between the source and the consumer. This allows for
 * the publisher to unpublish without the subscriber requiring to re-subscribe. When a publisher publishes or
 * unpublishes, subscribers need to be notified so that they can change their in-op status accordingly.
 *
 * TODO: Make a protocol and protocol handler to receive FGFS data asynchronously. Should be faster and less invasive.
 *
 * TODO: Make a module manager that scans all classes at start-up for publisher and subscriber telemetry as well as
 * configuration settings.
 *
 * Do not apply smoothing at the registry level. Smoothing is for display purposes.
 *
 * @author Matthew Tyler
 */
public class Registry
{

    public void publish(String path, Observable property) {

    }

    public void unpublish(String path) {

    }

    public Observable subscribe(String path) {
        return null;
    }

}

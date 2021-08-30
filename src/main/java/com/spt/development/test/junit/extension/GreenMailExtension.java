package com.spt.development.test.junit.extension;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailProxy;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

/**
 * A JUnit 5 extension used for starting and stopping a {@link GreenMail} test email server.
 */
public class GreenMailExtension extends GreenMailProxy implements BeforeEachCallback, AfterEachCallback {
    private final GreenMail greenMail;

    /**
     * Creates an extension that starts (and stops) an email server that supports all ({@link ServerSetupTest#ALL})
     * protocols.
     */
    public GreenMailExtension() {
        this(ServerSetupTest.ALL);
    }

    /**
     * Creates an extension that starts (and stops) an emails server using the configuration supplied.
     *
     * @param serverSetup the email server configuration.
     */
    public GreenMailExtension(final ServerSetup serverSetup) {
        this(new ServerSetup[] { serverSetup });
    }

    /**
     * Creates an extension that starts (and stops) an emails server using the configuration supplied.
     *
     * @param serverSetups the email server configuration.
     */
    public GreenMailExtension(final ServerSetup[] serverSetups) {
        this.greenMail = new GreenMail(Arrays.copyOf(serverSetups, serverSetups.length));
    }

    /**
     * Restarts the email server, before each test.
     *
     * @param context the current extension context; never null.
     */
    @Override
    public void beforeEach(final ExtensionContext context) {
        reset();
    }

    /**
     * Stops the email server, after each test.
     *
     * @param context the current extension context; never null.
     */
    @Override
    public void afterEach(final ExtensionContext context) {
        stop();
    }

    /**
     * Gets the {@link GreenMail} instance created by this extension.
     *
     * @return the {@link GreenMail} instance created by this extension.
     */
    @Override
    protected GreenMail getGreenMail() {
        return greenMail;
    }

    /**
     * Checks if GreenMail is up and running.
     *
     * @return true if ready to serve.
     */
    @Override
    public boolean isRunning() {
        return greenMail.isRunning();
    }

    /**
     * Fluent interface for configuring the {@link GreenMail} server.
     *
     * @param config the configuration.
     *
     * @return <code>this</code>.
     */
    @Override
    public GreenMailExtension withConfiguration(final GreenMailConfiguration config) {
        super.withConfiguration(config);
        return this;
    }
}

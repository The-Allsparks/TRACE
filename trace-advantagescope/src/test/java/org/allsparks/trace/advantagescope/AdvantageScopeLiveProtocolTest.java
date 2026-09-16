package org.allsparks.trace.advantagescope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import org.allsparks.trace.TraceConfig;
import org.allsparks.trace.TraceMode;
import org.allsparks.trace.core.Pose2d;
import org.allsparks.trace.core.Units;
import org.allsparks.trace.live.AdvantageScopeLiveStats;
import org.allsparks.trace.session.TraceSession;
import org.junit.jupiter.api.Test;

class AdvantageScopeLiveProtocolTest {
    @Test
    void handshakeStatusAndTelemetryReachAClient() throws Exception {
        TraceSession session = sessionOnEphemeralPort();
        try {
            int port = session.advantageScopeLive().listenPort();
            assertTrue(port > 0, "live server should bind an ephemeral port");
            TestWsClient client = TestWsClient.connect(port);
            try {
                client.send("{\"type\":\"GET_ROBOT_STATUS\"}");
                assertTrue(client.waitFor("RECEIVE_ROBOT_STATUS", 2000), client.messages().toString());
                session.record("Battery/Voltage", 12.6, Units.VOLTS);
                session.record("Drive/Pose", new Pose2d(1.0, 0.5, 0.25));
                assertTrue(client.waitFor("RECEIVE_TELEMETRY", 2000), client.messages().toString());
                assertTrue(client.waitFor("Battery/Voltage", 2000), client.messages().toString());
                assertTrue(client.waitFor("Drive/Pose x", 2000), client.messages().toString());
            } finally {
                client.close();
            }
        } finally {
            session.close();
        }
    }

    @Test
    void noClientMeansNoPreparedFrames() throws Exception {
        TraceSession session = sessionOnEphemeralPort();
        try {
            for (int i = 0; i < 50; i++) {
                session.record("Drive/Command", i * 0.01, Units.DIMENSIONLESS);
            }
            Thread.sleep(200L);
            AdvantageScopeLiveStats stats = session.advantageScopeLive();
            assertEquals(0, stats.clientCount());
            assertEquals(0L, stats.framesPrepared());
            assertEquals(0L, stats.bytesSent());
            assertTrue(session.recorded().stream().anyMatch(r -> r.name().value().equals("Drive/Command")));
        } finally {
            session.close();
        }
    }

    @Test
    void disconnectAndReconnectDoesNotLeakTheListenPort() throws Exception {
        TraceSession session = sessionOnEphemeralPort();
        try {
            int port = session.advantageScopeLive().listenPort();
            TestWsClient first = TestWsClient.connect(port);
            first.send("{\"type\":\"GET_ROBOT_STATUS\"}");
            assertTrue(first.waitFor("RECEIVE_ROBOT_STATUS", 2000));
            first.close();
            Thread.sleep(150L);
            TestWsClient second = TestWsClient.connect(port);
            try {
                second.send("{\"type\":\"GET_ROBOT_STATUS\"}");
                assertTrue(second.waitFor("RECEIVE_ROBOT_STATUS", 2000), second.messages().toString());
                session.record("AMPER/Voltage", 13.0, Units.VOLTS);
                assertTrue(second.waitFor("AMPER/Voltage", 2000), second.messages().toString());
            } finally {
                second.close();
            }
        } finally {
            session.close();
        }
    }

    @Test
    void portConflictDoesNotStopTraceRecording() throws Exception {
        try (ServerSocket blocker = new ServerSocket(0)) {
            int busy = blocker.getLocalPort();
            TraceSession session = new TraceSession(TraceConfig.builder()
                    .mode(TraceMode.ESSENTIAL)
                    .memorySink(true)
                    .essentialSampleIntervalNanos(0)
                    .advantageScopeStreaming(true)
                    .advantageScopePort(busy)
                    .build());
            try {
                session.record("Battery/Voltage", 12.1, Units.VOLTS);
                assertTrue(session.recorded().stream().anyMatch(r -> r.name().value().equals("Battery/Voltage")));
                assertFalse(session.advantageScopeLive().listening());
                assertTrue(session.recorded().stream()
                        .anyMatch(r -> r.name().value().equals("TRACE/AdvantageScope/Error")));
            } finally {
                session.close();
            }
        }
    }

    @Test
    void duplicateConfigureDoesNotLeaveTwoServers() throws Exception {
        TraceSession first = sessionOnEphemeralPort();
        int firstPort = first.advantageScopeLive().listenPort();
        first.close();
        TraceSession second = sessionOnEphemeralPort();
        try {
            int secondPort = second.advantageScopeLive().listenPort();
            assertTrue(secondPort > 0);
            TestWsClient client = TestWsClient.connect(secondPort);
            try {
                client.send("{\"type\":\"GET_ROBOT_STATUS\"}");
                assertTrue(client.waitFor("RECEIVE_ROBOT_STATUS", 2000));
            } finally {
                client.close();
            }
            if (firstPort != secondPort && firstPort > 0) {
                boolean oldStillOpen = true;
                try {
                    TestWsClient leftover = TestWsClient.connect(firstPort);
                    leftover.close();
                } catch (Exception closed) {
                    oldStillOpen = false;
                }
                assertFalse(oldStillOpen, "first session still listening on " + firstPort);
            }
        } finally {
            second.close();
        }
    }

    @Test
    void fasterThanPublishDropsStaleFramesInsteadOfQueueing() throws Exception {
        TraceSession session = sessionOnEphemeralPort();
        TestWsClient client = TestWsClient.connect(session.advantageScopeLive().listenPort());
        try {
            client.send("{\"type\":\"GET_ROBOT_STATUS\"}");
            assertTrue(client.waitFor("RECEIVE_ROBOT_STATUS", 2000));
            for (int i = 0; i < 200; i++) {
                session.record("Drive/Command", i, Units.DIMENSIONLESS);
            }
            assertTrue(client.waitFor("Drive/Command", 2000), client.messages().toString());
            Thread.sleep(300L);
            assertTrue(
                    session.advantageScopeLive().framesDropped() > 0L,
                    "expected stale frames, stats=" + session.advantageScopeLive().framesDropped());
            assertTrue(session.advantageScopeLive().framesSent() > 0L);
        } finally {
            client.close();
            session.close();
        }
    }

    @Test
    void producerStaysFastWhenAClientIsConnected() throws Exception {
        TraceSession session = sessionOnEphemeralPort();
        TestWsClient client = TestWsClient.connect(session.advantageScopeLive().listenPort());
        try {
            client.send("{\"type\":\"GET_ROBOT_STATUS\"}");
            client.waitFor("RECEIVE_ROBOT_STATUS", 2000);
            int iterations = 400;
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                session.record("Drive/Command", 0.2, Units.DIMENSIONLESS);
            }
            double nsEach = (System.nanoTime() - start) / (double) iterations;
            assertTrue(nsEach < 2_000_000.0, "record() tracked live send at " + nsEach + " ns");
            Thread.sleep(250L);
            assertTrue(session.advantageScopeLive().framesPrepared() > 0L);
        } finally {
            client.close();
            session.close();
        }
    }

    @Test
    void shutdownStopsSends() throws Exception {
        TraceSession session = sessionOnEphemeralPort();
        int port = session.advantageScopeLive().listenPort();
        session.close();
        boolean connected = true;
        try {
            TestWsClient client = TestWsClient.connect(port);
            client.close();
        } catch (Exception closed) {
            connected = false;
        }
        assertFalse(connected);
    }

    private static TraceSession sessionOnEphemeralPort() {
        return new TraceSession(TraceConfig.builder()
                .mode(TraceMode.ESSENTIAL)
                .memorySink(true)
                .essentialSampleIntervalNanos(0)
                .advantageScopeStreaming(true)
                .advantageScopePort(0)
                .advantageScopeRateHz(20)
                .opModeName("TeleOp")
                .build());
    }
}

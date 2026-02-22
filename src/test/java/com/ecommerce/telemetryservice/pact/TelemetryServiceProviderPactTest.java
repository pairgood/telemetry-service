package com.ecommerce.telemetryservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import com.ecommerce.telemetryservice.model.TraceEvent;
import com.ecommerce.telemetryservice.repository.TraceEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Provider("telemetry-service")   // MUST match spring.application.name exactly
@PactBroker(
    url = "http://localhost:9292",
    authentication = @PactBrokerAuth(username = "admin", password = "admin")
)
@IgnoreNoPactsToVerify  // Allow test to pass when no consumer pacts exist yet
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TelemetryServiceProviderPactTest {

    @LocalServerPort
    private int port;

    @MockBean
    private TraceEventRepository traceEventRepository;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        // Context will be null when @IgnoreNoPactsToVerify creates a placeholder test
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        // Context will be null when @IgnoreNoPactsToVerify creates a placeholder test
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // State string must be IDENTICAL to consumer's given() — character for character
    @State("the telemetry service is available to accept events")
    void telemetryServiceAvailable() {
        // Mock the repository to successfully save events
        when(traceEventRepository.save(any(TraceEvent.class)))
            .thenAnswer(invocation -> {
                TraceEvent event = invocation.getArgument(0);
                event.setId(1L);
                return event;
            });
    }

    @State("the telemetry service is unavailable")
    void telemetryServiceUnavailable() {
        // Mock the repository to throw an exception simulating service unavailability
        when(traceEventRepository.save(any(TraceEvent.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
    }
}

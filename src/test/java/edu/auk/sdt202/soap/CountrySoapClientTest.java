package edu.auk.sdt202.soap;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** Dependency-free integration tests: issue genuine SOAP HTTP requests to a mock server. */
public final class CountrySoapClientTest {
    private static final String NS = "http://www.oorsprong.org/websamples.countryinfo";
    private CountrySoapClientTest() { }

    public static void main(String[] args) throws Exception {
        AtomicReference<String> lastRequest = new AtomicReference<>();
        AtomicReference<String> lastAction = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/CountryInfoService.wso", exchange -> {
            lastAction.set(exchange.getRequestHeaders().getFirst("SOAPAction"));
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastRequest.set(request);
            String response;
            int status = 200;
            if (request.contains("<sCountryISOCode>XX</sCountryISOCode>")) {
                status = 500;
                response = """
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body><soap:Fault><faultcode>soap:Client</faultcode>
                      <faultstring>Unknown country</faultstring></soap:Fault></soap:Body>
                    </soap:Envelope>
                    """;
            } else {
                response = """
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body><CapitalCityResponse xmlns="%s"><CapitalCityResult>Berlin</CapitalCityResult>
                      </CapitalCityResponse></soap:Body></soap:Envelope>
                    """.formatted(NS);
            }
            byte[] data = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(status, data.length);
            try (var out = exchange.getResponseBody()) { out.write(data); }
        });
        server.start();
        try {
            CountrySoapClient client = new CountrySoapClient(
                    java.net.URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/CountryInfoService.wso"));
            assertEqual("Berlin", client.capitalCity("de"), "successful response");
            assertTrue(lastRequest.get().contains("<sCountryISOCode>DE</sCountryISOCode>"), "request contains ISO code");
            assertTrue(lastAction.get().contains("CapitalCity"), "SOAPAction header exists");
            assertThrows(IllegalArgumentException.class, () -> client.capitalCity("DE<script>"), "reject invalid input");
            assertThrows(IOException.class, () -> client.capitalCity("XX"), "parse SOAP Fault");
            System.out.println("PASS: success, XML request, SOAPAction, validation, SOAP Fault (5 checks)");
        } finally {
            server.stop(0);
        }
    }

    private static void assertEqual(String expected, String actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    private static void assertThrows(Class<? extends Throwable> type, ThrowingRunnable action, String label) throws Exception {
        try { action.run(); } catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError(label + ": wrong exception " + ex, ex);
        }
        throw new AssertionError(label + ": expected " + type.getSimpleName());
    }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}

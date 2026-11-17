package edu.auk.sdt202.soap;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Small SOAP 1.1 client using only the Java 17 standard library. */
public final class CountrySoapClient {
    public static final URI DEFAULT_ENDPOINT = URI.create(
            "https://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso");
    private static final String NS = "http://www.oorsprong.org/websamples.countryinfo";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private final HttpClient http;
    private final URI endpoint;

    public CountrySoapClient(URI endpoint) {
        if (!"http".equalsIgnoreCase(endpoint.getScheme())
                && !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("Endpoint must use HTTP or HTTPS");
        }
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    public String capitalCity(String countryCode) throws IOException, InterruptedException {
        if (countryCode == null || !countryCode.matches("(?i)[a-z]{2}")) {
            throw new IllegalArgumentException("Enter a two-letter ISO country code, e.g. DE or UA");
        }
        String code = countryCode.toUpperCase(Locale.ROOT);
        String payload = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <CapitalCity xmlns="http://www.oorsprong.org/websamples.countryinfo">
                      <sCountryISOCode>%s</sCountryISOCode>
                    </CapitalCity>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(code);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"http://www.oorsprong.org/websamples.countryinfo/CapitalCity\"")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return parseCapital(response.body(), response.statusCode());
    }

    private static String parseCapital(String xml, int status) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList faults = document.getElementsByTagNameNS(SOAP_NS, "Fault");
            if (faults.getLength() > 0) {
                Element fault = (Element) faults.item(0);
                NodeList messages = fault.getElementsByTagName("faultstring");
                String message = messages.getLength() > 0 ? messages.item(0).getTextContent().trim() : "Unknown SOAP fault";
                throw new IOException("SOAP fault: " + message);
            }
            if (status < 200 || status >= 300) {
                throw new IOException("Web service returned HTTP " + status);
            }
            NodeList body = document.getElementsByTagNameNS(SOAP_NS, "Body");
            if (body.getLength() != 1) throw new IOException("Invalid SOAP envelope: missing Body");
            NodeList results = ((Element) body.item(0)).getElementsByTagNameNS(NS, "CapitalCityResult");
            if (results.getLength() != 1) throw new IOException("Missing CapitalCityResult in SOAP response");
            String city = results.item(0).getTextContent().trim();
            if (city.isBlank()) throw new IOException("No capital returned for this country code");
            return city;
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException ex) {
            throw new IOException("Invalid SOAP XML response", ex);
        }
    }
}

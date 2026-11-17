package edu.auk.sdt202.soap;

import java.net.URI;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java ...Main <ISO-2-code> [SOAP-endpoint-url]");
            System.err.println("Example: java ...Main DE");
            System.exit(2);
        }
        try {
            URI endpoint = args.length == 2 ? URI.create(args[1]) : CountrySoapClient.DEFAULT_ENDPOINT;
            String capital = new CountrySoapClient(endpoint).capitalCity(args[0]);
            System.out.printf("Country code: %s%nCapital city: %s%n", args[0].toUpperCase(java.util.Locale.ROOT), capital);
        } catch (Exception ex) {
            System.err.println("Request failed: " + ex.getMessage());
            System.exit(1);
        }
    }
}

Program that invokes the public **CountryInfoService** SOAP 1.1 web service and prints the capital city for a two-letter ISO country code.

## Requirements

- JDK 17 or newer; no third-party dependencies
- Internet access for a live request (local tests work offline)
- Optional: Maven 3.9+

## Compile and run (PowerShell, no Maven required)

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out src/main/java/edu/auk/sdt202/soap/*.java
java -cp out edu.auk.sdt202.soap.Main DE
java -cp out edu.auk.sdt202.soap.Main UA
```

On macOS/Linux, replace the commands with `mkdir -p out`, `javac -d out src/main/java/edu/auk/sdt202/soap/*.java`, and the same `java` command.

Optional endpoint override: `java -cp out edu.auk.sdt202.soap.Main DE https://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso`

The external service may be unavailable, rate-limited, or behave differently over time. An HTTP/network error is reported cleanly.

## Run deterministic tests (PowerShell)

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out src/main/java/edu/auk/sdt202/soap/*.java src/test/java/edu/auk/sdt202/soap/*.java
java -cp out edu.auk.sdt202.soap.CountrySoapClientTest
```

Expected output:

```
PASS: success, XML request, SOAPAction, validation, SOAP Fault (5 checks)
```

## Service details

- WSDL: https://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL
- Endpoint: https://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso
- SOAP version: 1.1; method: `CapitalCity`; input: `sCountryISOCode`; output: `CapitalCityResult`
- Input example: `DE` → typical result: `Berlin` (live response depends on the external provider)

## Implementation notes

`Main` validates CLI arguments and prints output. `CountrySoapClient` builds a SOAP XML envelope, sends it over Java's `HttpClient`, parses the response using namespace-aware DOM, and handles SOAP Faults. XML external entities are disabled. Tests use the JDK's built-in local HTTP server, so they do not rely on public service availability.

## Files

- `src/main/java/edu/auk/sdt202/soap/Main.java`: command line entry point
- `src/main/java/edu/auk/sdt202/soap/CountrySoapClient.java`: SOAP client
- `src/test/java/edu/auk/sdt202/soap/CountrySoapClientTest.java`: offline integration tests
- `pom.xml`: optional Maven project definition

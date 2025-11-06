package vn.mobileid.everification;

import vn.mobileid.everification.service.EVerification;
import vn.mobileid.everification.service.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

public class MainClass {

    static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    static ObjectMapper objectMapper = new ObjectMapper();
    private static String accessToken;
    private static final Object tokenLock = new Object();
    private static EVerification eVerification;

    public static void main(String[] args) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
        try {
            java.lang.reflect.Field charset = java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        eVerification = new EVerification();

        refreshToken("startup");

        // 2) Start HTTP server to accept file upload (multipart/form-data) or raw binary with filename query
        // port can be provided as first arg or via env EVERIFY_PORT; default 8080
        int fixedPort = 8083;
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(fixedPort), 0);
        } catch (java.net.BindException e) {
            System.err.println("Port " + fixedPort + " is already in use. Please free up the port and restart.");
            return; // Exit the application
        }
        
        server.createContext("/verify", MainClass::handleVerify);
        server.setExecutor(null);
        server.start();
        System.out.println("Verify server started at http://localhost:" + fixedPort + "/verify");
        System.out.println("POST multipart/form-data with field 'file' (or raw binary with ?filename=<name>) to verify.");
        System.out.println("Press Ctrl+C to stop.");
    }
    
    private static void refreshToken(String reason) throws Exception {
        synchronized (tokenLock) {
            System.out.println("Attempting to refresh token due to: " + reason);
            String jsonResp = eVerification.login();
            JsonNode jsonNode = objectMapper.readTree(jsonResp);
            String newAccessToken = jsonNode.path("access_token").asText(null);
            if (newAccessToken != null && !newAccessToken.isEmpty()) {
                accessToken = newAccessToken;
                System.out.println("Access token refreshed successfully.");
            } else {
                System.err.println("Failed to obtain new access token. Login response: " + jsonResp);
                throw new IOException("Failed to refresh access token.");
            }
        }
    }

    private static void handleVerify(HttpExchange exchange) throws IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        System.out.println("\n[" + requestId + "] Received new request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Only POST supported");
                return;
            }

            Headers reqHeaders = exchange.getRequestHeaders();
            String contentType = reqHeaders.getFirst("Content-Type");
            System.out.println("[" + requestId + "] Content-Type: " + contentType);
            byte[] body = readAllBytes(exchange.getRequestBody());

            String filename = null;
            byte[] fileBytes = null;

            if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
                System.out.println("[" + requestId + "] Handling as multipart/form-data.");
                // Extract boundary
                String boundary = getBoundary(contentType);
                if (boundary == null) {
                    sendResponse(exchange, 400, "Missing boundary in Content-Type");
                    return;
                }
                Map<String, Part> parts = parseMultipart(body, boundary.getBytes("UTF-8"));
                Part p = parts.get("file");
                if (p == null) {
                    System.err.println("[" + requestId + "] 'file' part is missing in multipart data.");
                    sendResponse(exchange, 400, "Missing 'file' part");
                    return;
                }
                filename = p.filename;
                fileBytes = p.data;
            } else {
                // Support raw binary with query param ?filename=xxx.pdf
                System.out.println("[" + requestId + "] Handling as raw binary upload.");
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.toLowerCase().contains("filename=")) {
                    String[] qs = query.split("&");
                    for (String kv : qs) {
                        if (kv.toLowerCase().startsWith("filename=")) {
                            filename = kv.substring(kv.indexOf('=') + 1);
                            break;
                        }
                    }
                }
                if (filename == null) {
                    System.err.println("[" + requestId + "] Filename not provided in query string for raw upload.");
                    sendResponse(exchange, 400, "No filename provided. Use multipart/form-data or ?filename=...");
                    return;
                }
                fileBytes = body;
            }

            if (filename == null || fileBytes == null) {
                System.err.println("[" + requestId + "] Could not determine filename or file bytes.");
                sendResponse(exchange, 400, "Invalid upload");
                return;
            }
            
            System.out.println("[" + requestId + "] Processing file: " + filename + " (" + fileBytes.length + " bytes)");

            String lower = filename.toLowerCase();
            Path tmpFile = Files.createTempFile("upload-" + UUID.randomUUID().toString(), getSuffixFromFilename(filename));
            Files.write(tmpFile, fileBytes);
            System.out.println("[" + requestId + "] Wrote " + fileBytes.length + " bytes to temporary file: " + tmpFile.toString());

            String jsonResp = null;
            for (int i = 0; i < 2; i++) { // Allow one retry
                try {
                    if (lower.endsWith(".pdf")) {
                        System.out.println("[" + requestId + "] Attempt " + (i+1) + ": Calling pki_v11_pades_Verify...");
                        jsonResp = eVerification.pki_v11_pades_Verify(false, false, tmpFile.toString(), accessToken);
                    } else if (lower.endsWith(".xml")) {
                        System.out.println("[" + requestId + "] Attempt " + (i+1) + ": Calling pki_v11_xades_Verify...");
                        jsonResp = eVerification.pki_v11_xades_Verify(false, false, tmpFile.toString(), accessToken);
                    } else {
                        System.err.println("[" + requestId + "] Unsupported file extension for file: " + filename);
                        sendResponse(exchange, 400, "Unsupported file extension. Supported: .pdf, .xml");
                        return;
                    }

                    // Check if response indicates an invalid token.
                    // This condition may need to be adjusted based on the actual error response from the API.
                    if (jsonResp != null && jsonResp.contains("invalid_token")) {
                         throw new IOException("Token is invalid or expired");
                    }
                    
                    break; // Success, exit retry loop
                } catch (Exception e) {
                    // Assuming an exception or a specific response indicates an expired token.
                    // This condition might need adjustment.
                    if (i == 0 && (e.getMessage().contains("Token is invalid") || (jsonResp != null && jsonResp.contains("invalid_token")))) {
                        System.out.println("[" + requestId + "] Token appears to be expired. Refreshing and retrying...");
                        try {
                            refreshToken("API call failure");
                        } catch (Exception refreshEx) {
                            System.err.println("[" + requestId + "] Failed to refresh token during retry: " + refreshEx.getMessage());
                            throw e; // Rethrow the original exception if token refresh fails
                        }
                    } else {
                        throw e; // Rethrow if it's not a token error or if retry failed
                    }
                }
            }

            // cleanup temp file
            System.out.println("[" + requestId + "] Deleting temporary file: " + tmpFile.toString());
            Files.deleteIfExists(tmpFile);

            // Return the JSON response from eVerification directly
            if (jsonResp == null) {
                throw new IOException("Failed to get verification response after retries.");
            }
            
            System.out.println("[" + requestId + "] Sending 200 OK response: " + jsonResp);
            sendResponse(exchange, 200, jsonResp, "application/json");
        } catch (Exception ex) {
            System.err.println("[" + requestId + "] An unexpected error occurred: " + ex.getMessage());
            ex.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }


    private static String getBoundary(String contentType) {
        String[] parts = contentType.split(";");
        for (String p : parts) {
            p = p.trim();
            if (p.startsWith("boundary=")) {
                String b = p.substring("boundary=".length());
                if (b.startsWith("\"") && b.endsWith("\"") && b.length() >= 2) {
                    b = b.substring(1, b.length()-1);
                }
                return b;
            }
        }
        return null;
    }

    private static class Part {
        String name;
        String filename;
        byte[] data;
    }

    // Very small multipart parser sufficient for single-file uploads
    private static Map<String, Part> parseMultipart(byte[] body, byte[] boundary) throws IOException {
        Map<String, Part> parts = new HashMap<>();
        byte[] dashBoundary = concat(new byte[] { '-', '-' }, boundary);

        int idx = indexOf(body, dashBoundary, 0);
        if (idx != 0) {
            // does not start with boundary, try find first boundary
            idx = indexOf(body, dashBoundary, 0);
            if (idx < 0) return parts;
        }
        int pos = idx + dashBoundary.length;
        while (pos < body.length) {
            // skip CRLF
            if (pos + 2 <= body.length && body[pos] == '\r' && body[pos+1] == '\n') pos += 2;
            // read headers until empty line
            int headersStart = pos;
            int headersEnd = indexOf(body, new byte[] { '\r','\n','\r','\n' }, headersStart);
            if (headersEnd < 0) break;
            String headersStr = new String(body, headersStart, headersEnd - headersStart, "UTF-8");
            Map<String, String> headerMap = parsePartHeaders(headersStr);
            pos = headersEnd + 4;
            // find next boundary
            int nextBoundary = indexOf(body, dashBoundary, pos);
            if (nextBoundary < 0) break;
            int dataEnd = nextBoundary - 2; // strip trailing CRLF before boundary
            if (dataEnd < pos) dataEnd = pos;
            byte[] data = Arrays.copyOfRange(body, pos, dataEnd);
            Part part = new Part();
            part.name = headerMap.get("name");
            part.filename = headerMap.get("filename");
            part.data = data;
            if (part.name != null) parts.put(part.name, part);
            pos = nextBoundary + dashBoundary.length;
            // check for final boundary "--"
            if (pos + 2 <= body.length && body[pos] == '-' && body[pos+1] == '-') break;
        }
        return parts;
    }

    private static Map<String,String> parsePartHeaders(String headersStr) {
        Map<String,String> map = new HashMap<>();
        String[] lines = headersStr.split("\r\n");
        for (String line : lines) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String val = line.substring(idx+1).trim();
                if ("Content-Disposition".equalsIgnoreCase(name)) {
                    // parse disposition parameters
                    String[] parts = val.split(";");
                    for (String p : parts) {
                        p = p.trim();
                        int eq = p.indexOf('=');
                        if (eq > 0) {
                            String k = p.substring(0, eq).trim();
                            String v = p.substring(eq+1).trim();
                            if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1);
                            map.put(k.toLowerCase(), v);
                        }
                    }
                } else {
                    map.put(name.toLowerCase(), val);
                }
            }
        }
        return map;
    }

    private static int indexOf(byte[] outer, byte[] inner, int from) {
        outer: for (int i = from; i <= outer.length - inner.length; i++) {
            for (int j = 0; j < inner.length; j++) {
                if (outer[i+j] != inner[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(in, baos);
        return baos.toByteArray();
    }

    private static String getSuffixFromFilename(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx >= 0) {
            return filename.substring(idx);
        }
        return ".bin";
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        sendResponse(exchange, statusCode, body, "text/plain; charset=utf-8");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void printUsage() {
        System.out.println("Functions: ");
        System.out.println("1.  /oidc/token.");
        System.out.println("2.  /v1/e-verification/pades. (PDF)");
        System.out.println("3.  /v1/e-verification/cades. (CMS)");
        System.out.println("4.  /v1/e-verification/xades. (XML)");
        System.out.println("5.  /v1.1/e-verification/pades. (PDF)");
        System.out.println("6.  /v1.1/e-verification/xades. (XML)");
        System.out.println("7. quit.");
    }
}

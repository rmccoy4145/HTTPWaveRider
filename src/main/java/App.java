import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class App {
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started on port 8080");
            while (true) {
                try(Socket socket = serverSocket.accept()) {
                    socket.setSoTimeout(5000);
                    System.out.println("Client connected: " + socket.getInetAddress());
                    try(InputStream input = socket.getInputStream();
                        OutputStream output = socket.getOutputStream())
                    {
                        requestPipeline(input, output);
                    }

                } catch (IOException e) {
                    System.out.println("Client connection error: " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    static void requestPipeline(InputStream input, OutputStream output) throws IOException {

        byte[] data = new byte[1024];
        input.read(data, 0, 1024);

        Map<String, String> requestComponents = parseHttpRequest(data);

        if (!isValidHttp(requestComponents.get("requestLine"))) {
            output.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            return;
        }

        System.out.println("Received HTTP request");

        output.write("HTTP/1.1 200 OK\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        output.write("Hello World!".getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isValidHttp(String requestLine)
    {
        if (requestLine == null) {
            return false;
        }
        System.out.printf("Received request: %s%n", requestLine);

        // Regular expression pattern to check if the request line follows the HTTP protocol
        Pattern httpPattern =
                Pattern.compile("^(GET|HEAD|POST|PUT|DELETE|CONNECT|OPTIONS|TRACE|PATCH)\\s+(\\S+)\\s+HTTP/1\\.[01]$");

        boolean match = httpPattern.matcher(requestLine.trim()).matches();
        return match;
    }

    public static Map<String, String> parseHttpRequest(byte[] data) throws IOException {
        Map<String, String> requestComponents = new HashMap<>();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        try (InputStreamReader inputStreamReader = new InputStreamReader(bais, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            // Read request line
            String requestLine = bufferedReader.readLine();
            requestComponents.put("requestLine", requestLine);

            int contentLength = 0;

            // Read headers
            StringBuilder headers = new StringBuilder();
            String headerLine;
            while (!(headerLine = bufferedReader.readLine()).isEmpty()) {
                if(headerLine.contains("Content-Length")) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
                headers.append(headerLine).append("\n");
            }
            requestComponents.put("headers", headers.toString());

            // Read body, if available
            StringBuilder body = new StringBuilder();
            String bodyLine;

            while (bufferedReader.ready() && !(bodyLine = bufferedReader.readLine()).isEmpty()) {
                body.append(bodyLine).append("\n");
            }
            if (body.length() > 0) {
                requestComponents.put("body", body.toString());
            }

        } catch (IOException e) {
            System.out.println("Error parsing HTTP request: " + e.getMessage());
            throw e;
        }
        return requestComponents;
    }
}

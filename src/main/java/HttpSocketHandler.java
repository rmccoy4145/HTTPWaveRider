import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpSocketHandler implements Runnable
{

    private final Socket socket;
    private final RequestPipeline requestPipeline = new RequestPipeline();

    public HttpSocketHandler(Socket socket)
    {
        this.socket = socket;
    }
    @Override
    public void run()
    {
        try(Socket socket = this.socket) {
            socket.setSoTimeout(5000);
            System.out.println("Client connected: " + socket.getInetAddress());
            try(InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream())
            {
                requestPipeline.process(input, output);
            }

        } catch (IOException e) {
            System.out.println("Client connection error: " + e.getMessage());
        }
    }

}

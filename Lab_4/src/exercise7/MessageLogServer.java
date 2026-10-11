package exercise7;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Bài tập đề xuất 7: Server lưu nhật ký tin nhắn
 * - Client gửi HELLO clientId trước khi gửi nội dung
 * - Chỉ chấp nhận clientId gồm chữ, số, dấu gạch ngang và gạch dưới (^[a-zA-Z0-9_-]+$)
 * - Server lưu tin nhắn theo từng client vào data/logs/<clientId>.txt
 * - Mỗi dòng gồm timestamp, địa chỉ remote và nội dung
 * - Kết thúc bằng QUIT thay cho chuỗi HET nằm lẫn trong nội dung
 */
public class MessageLogServer {
    private static final int PORT = 7008;
    private static final int MAX_CLIENTS = 20;
    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_DIR = "data/logs";

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;

        // Tạo thư mục logs nếu chưa tồn tại
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Message Log Server listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    private static void handleClient(Socket socket) {
        String remoteAddr = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("Client connected: " + remoteAddr);

        String clientId = null;

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("QUIT")) {
                    out.println("OK BYE");
                    break;
                }

                if (line.startsWith("HELLO ")) {
                    String candidateId = line.substring(6).trim();
                    if (!CLIENT_ID_PATTERN.matcher(candidateId).matches()) {
                        out.println("ERR INVALID_CLIENT_ID (Chỉ chấp nhận chữ, số, '-' và '_')");
                    } else {
                        clientId = candidateId;
                        out.println("OK HELLO_ACCEPTED " + clientId);
                        System.out.println("[" + remoteAddr + "] Registered clientId: " + clientId);
                    }
                } else {
                    if (clientId == null) {
                        out.println("ERR NOT_REGISTERED (Phải gửi 'HELLO clientId' trước khi gửi tin nhắn)");
                    } else {
                        // Lưu nhật ký vào data/logs/<clientId>.txt
                        saveLog(clientId, remoteAddr, line);
                        out.println("OK SAVED");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi phiên với client " + remoteAddr + ": " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + remoteAddr + " (clientId: " + clientId + ")");
        }
    }

    private static synchronized void saveLog(String clientId, String remoteAddr, String message) {
        File file = new File(LOG_DIR, clientId + ".txt");
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logEntry = String.format("[%s] [%s] %s%n", timestamp, remoteAddr, message);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            writer.write(logEntry);
            writer.flush();
            System.out.print("LOGGED -> " + logEntry);
        } catch (IOException e) {
            System.err.println("Lỗi ghi log file cho client " + clientId + ": " + e.getMessage());
        }
    }
}

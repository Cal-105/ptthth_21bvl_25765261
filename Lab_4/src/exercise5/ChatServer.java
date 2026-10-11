package exercise5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bài tập đề xuất 5: Chat TCP nhiều client
 * - Server dùng thread pool và ConcurrentHashMap (thread-safe)
 * - Mỗi client chọn nickname duy nhất qua lệnh NICK <nickname>
 * - Broadcast tin nhắn tới tất cả client khác
 * - Hỗ trợ các lệnh: USERS, MSG <nội_dung>, QUIT
 * - Quản lý ngắt kết nối an toàn khi client thoát bất thường
 */
public class ChatServer {
    private static final int PORT = 7005;
    private static final int MAX_CLIENTS = 30;

    // Lưu danh sách client theo nickname -> PrintWriter (thread-safe)
    private static final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Chat Server listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            System.err.println("Chat Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    private static void handleClient(Socket socket) {
        String clientAddress = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("Client connected: " + clientAddress);

        String currentNick = null;

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            out.println("WELCOME Chào mừng bạn đến Chat Server. Hãy đăng ký: NICK <nickname>");

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("QUIT")) {
                    out.println("OK BYE");
                    break;
                }

                if (line.startsWith("NICK ")) {
                    String requestedNick = line.substring(5).trim();
                    if (requestedNick.isEmpty()) {
                        out.println("ERR INVALID_NICK_FORMAT");
                        continue;
                    }
                    if (activeClients.containsKey(requestedNick)) {
                        out.println("ERR NICK_TAKEN");
                    } else {
                        if (currentNick != null) {
                            activeClients.remove(currentNick);
                            broadcast("SERVER", "Người dùng " + currentNick + " đã đổi tên thành " + requestedNick, null);
                        }
                        currentNick = requestedNick;
                        activeClients.put(currentNick, out);
                        out.println("OK NICK_ACCEPTED " + currentNick);
                        broadcast("SERVER", "Người dùng [" + currentNick + "] đã tham gia phòng chat.", currentNick);
                    }
                } else if (line.equalsIgnoreCase("USERS")) {
                    String userList = String.join(", ", activeClients.keySet());
                    out.println("USERS [" + userList + "]");
                } else if (line.startsWith("MSG ")) {
                    if (currentNick == null) {
                        out.println("ERR PLEASE_SET_NICK_FIRST (Sử dụng: NICK <nickname>)");
                    } else {
                        String msgContent = line.substring(4);
                        broadcast(currentNick, msgContent, currentNick);
                        out.println("OK SENT");
                    }
                } else {
                    out.println("ERR UNKNOWN_COMMAND (Hỗ trợ: NICK, USERS, MSG, QUIT)");
                }
            }
        } catch (IOException e) {
            System.err.println("Mất kết nối với client " + (currentNick != null ? currentNick : clientAddress) + ": " + e.getMessage());
        } finally {
            if (currentNick != null) {
                activeClients.remove(currentNick);
                broadcast("SERVER", "Người dùng [" + currentNick + "] đã rời phòng chat.", null);
                System.out.println("User disconnected: " + currentNick);
            } else {
                System.out.println("Client disconnected: " + clientAddress);
            }
        }
    }

    private static void broadcast(String sender, String message, String excludeNick) {
        String fullMsg = "[" + sender + "]: " + message;
        System.out.println("BROADCAST -> " + fullMsg);
        for (Map.Entry<String, PrintWriter> entry : activeClients.entrySet()) {
            if (excludeNick != null && excludeNick.equals(entry.getKey())) {
                continue; // Không gửi lại chính người gửi nếu muốn
            }
            try {
                entry.getValue().println(fullMsg);
            } catch (Exception ignored) {
            }
        }
    }
}

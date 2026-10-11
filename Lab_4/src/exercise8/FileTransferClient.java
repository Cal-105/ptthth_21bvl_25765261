package exercise8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Bài tập đề xuất 8: File Transfer Client
 * - Tính mã SHA-256 của file trước khi gửi
 * - Gửi header: FILE <filename> <size> <sha256>
 * - Truyền byte stream dữ liệu của file
 * - Nhận kết quả xác nhận tính toàn vẹn từ Server
 */
public class FileTransferClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7009;

        if (args.length > 2) {
            // Gửi 1 file cụ thể
            File fileToSend = new File(args[2]);
            if (!fileToSend.exists()) {
                System.err.println("File không tồn tại: " + args[2]);
                return;
            }
            sendFile(host, port, fileToSend, fileToSend.getName());
        } else {
            // Chế độ kiểm thử tự động toàn diện các ca kiểm thử theo yêu cầu đề bài
            runAutomatedTests(host, port);
        }
    }

    public static void runAutomatedTests(String host, int port) {
        System.out.println("==========================================================================");
        System.out.println("BÀI TẬP 8: KIỂM THỬ TRUYỀN FILE TCP AN TOÀN VÀ KIỂM TRA MÃ HASH SHA-256");
        System.out.println("==========================================================================");

        try {
            File testDir = new File("temp_test_files");
            if (!testDir.exists()) testDir.mkdirs();

            // Ca 1: File rỗng (0 bytes)
            File emptyFile = new File(testDir, "empty.txt");
            if (!emptyFile.exists()) emptyFile.createNewFile();
            System.out.println("\n[Ca 1] Kiểm thử File rỗng (0 bytes):");
            sendFile(host, port, emptyFile, "empty.txt");

            // Ca 2: File văn bản tiếng Việt UTF-8
            File textFile = new File(testDir, "tieng_viet.txt");
            Files.writeString(textFile.toPath(), "Cộng hòa Xã hội Chủ nghĩa Việt Nam\nĐại học Công nghiệp TP.HCM (IUH)\nBài tập Socket TCP an toàn 2026.", StandardCharsets.UTF_8);
            System.out.println("\n[Ca 2] Kiểm thử File văn bản tiếng Việt UTF-8:");
            sendFile(host, port, textFile, "tieng_viet.txt");

            // Ca 3: File nhị phân mô phỏng file ảnh (1024 bytes)
            File binFile = new File(testDir, "sample_image.png");
            byte[] dummyData = new byte[1024];
            for (int i = 0; i < dummyData.length; i++) dummyData[i] = (byte) (i % 256);
            Files.write(binFile.toPath(), dummyData);
            System.out.println("\n[Ca 3] Kiểm thử File nhị phân (Ảnh mô phỏng 1024 bytes):");
            sendFile(host, port, binFile, "sample_image.png");

            // Ca 4: Kiểm thử bảo mật Directory Traversal (tên chứa ../)
            System.out.println("\n[Ca 4] Kiểm thử bảo mật Path Traversal (tên file chứa '../'):");
            sendFile(host, port, textFile, "../../etc/passwd");

            System.out.println("\n==========================================================================");
            System.out.println("HOÀN THÀNH TẤT CẢ CÁC CA KIỂM THỬ TRUYỀN FILE AN TOÀN!");
            System.out.println("==========================================================================\n");

        } catch (Exception e) {
            System.err.println("Lỗi kiểm thử truyền file: " + e.getMessage());
        }
    }

    public static void sendFile(String host, int port, File file, String sendFileName) {
        try {
            long fileSize = file.length();
            String sha256 = calculateSha256(file);

            System.out.println("-> Chuẩn bị gửi: " + sendFileName);
            System.out.println("   Kích thước  : " + fileSize + " bytes");
            System.out.println("   Mã SHA-256  : " + sha256);

            try (Socket socket = new Socket(host, port);
                 BufferedOutputStream outStream = new BufferedOutputStream(socket.getOutputStream());
                 PrintWriter headerWriter = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
                 BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file))) {

                // 1. Gửi Header
                String header = String.format("FILE %s %d %s", sendFileName, fileSize, sha256);
                headerWriter.println(header);
                headerWriter.flush();

                // 2. Gửi nội dung file (nếu không phải ca kiểm tra lỗi path traversal trước)
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
                outStream.flush();

                // 3. Đọc phản hồi từ Server
                String response = serverReader.readLine();
                System.out.println("<- Server phản hồi: " + response);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi file '" + sendFileName + "': " + e.getMessage());
        }
    }

    private static String calculateSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).toLowerCase();
    }
}

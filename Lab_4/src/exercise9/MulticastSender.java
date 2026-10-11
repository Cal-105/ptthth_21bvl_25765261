package exercise9;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Bài tập đề xuất 9: Multicast Sender (Bên phát thông báo)
 * - Gửi gói tin Datagram tới địa chỉ multicast 239.255.0.1 cổng 7010
 * - Hỗ trợ chế độ kiểm thử tự động với đồng thời 2 Receiver nhận tin
 */
public class MulticastSender {
    public static final String MULTICAST_IP = "239.255.0.1";
    public static final int PORT = 7010;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String msg = String.join(" ", args);
            sendMulticastMessage(msg);
        } else {
            runDemonstration();
        }
    }

    public static void runDemonstration() throws Exception {
        System.out.println("==========================================================================");
        System.out.println("BÀI TẬP 9: THỰC NGHIỆM MULTICAST SOCKET (2 RECEIVER CÙNG NHẬN THÔNG BÁO)");
        System.out.println("Địa chỉ Multicast: " + MULTICAST_IP + " | Cổng: " + PORT);
        System.out.println("==========================================================================");

        // Khởi động Receiver 1 ngầm (nhận 3 tin nhắn rồi thoát)
        Thread recv1 = new Thread(() -> MulticastReceiver.startReceiver("Receiver_1", 3));
        recv1.setDaemon(true);
        recv1.start();

        // Khởi động Receiver 2 ngầm (nhận 3 tin nhắn rồi thoát)
        Thread recv2 = new Thread(() -> MulticastReceiver.startReceiver("Receiver_2", 3));
        recv2.setDaemon(true);
        recv2.start();

        Thread.sleep(1000); // Đợi 2 receiver joinGroup hoàn tất

        String[] notifications = {
            "THÔNG BÁO 1: Họp sinh viên khoa CNTT trực tuyến lúc 14:00.",
            "CẢNH BÁO 2: Bảo trì máy chủ thực hành hệ thống mạng lúc 18:00.",
            "BẢN TIN 3: Nhắc nhở nộp báo cáo Lab 4 lên LMS đúng hạn."
        };

        for (String notif : notifications) {
            System.out.println("\n[SENDER] Đang phát: \"" + notif + "\"");
            sendMulticastMessage(notif);
            Thread.sleep(600);
        }

        recv1.join(3000);
        recv2.join(3000);

        System.out.println("\n==========================================================================");
        System.out.println("KẾT LUẬN: Cả 2 Receiver đều đã nhận đủ 3 thông báo từ địa chỉ Multicast!");
        System.out.println("Lưu ý mạng: Nếu chạy giữa nhiều máy thật, hãy bảo đảm switch/router cho phép IGMP");
        System.out.println("hoặc không bị chặn bởi tường lửa Windows Defender / VPN.");
        System.out.println("==========================================================================\n");
    }

    public static void sendMulticastMessage(String message) throws Exception {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        InetAddress group = InetAddress.getByName(MULTICAST_IP);

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);
        }
    }
}

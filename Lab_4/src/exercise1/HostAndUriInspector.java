package exercise1;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Bài tập đề xuất 1: Host và URI Inspector
 * Phân giải hostname thành địa chỉ IP (IPv4/IPv6, loopback, site local)
 * và phân tích các thành phần của URI bằng java.net.URI.
 */
public class HostAndUriInspector {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("===============================================================");
            System.out.println("Cú pháp sử dụng: java exercise1.HostAndUriInspector <hostname> <uri>");
            System.out.println("Ví dụ: java exercise1.HostAndUriInspector example.com https://example.com:8080/path/test?id=1#sec");
            System.out.println("===============================================================");
            return;
        }

        String host = args[0];
        String uriStr = args[1];

        System.out.println("---------------------------------------------------------------");
        System.out.println("1. KHẢO SÁT HOSTNAME: " + host);
        System.out.println("---------------------------------------------------------------");
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                System.out.println("- IP: " + address.getHostAddress());
                System.out.println("  Canonical : " + address.getCanonicalHostName());
                System.out.println("  Loopback  : " + address.isLoopbackAddress());
                System.out.println("  Site local: " + address.isSiteLocalAddress());
                String type = (address instanceof Inet4Address) ? "IPv4"
                        : (address instanceof Inet6Address) ? "IPv6" : "Unknown";
                System.out.println("  Loại IP   : " + type);
            }
        } catch (UnknownHostException e) {
            System.err.println("Lỗi phân giải hostname: Không tìm thấy host '" + host + "' (" + e.getMessage() + ")");
        }

        System.out.println("\n---------------------------------------------------------------");
        System.out.println("2. PHÂN TÍCH CÚ PHÁP URI: " + uriStr);
        System.out.println("---------------------------------------------------------------");
        try {
            URI uri = new URI(uriStr);
            System.out.println("- Scheme   : " + (uri.getScheme() != null ? uri.getScheme() : "(không có)"));
            System.out.println("- Host     : " + (uri.getHost() != null ? uri.getHost() : "(không có)"));
            System.out.println("- Port     : " + (uri.getPort() != -1 ? uri.getPort() : "(không có/mặc định)"));
            System.out.println("- Path     : " + (uri.getPath() != null && !uri.getPath().isEmpty() ? uri.getPath() : "(trống)"));
            System.out.println("- Query    : " + (uri.getQuery() != null ? uri.getQuery() : "(không có)"));
            System.out.println("- Fragment : " + (uri.getFragment() != null ? uri.getFragment() : "(không có)"));
        } catch (URISyntaxException e) {
            System.err.println("Lỗi cú pháp URI: Chuỗi URI '" + uriStr + "' không hợp lệ (" + e.getMessage() + ")");
        }
        System.out.println("===============================================================\n");
    }
}

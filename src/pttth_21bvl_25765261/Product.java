package pttth_21bvl_25765261;

public class Product {
    private final String code;
    private final String name;
    private final double unitPrice;
    private final int quantity;

    public Product(String code, String name,
                   double unitPrice, int quantity) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã không được rỗng");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên không được rỗng");
        }

        if (unitPrice <= 0 || quantity < 0) {
            throw new IllegalArgumentException(
                    "Giá hoặc số lượng không hợp lệ");
        }

        this.code = code;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public double inventoryValue() {
        return unitPrice * quantity;
    }

    public String toCsv() {
        return code + "," + name + "," + unitPrice
                + "," + quantity;
    }

    @Override
    public String toString() {
        return "%s - %s: %,.0f VND".formatted(
                code, name, inventoryValue());
    }
}
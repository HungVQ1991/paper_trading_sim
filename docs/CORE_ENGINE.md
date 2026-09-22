# HƯỚNG DẪN KỸ THUẬT & ĐẶC TẢ PHẦN VIỆC: THÀNH VIÊN 1
## PHÂN HỆ: ĐỘNG CƠ GIAO DỊCH & MÔ HÌNH NGHIỆP VỤ (CORE DOMAIN & TRADING ENGINE)

---

## 1. MỤC TIÊU VÀ PHẠM VI TRÁCH NHIỆM
Thành viên 1 chịu trách nhiệm xây dựng nền tảng nghiệp vụ cốt lõi của ứng dụng, bao gồm:
- Toàn bộ các lớp thực thể dữ liệu (Entities/Models) thuộc gói `com.trading.model`.
- Động cơ xử lý giao dịch và khớp lệnh `TradingEngine` thuộc gói `com.trading.engine`.
- Đảm bảo tính toàn vẹn dữ liệu tài chính, an toàn đa luồng (Thread-safety) và áp dụng các nguyên lý OOP (Đóng gói, Kế thừa, Đa hình).
- Xây dựng bộ Unit Test tự động cho toàn bộ logic nghiệp vụ tính toán và khớp lệnh.

---

## 2. DANH SÁCH TỆP MÃ NGUỒN PHỤ TRÁCH

```text
src/
├── main/java/com/trading/
│   ├── model/
│   │   ├── Order.java                  # Abstract class cơ sở cho các loại lệnh
│   │   ├── MarketOrder.java            # Lớp con: Lệnh thị trường (khớp ngay)
│   │   ├── LimitOrder.java             # Lớp con: Lệnh giới hạn (chờ giá)
│   │   ├── OrderStatus.java            # Enum: PENDING, FILLED, CANCELLED, REJECTED
│   │   ├── Position.java               # Quản lý vị thế cổ phiếu & giá vốn bình quân
│   │   ├── Account.java                # Quản lý số dư tiền mặt & danh mục tổng
│   │   ├── Quote.java                  # Dữ liệu giá thị trường tức thời
│   │   └── Trade.java                  # Bản ghi giao dịch đã khớp thành công
│   └── engine/
│       └── TradingEngine.java          # Động cơ điều phối đặt lệnh & khớp lệnh
└── test/java/com/trading/
    ├── model/
    │   ├── OrderPolymorphismTest.java  # Test tính đa hình của Order
    │   └── PositionCalculationTest.java# Test tính giá vốn bình quân & Realized P&L
    └── engine/
        └── TradingEngineTest.java      # Test quy trình đặt lệnh, trừ tiền, khớp lệnh Limit
```

---

## 3. ĐẶC TẢ CHI TIẾT CÁC LỚP & PHƯƠNG THỨC

### 3.1. Phân hệ `com.trading.model`

#### 1. `public abstract class Order`
- **Ý nghĩa nghiệp vụ:** Đại diện trừu tượng cho một yêu cầu mua hoặc bán cổ phiếu.
- **Thuộc tính:**
  - `private final String orderId`: Khởi tạo bằng `UUID.randomUUID().toString()`.
  - `private final String symbol`: Mã chứng khoán (ví dụ: "AAPL").
  - `private final boolean isBuy`: `true` nếu là lệnh Mua, `false` nếu là lệnh Bán.
  - `private final int quantity`: Khối lượng đặt (phải > 0).
  - `private OrderStatus status`: Trạng thái ban đầu là `OrderStatus.PENDING`.
  - `private final Instant createdAt`: `Instant.now()`.
- **Phương thức bắt buộc:**
  - Getters đầy đủ cho các trường; `setStatus(OrderStatus status)`.
  - `public abstract boolean canExecute(double currentPrice)`: Phương thức trừu tượng (*Polymorphism*).

#### 2. `public class MarketOrder extends Order`
- **Constructor:** `public MarketOrder(String symbol, boolean isBuy, int quantity)`
- **Hiện thực phương thức:**
  - `public boolean canExecute(double currentPrice)`: Luôn trả về `true` (khớp tức thời tại giá thị trường).

#### 3. `public class LimitOrder extends Order`
- **Thuộc tính bổ sung:**
  - `private final double limitPrice`: Mức giá giới hạn kỳ vọng.
- **Constructor:** `public LimitOrder(String symbol, boolean isBuy, int quantity, double limitPrice)`
- **Hiện thực phương thức:**
  - `public boolean canExecute(double currentPrice)`:
    - Nếu `isBuy == true`: Trả về `currentPrice <= limitPrice` (chỉ mua khi giá rẻ hơn hoặc bằng giá trần).
    - Nếu `isBuy == false`: Trả về `currentPrice >= limitPrice` (chỉ bán khi giá cao hơn hoặc bằng giá sàn).

#### 4. `public class Position`
- **Ý nghĩa nghiệp vụ:** Quản lý số lượng và giá vốn của một mã cổ phiếu mà tài khoản đang nắm giữ.
- **Thuộc tính:**
  - `private final String symbol`: Mã chứng khoán.
  - `private int quantity`: Số lượng cổ phiếu nắm giữ.
  - `private double averagePrice`: Giá vốn bình quân trên mỗi cổ phiếu.
- **Phương thức cốt lõi:**
  - `public void buyMore(int additionalQty, double purchasePrice)`:
    - Công thức cập nhật giá vốn:
      $$\text{averagePrice} = \frac{(\text{quantity} \times \text{averagePrice}) + (\text{additionalQty} \times \text{purchasePrice})}{\text{quantity} + \text{additionalQty}}$$
    - Cập nhật số lượng: `quantity += additionalQty`.
  - `public double sell(int sellQty, double sellPrice)`:
    - Kiểm tra: `sellQty <= quantity`. Nếu không đủ, ném ngoại lệ hoặc trả về lỗi.
    - Tính lợi nhuận chốt lời/lỗ:
      $$\text{realizedPnL} = (\text{sellPrice} - \text{averagePrice}) \times \text{sellQty}$$
    - Giảm số lượng: `quantity -= sellQty`. Lưu ý: `averagePrice` của số cổ phiếu còn lại giữ nguyên không đổi.
    - Trả về: `realizedPnL` (`double`).
  - `public double getUnrealizedPnL(double currentPrice)`:
    - Trả về: `(currentPrice - averagePrice) * quantity`.
  - `public double getMarketValue(double currentPrice)`:
    - Trả về: `currentPrice * quantity`.

#### 5. `public class Account`
- **Ý nghĩa nghiệp vụ:** Lưu trữ số dư tiền mặt và quản lý danh mục tài sản tổng hợp.
- **Thuộc tính:**
  - `private double cashBalance`: Số dư tiền mặt khả dụng ban đầu (ví dụ: $100,000.0).
  - `private final Map<String, Position> positions`: Danh sách vị thế (Key là Ticker Symbol).
  - `private final List<Trade> tradeHistory`: Lịch sử các giao dịch đã khớp.
- **Phương thức:**
  - `public synchronized boolean deductCash(double amount)`: Trừ tiền khi mua. Nếu `amount > cashBalance` trả về `false`; ngược lại trừ tiền và trả về `true`.
  - `public synchronized void addCash(double amount)`: Cộng tiền khi bán hoặc nạp vốn.
  - `public synchronized Position getOrCreatePosition(String symbol)`: Lấy vị thế hiện tại hoặc tạo mới `Position(symbol)` nếu chưa có.
  - `public synchronized double getTotalValue(Map<String, Double> currentPrices)`:
    - Giá trị = `cashBalance + sum(position.getMarketValue(currentPrices.get(symbol)))`.
  - Getters trả về bản sao hoặc `Collections.unmodifiableList` / `Collections.unmodifiableMap` để bảo toàn tính đóng gói.

---

### 3.2. Phân hệ `com.trading.engine`

#### `public class TradingEngine`
- **Ý nghĩa nghiệp vụ:** Khối xử lý trung tâm, tiếp nhận lệnh từ giao diện, kiểm soát số dư, lưu trữ hàng đợi lệnh chờ và xử lý khớp lệnh tự động khi có giá mới từ thị trường.
- **Thuộc tính:**
  - `private final Account account`: Tham chiếu tới tài khoản người dùng.
  - `private final List<Order> pendingOrders = new CopyOnWriteArrayList<>()`: Danh sách các lệnh `LimitOrder` đang chờ kích hoạt (đảm bảo an toàn đa luồng).
- **Phương thức:**
  - `public synchronized boolean placeOrder(Order order, double currentPrice)`:
    1. Nếu là lệnh MUA: Kiểm tra `account.getCashBalance() >= order.getQuantity() * (order instanceof LimitOrder ? ((LimitOrder) order).getLimitPrice() : currentPrice)`. Nếu không đủ tiền, đặt `order.setStatus(OrderStatus.REJECTED)` và trả về `false`.
    2. Nếu là lệnh BÁN: Kiểm tra `account.getPositions().get(order.getSymbol()).getQuantity() >= order.getQuantity()`. Nếu không đủ cổ phiếu để bán, đặt `order.setStatus(OrderStatus.REJECTED)` và trả về `false`.
    3. Nếu hợp lệ:
       - Nếu `order instanceof MarketOrder`: Thực thi ngay lập tức qua hàm nội bộ `executeOrder(order, currentPrice)`.
       - Nếu `order instanceof LimitOrder`: Lưu vào `pendingOrders`, đặt `order.setStatus(OrderStatus.PENDING)`. Trả về `true`.
  - `public synchronized void onPriceUpdate(Quote quote)`:
    - Được gọi khi thị trường có giá mới (từ Thành viên 2 đẩy sang).
    - Lặp qua danh sách `pendingOrders` có `order.getSymbol().equals(quote.getSymbol())`.
    - Kiểm tra `order.canExecute(quote.getPrice())`.
    - Nếu thỏa mãn: Gọi `executeOrder(order, quote.getPrice())` và xóa lệnh khỏi `pendingOrders`.
  - `public synchronized boolean cancelOrder(String orderId)`:
    - Tìm lệnh trong `pendingOrders`, đổi trạng thái sang `CANCELLED`, xóa khỏi danh sách và trả về `true`.
  - `private void executeOrder(Order order, double execPrice)`:
    - Nếu MUA: `account.deductCash(order.getQuantity() * execPrice)`; vị thế gọi `buyMore(order.getQuantity(), execPrice)`.
    - Nếu BÁN: Vị thế gọi `realizedPnL = sell(order.getQuantity(), execPrice)`; `account.addCash(order.getQuantity() * execPrice)`.
    - Đổi trạng thái: `order.setStatus(OrderStatus.FILLED)`.
    - Tạo đối tượng `Trade` lưu vào `account.getTradeHistory()`.

---

## 4. GIAO DIỆN KẾT NỐI VỚI CÁC THÀNH VIÊN KHÁC (INTERFACE CONTRACTS)

### 1. Cung cấp cho Thành viên 3 (Analytics):
- Cung cấp phương thức `account.getTradeHistory()` trả về `List<Trade>`.
- Mỗi đối tượng `Trade` bắt buộc chứa: `symbol`, `isBuy`, `quantity`, `executedPrice`, `realizedPnL`, `timestamp`.

### 2. Cung cấp cho Thành viên 4 (UI):
- Cung cấp các phương thức điều khiển:
  - `tradingEngine.placeOrder(Order order, double currentPrice): boolean`
  - `tradingEngine.cancelOrder(String orderId): boolean`
  - `tradingEngine.getPendingOrders(): List<Order>`
  - `account.getCashBalance(): double`
  - `account.getPositions(): Map<String, Position>`
  - `account.getTotalValue(Map<String, Double> currentPrices): double`

### 3. Tiếp nhận từ Thành viên 2 (Market Data):
- Lắng nghe sự kiện giá qua hàm: `tradingEngine.onPriceUpdate(Quote quote)`.

---

## 5. HƯỚNG DẪN KIỂM THỬ ĐƠN VỊ (UNIT TESTS)

Thành viên 1 có thể hoàn thành và kiểm thử 100% mã nguồn mà không cần đợi UI hay WebSocket bằng cách viết các bài test sau trong `src/test/java/com/trading/`:

1. **`OrderPolymorphismTest.java`**:
   - Khởi tạo `MarketOrder("AAPL", true, 10)` $\rightarrow$ Kiểm tra `canExecute(100.0)` và `canExecute(200.0)` luôn trả về `true`.
   - Khởi tạo `LimitOrder("AAPL", true, 10, 150.0)` (Mua giá $\le$ 150) $\rightarrow$ `canExecute(155.0)` trả về `false`; `canExecute(150.0)` và `canExecute(145.0)` trả về `true`.
   - Khởi tạo `LimitOrder("TSLA", false, 5, 200.0)` (Bán giá $\ge$ 200) $\rightarrow$ `canExecute(195.0)` trả về `false`; `canExecute(205.0)` trả về `true`.

2. **`PositionCalculationTest.java`**:
   - Tạo vị thế rỗng. Mua lần 1: 10 cp giá $100. Mua lần 2: 10 cp giá $150.
   - Kiểm tra `quantity == 20` và `averagePrice == 125.0`.
   - Bán 10 cp giá $140 $\rightarrow$ `realizedPnL` phải bằng $(140 - 125) \times 10 = \$150.0$.
   - Kiểm tra số lượng còn lại bằng `10` và `averagePrice` vẫn bằng `125.0`.

3. **`TradingEngineTest.java`**:
   - Nạp tài khoản $1,000. Đặt lệnh Mua Market 100 cp giá $15 $\rightarrow$ Tổng tiền $1,500 > $1,000 $\rightarrow$ Kết quả `placeOrder` trả về `false`.
   - Đặt lệnh Mua Limit 10 cp giá $90 khi thị giá đang $100 $\rightarrow$ Lệnh nằm trong `pendingOrders`.
   - Bắn giá mới $95 $\rightarrow$ Lệnh vẫn `PENDING`.
   - Bắn giá mới $89 $\rightarrow$ Lệnh biến mất khỏi `pendingOrders`, tiền mặt giảm $\$890$, vị thế có 10 cp.

---

## 6. CÔNG CỤ, FRAMEWORK & THƯ VIỆN CẦN THIẾT

### 6.1. Thư viện phụ thuộc chính (Dependencies)
- **Thư viện chuẩn Java Concurrency (`java.util.concurrent.*`):**
  - `CopyOnWriteArrayList`: Quản lý danh sách `pendingOrders` an toàn khi duyệt qua danh sách trong luồng nhận giá từ WebSocket.
  - `ConcurrentHashMap`: Lưu trữ bảng ánh xạ danh mục vị thế `positions` của tài khoản người dùng.
- **Tiện ích định danh & thời gian:**
  - `java.util.UUID`: Sinh mã định danh duy nhất ngẫu nhiên cho `orderId` và `tradeId`.
  - `java.time.Instant`: Ghi nhận dấu mốc thời gian chuẩn UTC không phụ thuộc múi giờ máy client.
- **Khung kiểm thử tự động (Unit Testing tối giản):**
  - `org.junit.jupiter:junit-jupiter:5.10.2`: Sử dụng chú thích `@Test`, `@BeforeEach`, `@DisplayName` và phương thức kiểm tra số học chuẩn `assertEquals(expected, actual, delta)`.
  - `org.mockito:mockito-core:5.11.0` & `mockito-junit-jupiter`: Giả lập các lời gọi phương thức `QuoteListener` và trạng thái thị trường.

### 6.2. Công cụ phát triển hỗ trợ
- **IDE:** IntelliJ IDEA Community / Ultimate hoặc VS Code (kèm Extension Pack for Java).
- **Công cụ đo độ phủ kiểm thử (Code Coverage):** JaCoCo (đặt mục tiêu độ phủ logic nghiệp vụ $\ge 90\%$).

### 6.3. Quy chuẩn viết mã Google Checkstyle (Bắt buộc cho Thành viên 1)
- **Thụt lề:** Bắt buộc 2 spaces cho mỗi cấp block mã nguồn.
- **Bao đóng dấu ngoặc `{}`:** Mọi lệnh điều kiện trong `TradingEngine` (kiểm tra số dư, kiểm tra điều kiện khớp lệnh) bắt buộc dùng ngoặc nhọn `{}` đầy đủ:
  ```java
  // Đúng chuẩn Google Checkstyle:
  if (order.canExecute(quote.getPrice())) {
    executeOrder(order, quote.getPrice());
  }
  ```
- **Không dùng wildcard imports:** Khai báo cụ thể `import java.util.List;` thay vì `import java.util.*;`.
- **Lệnh tự kiểm tra trước khi commit:**
  ```bash
  ./gradlew checkstyleMain
  ```

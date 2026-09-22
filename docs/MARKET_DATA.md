# HƯỚNG DẪN KỸ THUẬT & ĐẶC TẢ PHẦN VIỆC: THÀNH VIÊN 2
## PHÂN HỆ: TÍCH HỢP ĐA NGUỒN DỮ LIỆU THỊ TRƯỜNG & CHUYỂN ĐỔI ĐỘNG (MULTI-API MARKET DATA & DYNAMIC SWITCHING)

---

## 1. MỤC TIÊU VÀ PHẠM VI TRÁCH NHIỆM
Thành viên 2 chịu trách nhiệm toàn bộ tầng giao tiếp dữ liệu mạng và kiến trúc quản lý nguồn cấp dữ liệu:
- Thiết kế Interface trừu tượng `MarketService` theo mẫu thiết kế **Adapter Pattern** để chuẩn hóa nhiều sàn/nhà cung cấp dữ liệu khác nhau (Twelve Data, Finnhub, Alpha Vantage, Mock).
- Xây dựng lớp quản lý trung tâm `MarketServiceProviderManager` (kết hợp **Strategy & Registry Pattern**) cho phép **chuyển đổi linh hoạt nhà cung cấp dữ liệu trực tiếp từ giao diện người dùng (Frontend)** tại thời gian chạy (runtime).
- Triển khai ít nhất 02 adapter WebSocket thực tế:
  1. `TwelveDataWebSocket`: Kết nối Twelve Data API.
  2. `FinnhubWebSocket`: Kết nối Finnhub API.
- Triển khai bộ sinh dữ liệu thị trường mô phỏng nội tuyến (`MockMarketService`) đảm bảo hệ thống có thể chạy độc lập, kiểm thử ngoại tuyến mà không cần Internet hay API Key.
- Áp dụng mẫu thiết kế **Observer Pattern** (`QuoteListener`) để phát luồng dữ liệu giá thống nhất tới `TradingEngine` và `MainController`.
- Xây dựng giao diện cấu hình hệ thống `SettingsView.fxml` và bộ điều khiển `SettingsController` hỗ trợ quản lý khóa API của từng sàn và chuyển đổi nguồn cấp.

---

## 2. DANH SÁCH TỆP MÃ NGUỒN PHỤ TRÁCH

```text
src/
├── main/
│   ├── java/com/trading/
│   │   ├── market/
│   │   │   ├── MarketProviderType.java             # Enum: TWELVE_DATA, FINNHUB, ALPHA_VANTAGE, MOCK
│   │   │   ├── MarketService.java                  # Interface chuẩn hóa hợp đồng cấp dữ liệu (Adapter)
│   │   │   ├── MarketServiceProviderManager.java   # Lớp điều phối chuyển đổi API động (Strategy Manager)
│   │   │   ├── QuoteListener.java                  # Observer Functional Interface nhận giá mới
│   │   │   ├── TwelveDataWebSocket.java            # Adapter kết nối Twelve Data WebSocket
│   │   │   ├── FinnhubWebSocket.java               # Adapter kết nối Finnhub WebSocket
│   │   │   └── MockMarketService.java              # Adapter bộ sinh giá ngẫu nhiên nội tuyến (Mock Engine)
│   │   └── ui/
│   │       └── SettingsController.java             # Điều khiển cửa sổ cài đặt API & Chuyển đổi nguồn
│   └── resources/
│       ├── fxml/
│       │   └── SettingsView.fxml                   # Cửa sổ cấu hình khóa API từng sàn & chọn nguồn
│       └── config/
│           └── config.properties                   # Lưu trữ các API Key và thiết lập mặc định
└── test/
    └── java/com/trading/market/
        ├── TwelveDataJsonParserTest.java           # Kiểm thử phân tích JSON từ Twelve Data
        ├── FinnhubJsonParserTest.java              # Kiểm thử phân tích JSON từ Finnhub
        ├── MockMarketServiceTest.java              # Kiểm thử luồng phát sự kiện và biên độ giá Mock
        └── MarketProviderSwitchingTest.java        # Kiểm thử chuyển đổi API động tại thời gian chạy
```

---

## 3. ĐẶC TẢ CHI TIẾT CÁC LỚP & GIAO DIỆN

### 3.1. `public enum MarketProviderType`
Định danh các nguồn dữ liệu được hỗ trợ:
```java
package com.trading.market;

public enum MarketProviderType {
    TWELVE_DATA("Twelve Data (WebSocket)"),
    FINNHUB("Finnhub (WebSocket)"),
    ALPHA_VANTAGE("Alpha Vantage (REST)"),
    MOCK_ENGINE("Mô Phỏng Nội Tuyến (Mock Engine)");

    private final String displayName;

    MarketProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

### 3.2. Giao diện trừu tượng `MarketService` (Adapter Interface)
Định nghĩa hợp đồng chuẩn hóa cho mọi nguồn dữ liệu:
```java
package com.trading.market;

import com.trading.model.Quote;

public interface MarketService {
    void start();
    void stop();
    void subscribe(String symbol);
    void unsubscribe(String symbol);
    Quote getLatestQuote(String symbol);
    void addListener(QuoteListener listener);
    void removeListener(QuoteListener listener);
    MarketProviderType getProviderType();
    boolean isConnected();
}
```

---

### 3.3. Bộ quản lý chuyển đổi API động `MarketServiceProviderManager`
Lớp trung tâm quản lý việc thay đổi nguồn cấp dữ liệu theo yêu cầu từ giao diện người dùng:
- **Thuộc tính:**
  - `private MarketService activeService`: Adapter đang được kích hoạt.
  - `private final Map<MarketProviderType, MarketService> providers = new ConcurrentHashMap<>()`: Danh mục các adapter sàn đã nạp.
  - `private final Set<QuoteListener> globalListeners = new CopyOnWriteArraySet<>()`: Danh sách các đối tượng quan sát toàn cục (`TradingEngine`, `MainController`).
  - `private final Set<String> subscribedSymbols = new CopyOnWriteArraySet<>()`: Tập hợp các mã cổ phiếu đang được theo dõi trên hệ thống.
- **Phương thức:**
  - `public synchronized void registerProvider(MarketProviderType type, MarketService service)`: Nạp một adapter vào danh sách quản lý.
  - `public synchronized void switchProvider(MarketProviderType newType, String apiKey)`:
    1. Kiểm tra nếu `activeService != null`: Gọi `activeService.stop()` để ngắt kết nối WebSocket hiện hành và hủy lắng nghe.
    2. Lấy adapter tương ứng với `newType`. Nếu cần cập nhật `apiKey`, thực hiện thiết lập cấu hình mới.
    3. Gán `this.activeService = newService`.
    4. Chuyển giao toàn bộ danh sách `globalListeners` sang cho `newService`.
    5. Gọi `newService.start()` để bắt đầu nhận dữ liệu.
    6. Tự động gọi `newService.subscribe(symbol)` cho toàn bộ các mã nằm trong `subscribedSymbols`.
    7. Phát thông báo thay đổi nguồn thành công ra giao diện người dùng.
  - `public MarketService getActiveService()`: Trả về adapter đang vận hành.
  - `public MarketProviderType getActiveProviderType()`: Trả về định danh loại sàn hiện tại.

---

### 3.4. Các Adapter Kết Nối Cụ Thể

#### 1. `TwelveDataWebSocket implements MarketService`
- **Endpoint:** `wss://ws.twelvedata.com/v1/quotes/price?apikey={API_KEY}`
- **Bản tin Subscribe:**
  ```json
  {"action": "subscribe", "params": {"symbols": "AAPL,MSFT,TSLA"}}
  ```
- **Bản tin tiếp nhận (JSON):**
  ```json
  {"event": "price", "symbol": "AAPL", "price": 182.45, "timestamp": 1711105400}
  ```
  Phân tích thành `Quote(symbol, price, changePercent, timestamp)` và phát tới listeners.

#### 2. `FinnhubWebSocket implements MarketService`
- **Endpoint:** `wss://ws.finnhub.io?token={API_KEY}`
- **Bản tin Subscribe:**
  ```json
  {"type": "subscribe", "symbol": "AAPL"}
  ```
- **Bản tin tiếp nhận (JSON):**
  ```json
  {
    "type": "trade",
    "data": [
      {"s": "AAPL", "p": 182.45, "t": 1711105400000, "v": 100}
    ]
  }
  ```
  Trích xuất phần tử trong mảng `data`: `s` $\rightarrow$ symbol, `p` $\rightarrow$ price, `t` $\rightarrow$ timestamp; chuẩn hóa thành đối tượng `Quote` chung.

#### 3. `MockMarketService implements MarketService`
- Khởi tạo danh sách giá cơ sở cho các mã: `AAPL` (180.0), `MSFT` (410.0), `TSLA` (175.0), `GOOGL` (150.0).
- Sử dụng `ScheduledExecutorService` định thời 1 giây/lần:
  $$\text{Price}_{t} = \text{Price}_{t-1} \times (1 + \Delta), \quad \Delta \in [-0.005, +0.005]$$
- Phát `Quote` giả lập liên tục phục vụ chạy thử nghiệm và kiểm thử tự động.

---

### 3.5. Cửa sổ Cấu hình `SettingsView.fxml` & `SettingsController.java`
- **Thành phần giao diện:**
  - `ComboBox<MarketProviderType> cmbProvider`: Chọn nguồn cấp dữ liệu mong muốn (Twelve Data, Finnhub, Mock Engine...).
  - Các trường nhập API Key tương ứng:
    - `TextField txtTwelveDataKey`
    - `TextField txtFinnhubKey`
  - Nút bấm `btnApply`:
    - Đọc lựa chọn từ `cmbProvider`.
    - Gọi hàm chuyển đổi:
      ```java
      providerManager.switchProvider(selectedType, apiKey);
      ```
    - Lưu cấu hình vào tệp `config.properties`.
  - Nút bấm `btnResetAccount`: Thiết lập lại số dư tiền mặt tài khoản về mức mặc định ($100,000).

---

## 4. GIAO DIỆN HỢP ĐỒNG KẾT NỐI (INTERFACE CONTRACTS)

### 1. Cung cấp dữ liệu cho Thành viên 1 (`TradingEngine`):
- `TradingEngine` không cần biết dữ liệu đến từ Twelve Data, Finnhub hay Mock. `TradingEngine` chỉ đăng ký làm listener qua `MarketServiceProviderManager`:
  ```java
  providerManager.addListener(quote -> tradingEngine.onPriceUpdate(quote));
  ```

### 2. Cung cấp dữ liệu cho Thành viên 4 (`MainController` - UI):
- `MainController` đăng ký nhận giá và nhận thông báo đổi nguồn:
  ```java
  providerManager.addListener(quote -> {
      Platform.runLater(() -> mainController.updateMarketTick(quote));
  });
  ```
- `MainController` có thể gắn trực tiếp một `ComboBox<MarketProviderType>` trên Header hoặc thanh công cụ để người dùng chuyển đổi nguồn nhanh chóng:
  ```java
  cmbHeaderProvider.setOnAction(e -> {
      MarketProviderType type = cmbHeaderProvider.getValue();
      providerManager.switchProvider(type, getSavedApiKey(type));
  });
  ```

---

## 5. HƯỚNG DẪN KIỂM THỬ ĐƠN VỊ (UNIT TESTS)

Thành viên 2 thực hiện kiểm thử độc lập tại `src/test/java/com/trading/market/`:

1. **`TwelveDataJsonParserTest.java`**:
   - Kiểm tra phân tích cú pháp gói tin JSON mẫu từ Twelve Data thành đối tượng `Quote(symbol="AAPL", price=182.45)`.
2. **`FinnhubJsonParserTest.java`**:
   - Kiểm tra phân tích cú pháp gói tin JSON dạng mảng của Finnhub (`data[0].s == "AAPL"`, `data[0].p == 182.45`) thành đối tượng `Quote`.
3. **`MockMarketServiceTest.java`**:
   - Khởi động Mock, kiểm tra sau 2 giây nhận được ít nhất 2 sự kiện giá, biên độ dao động hợp lệ $\pm 5\%$.
4. **`MarketProviderSwitchingTest.java`**:
   - Khởi tạo `MarketServiceProviderManager` với cả `MockMarketService` và một mock provider khác.
   - Bắt đầu ở chế độ Mock $\rightarrow$ Observer nhận giá bình thường.
   - Gọi `switchProvider(...)` sang nguồn mới $\rightarrow$ Kiểm tra nguồn cũ đã dừng (`isConnected() == false`), nguồn mới đã chạy và observer tiếp tục nhận giá từ nguồn mới mà không cần đăng ký lại.

---

## 6. CÔNG CỤ, FRAMEWORK & THƯ VIỆN CẦN THIẾT

### 6.1. Thư viện phụ thuộc chính (Dependencies)
- **Thư viện mạng & WebSocket:**
  - `org.java-websocket:Java-WebSocket:1.5.6`: Thư viện chuyên dụng kết nối WebSocket client chuẩn WSS (WebSocket Secure), cung cấp các callback sự kiện rõ ràng (`onOpen`, `onMessage`, `onClose`, `onError`) và cơ chế quản lý ping/pong tự động.
  - `java.net.http.HttpClient` (Có sẵn trong JDK 11+): Sử dụng để gọi các yêu cầu HTTP REST lấy dữ liệu lịch sử hoặc kiểm tra trạng thái sàn mà không cần kéo thêm thư viện bên ngoài như OkHttp.
- **Thư viện xử lý JSON:**
  - `com.fasterxml.jackson.core:jackson-databind:2.17.0`: Bộ phân tích cú pháp JSON sang đối tượng Java (POJO) thông qua `ObjectMapper`, hỗ trợ bóc tách mảng lồng nhau (`JsonNode`) cho các gói tin đặc thù của Finnhub.
- **Tiện ích đa luồng nội tuyến:**
  - `java.util.concurrent.ScheduledExecutorService`: Bộ định thời thực hiện chu kỳ sinh giá ngẫu nhiên mỗi 1000ms cho `MockMarketService`.
- **Khung kiểm thử tự động (Unit Testing):**
  - `org.junit.jupiter:junit-jupiter:5.10.2`: Kiểm thử logic phân tích JSON và luồng sự kiện.
  - `org.mockito:mockito-core:5.11.0`: Giả lập phản hồi từ WebSocket mà không cần mở kết nối mạng thật.

### 6.2. Công cụ phát triển & kiểm thử mạng hỗ trợ
- **Postman / Hoppscotch:** Dùng để gửi yêu cầu và kiểm tra cấu trúc gói tin JSON trả về của Twelve Data REST/WebSocket và Finnhub trước khi bắt tay vào viết mã nguồn.
- **`wscat` (CLI WebSocket Client):** Công cụ dòng lệnh kiểm tra kết nối WebSocket tức thì:
  ```bash
  wscat -c "wss://ws.twelvedata.com/v1/quotes/price?apikey={YOUR_KEY}"
  ```
- **Gluon SceneBuilder 21:** Dùng để chỉnh sửa và bố trí giao diện cửa sổ cấu hình `SettingsView.fxml`.

### 6.3. Quy chuẩn viết mã Google Checkstyle (Bắt buộc cho Thành viên 2)
- **Hằng số cấu hình (Constants):** Đặt tên dạng `UPPER_SNAKE_CASE` và có phạm vi `private static final`:
  ```java
  private static final String DEFAULT_TWELVE_DATA_URL = "wss://ws.twelvedata.com/v1/quotes/price";
  ```
- **Thụt lề:** Chuẩn 2 spaces, không dùng ký tự Tab.
- **Giới hạn độ dài dòng:** Tối đa 100 ký tự. Khi chuỗi URL hoặc JSON dài, phải ngắt dòng hợp lý.
- **Không dùng wildcard import:** Khai báo từng lớp rõ ràng, không dùng `import com.trading.market.*`.
- **Lệnh tự kiểm tra trước khi commit:**
  ```bash
  ./gradlew checkstyleMain
  ```

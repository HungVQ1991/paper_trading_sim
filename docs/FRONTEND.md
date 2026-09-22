# HƯỚNG DẪN KỸ THUẬT & ĐẶC TẢ PHẦN VIỆC: THÀNH VIÊN 4
## PHÂN HỆ: GIAO DIỆN NGƯỜI DÙNG & TRẢI NGHIỆM (JAVAFX UI/UX & FRONTEND)

---

## 1. MỤC TIÊU VÀ PHẠM VI TRÁCH NHIỆM
Thành viên 4 chịu trách nhiệm xây dựng toàn bộ lớp hiển thị (Presentation Layer) và luồng tương tác người dùng:
- Xây dựng lớp khởi động ứng dụng `MainApp` kế thừa từ `javafx.application.Application`.
- Thiết kế các tệp giao diện FXML chính (`MainView.fxml`, `OrderDialog.fxml`) bằng SceneBuilder hoặc soạn thảo trực tiếp.
- Cài đặt các bộ điều khiển trung tâm (`MainController`, `OrderDialogController`).
- Đảm bảo tính an toàn luồng (Thread-safety) của JavaFX: Đồng bộ toàn bộ dữ liệu từ luồng nền WebSocket vào giao diện thông qua `Platform.runLater()`.
- Xây dựng phong cách hiển thị chuyên nghiệp chuẩn sàn giao dịch hiện đại thông qua tệp CSS `dark-theme.css`.
- Xây dựng bộ kiểm thử tính hợp lệ của dữ liệu đầu vào trên form đặt lệnh.

---

## 2. DANH SÁCH TỆP MÃ NGUỒN PHỤ TRÁCH

```text
src/
├── main/
│   ├── java/com/trading/
│   │   ├── MainApp.java                    # Điểm khởi động ứng dụng JavaFX
│   │   └── ui/
│   │       ├── MainController.java         # Điều phối giao diện Dashboard trung tâm
│   │       └── OrderDialogController.java  # Điều phối hộp thoại đặt lệnh Mua/Bán
│   └── resources/
│       ├── fxml/
│       │   ├── MainView.fxml               # Bố cục màn hình Dashboard
│       │   └── OrderDialog.fxml            # Bố cục cửa sổ pop-up đặt lệnh
│       └── css/
│           └── dark-theme.css              # Bảng màu Dark Theme chuẩn ứng dụng Trading
└── test/
    └── java/com/trading/ui/
        └── OrderDialogValidationTest.java  # Kiểm thử logic kiểm tra dữ liệu form đặt lệnh
```

---

## 3. ĐẶC TẢ CHI TIẾT GIAO DIỆN & BỘ ĐIỀU KHIỂN

### 3.1. `MainApp.java` (Khởi động ứng dụng)
- Tải tệp `MainView.fxml`.
- Tải tệp kiểu dáng `dark-theme.css`.
- Thiết lập tiêu đề cửa sổ: `"Paper Trading Simulator - Realtime System"`.
- Đảm bảo cơ chế đóng ứng dụng sạch sẽ (`stage.setOnCloseRequest`): Ngắt kết nối `MarketService` để không bị treo tiến trình chạy nền.

---

### 3.2. Cửa sổ chính `MainView.fxml` & `MainController.java`

#### Bố cục 4 khu vực chức năng:
```
+---------------------------------------------------------------------------------+
|  [Menu Bar]: Tệp (Xuất CSV, Thoát) | Thống Kê (Mở Báo Cáo) | Cài Đặt (API Key)  |
|  -----------------------------------------------------------------------------  |
|  [Khu vực Thống kê Nhanh (Header)]:                                             |
|   Tiền mặt: $50,000.00  |  Tổng tài sản: $105,250.00  |  Lãi/Lỗ: +$5,250 (+5.2%)|
|  -----------------------------------------------------------------------------  |
|   [BẢNG THEO DÕI GIÁ]   |              [BIỂU ĐỒ GIÁ THỜI GIAN THỰC]             |
|   Mã    Giá     Tăng/Giảm|                   (JavaFX LineChart)                 |
|   AAPL  182.50  +1.20%  |   Series: Dữ liệu biến động giá theo từng giây       |
|   MSFT  415.20  -0.45%  |                                                       |
|   TSLA  175.80  +3.10%  |                                                       |
|   [Nút: Đặt Lệnh Mới]   |                                                       |
|  -----------------------------------------------------------------------------  |
|   [BẢNG DANH MỤC & LỊCH SỬ - TabPane]                                           |
|   [Tab 1: Vị thế nắm giữ] | [Tab 2: Lệnh chờ khớp] | [Tab 3: Lịch sử giao dịch] |
+---------------------------------------------------------------------------------+
```

#### Các thành phần JavaFX gắn mã `@FXML`:
- **Thẻ hiển thị chỉ số & Trạng thái nguồn dữ liệu:**
  - `@FXML private Label lblCashBalance;`
  - `@FXML private Label lblTotalEquity;`
  - `@FXML private Label lblTotalPnL;`
  - `@FXML private ComboBox<MarketProviderType> cmbProviderSelector;` (Bộ chuyển đổi nguồn dữ liệu trực tiếp)
  - `@FXML private Label lblProviderStatus;` (Hiển thị trạng thái kết nối Live/Mock)
- **Menu Bar điều hướng:**
  - `Menu mnuDataSource`: Chứa các `RadioMenuItem` cho phép đổi nguồn (Twelve Data, Finnhub, Mock) nhanh chóng.
  - `Menu mnuSettings`: Mở hộp thoại cấu hình chi tiết `SettingsView.fxml`.
- **Bảng theo dõi giá (Watchlist):**
  - `@FXML private TableView<Quote> tblWatchlist;`
  - `@FXML private TableColumn<Quote, String> colSymbol;`
  - `@FXML private TableColumn<Quote, Double> colPrice;`
  - `@FXML private TableColumn<Quote, Double> colChange;`
- **Biểu đồ thời gian thực:**
  - `@FXML private LineChart<String, Number> chartRealtime;`
  - `@FXML private XYChart.Series<String, Number> seriesPrice;`
- **Bảng quản lý vị thế (Positions Table):**
  - `@FXML private TableView<Position> tblPositions;` (Cột: Mã, Khối lượng, Giá vốn bình quân, Thị giá, Lãi/lỗ tạm tính, Thao tác Bán).
- **Bảng lệnh chờ (Pending Orders Table):**
  - `@FXML private TableView<Order> tblPendingOrders;` (Cột: Mã lệnh, Mã CP, Chiều Mua/Bán, Giá Limit, Thao tác Hủy).

#### Phương thức xử lý luồng cập nhật giá:
```java
public void onQuoteReceived(Quote quote) {
    // Bắt buộc sử dụng Platform.runLater để tránh ngoại lệ IllegalStateException
    Platform.runLater(() -> {
        // 1. Cập nhật dòng tương ứng trong bảng Watchlist
        updateWatchlistRow(quote);

        // 2. Nếu mã chứng khoán đang được chọn xem biểu đồ, thêm điểm dữ liệu mới
        if (quote.getSymbol().equals(selectedSymbol)) {
            String timeStr = DateTimeFormatter.ofPattern("HH:mm:ss").format(quote.getTimestamp().atZone(ZoneId.systemDefault()));
            seriesPrice.getData().add(new XYChart.Data<>(timeStr, quote.getPrice()));
            if (seriesPrice.getData().size() > 30) {
                seriesPrice.getData().remove(0); // Giữ tối đa 30 điểm gần nhất trên đồ thị
            }
        }

        // 3. Cập nhật lại định giá thị trường của các vị thế đang giữ
        refreshPositionsTable();
    });
}
```

---

### 3.3. Hộp thoại Đặt lệnh `OrderDialog.fxml` & `OrderDialogController.java`

- **Thành phần giao diện:**
  - `ComboBox<String> cmbSymbol`: Cho phép chọn mã cổ phiếu trong danh mục theo dõi.
  - `ToggleGroup grpSide`: Hai nút chọn Mua (`rbBuy`) hoặc Bán (`rbSell`).
  - `ComboBox<String> cmbOrderType`: Chọn loại lệnh: `MARKET` (Khớp ngay) hoặc `LIMIT` (Đặt giá chờ).
  - `TextField txtLimitPrice`: Trường nhập mức giá giới hạn (bị vô hiệu hóa nếu chọn lệnh `MARKET`).
  - `TextField txtQuantity`: Trường nhập khối lượng cổ phiếu (bắt buộc số nguyên $> 0$).
  - `Label lblEstimatedCost`: Hiển thị chi phí ước tính tức thời: `Khối lượng * Giá đặt`.
  - `Label lblValidationMessage`: Hiển thị thông báo lỗi nếu nhập sai định dạng hoặc số dư không đáp ứng.
- **Hành vi xử lý:**
  - Khi người dùng bấm `btnConfirm`:
    1. Kiểm tra tính hợp lệ dữ liệu (Validate).
    2. Khởi tạo đối tượng `Order` tương ứng (`MarketOrder` hoặc `LimitOrder`).
    3. Gửi tới `TradingEngine.placeOrder(order, currentPrice)`.
    4. Đóng hộp thoại nếu thành công, hoặc hiển thị cảnh báo nếu bị từ chối.

---

### 3.4. Định kiểu giao diện `dark-theme.css`

Tạo giao diện hiện đại với bảng màu chuẩn:
```css
/* Nền tối chủ đạo */
.root {
    -fx-background-color: #131722;
    -fx-font-family: "Segoe UI", Helvetica, Arial, sans-serif;
    -fx-text-fill: #d1d4dc;
}

/* Bảng dữ liệu */
.table-view {
    -fx-background-color: #1e222d;
    -fx-border-color: #2a2e39;
}
.table-view .column-header {
    -fx-background-color: #2a2e39;
    -fx-text-fill: #787b86;
}

/* Màu sắc chỉ số tài chính */
.text-profit {
    -fx-text-fill: #26a69a; /* Xanh lá: Tăng giá / Có lãi */
    -fx-font-weight: bold;
}
.text-loss {
    -fx-text-fill: #ef5350; /* Đỏ: Giảm giá / Thua lỗ */
    -fx-font-weight: bold;
}

/* Nút bấm giao dịch */
.btn-buy {
    -fx-background-color: #26a69a;
    -fx-text-fill: white;
    -fx-font-weight: bold;
}
.btn-sell {
    -fx-background-color: #ef5350;
    -fx-text-fill: white;
    -fx-font-weight: bold;
}
```

---

## 4. GIAO DIỆN HỢP ĐỒNG KẾT NỐI (INTERFACE CONTRACTS)

### 1. Kết nối với Thành viên 1 (`TradingEngine` & `Account`):
- Gọi phương thức đặt lệnh:
  ```java
  boolean success = tradingEngine.placeOrder(order, latestPrice);
  ```
- Gọi phương thức hủy lệnh chờ:
  ```java
  boolean cancelled = tradingEngine.cancelOrder(orderId);
  ```
- Liên kết danh sách hiển thị:
  - Lấy số dư: `account.getCashBalance()`.
  - Lấy danh mục vị thế: `account.getPositions()`.
  - Lấy danh sách lệnh chờ: `tradingEngine.getPendingOrders()`.

### 2. Kết nối với Thành viên 2 (`MarketServiceProviderManager`):
- Đăng ký nhận sự kiện cập nhật giá thời gian thực:
  ```java
  providerManager.addListener(this::onQuoteReceived);
  ```
- Kích hoạt chuyển đổi nhà cung cấp dữ liệu trực tiếp khi người dùng chọn từ giao diện:
  ```java
  cmbProviderSelector.setOnAction(event -> {
      MarketProviderType selected = cmbProviderSelector.getValue();
      String apiKey = configManager.getApiKey(selected);
      providerManager.switchProvider(selected, apiKey);
      lblProviderStatus.setText("Nguồn: " + selected.getDisplayName());
  });
  ```

### 3. Kết nối với Thành viên 3 (`AnalyticsController`):
- Mở cửa sổ phân tích báo cáo khi người dùng chọn Menu:
  ```java
  FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AnalyticsView.fxml"));
  Parent root = loader.load();
  AnalyticsController controller = loader.getController();
  controller.initData(account.getTradeHistory());
  ```

---

## 5. HƯỚNG DẪN KIỂM THỬ ĐƠN VỊ (UNIT TESTS)

Thành viên 4 xây dựng kiểm thử tính hợp lệ của biểu mẫu tại `src/test/java/com/trading/ui/`:

**`OrderDialogValidationTest.java`**:
- Kiểm thử logic kiểm tra số lượng: Nhập `-5` hoặc `0` $\rightarrow$ Kết quả xác thực trả về lỗi *"Số lượng phải là số nguyên dương"*.
- Kiểm thử logic kiểm tra giá giới hạn: Chọn `LIMIT` nhưng để trống giá hoặc nhập `0` $\rightarrow$ Kết quả xác thực trả về lỗi *"Giá giới hạn không hợp lệ"*.
- Kiểm thử logic tính tổng chi phí dự tính: Nhập khối lượng `10`, thị giá `150.0` $\rightarrow$ Giá trị dự tính phải hiển thị chính xác `$1,500.00`.

---

## 6. CÔNG CỤ, FRAMEWORK & THƯ VIỆN CẦN THIẾT

### 6.1. Thư viện phụ thuộc chính (Dependencies tối giản)
- **Thư viện nền tảng giao diện OpenJFX (JavaFX 21.0.2):**
  - `javafx-controls`: Cung cấp các điều khiển UI phong phú (`TableView`, `ComboBox`, `Button`, `TextField`, `TabPane`, `Label`, `RadioButton`).
  - `javafx-fxml`: Cơ chế nạp và ánh xạ cấu trúc giao diện XML sang mã điều khiển Java Controller thông qua chú thích `@FXML`.
  - `javafx-graphics`: Cung cấp công cụ vẽ biểu đồ chuyên nghiệp `LineChart`, `XYChart.Series`, `NumberAxis`, `CategoryAxis`.
- **Biểu tượng giao diện tối giản (Không cần thư viện ngoài):**
  - Sử dụng trực tiếp ký tự đồ họa Unicode chuẩn tài chính (▲ xanh tăng giá, ▼ đỏ giảm giá, ⚙ cài đặt, 📊 thống kê) kết hợp CSS JavaFX, loại bỏ hoàn toàn sự phụ thuộc vào Ikonli hoặc FontAwesomeFX.
- **Tiện ích đa luồng JavaFX:**
  - `javafx.application.Platform.runLater()`: Đảm bảo toàn bộ các tác vụ cập nhật bảng biểu, đồ thị từ luồng mạng WebSocket được đồng bộ an toàn vào luồng giao diện chính (JavaFX Application Thread).
- **Khung kiểm thử tự động (Unit Testing):**
  - `org.junit.jupiter:junit-jupiter:5.10.2`: Kiểm thử các hàm xác thực dữ liệu đầu vào (Form Validation) một cách độc lập không phụ thuộc vào vòng đời đồ họa.

### 6.2. Công cụ phát triển hỗ trợ
- **Gluon SceneBuilder 21:** Công cụ đồ họa trực quan (WYSIWYG) bắt buộc để kéo thả các khối layout (`BorderPane`, `GridPane`, `HBox`, `VBox`), gán mã `fx:id` và ánh xạ hàm sự kiện `onAction` mà không cần viết mã XML thủ công.
- **Trình soạn thảo kiểu dáng CSS:** Tích hợp trực tiếp trong IntelliJ IDEA để kiểm thử và tinh chỉnh bảng màu Dark Theme (`dark-theme.css`) theo thời gian thực.

### 6.3. Quy chuẩn viết mã Google Checkstyle (Bắt buộc cho Thành viên 4)
- **Khai báo biến FXML:** Toàn bộ các trường gắn thẻ `@FXML` phải khai báo phạm vi `private`:
  ```java
  @FXML
  private TableView<Quote> tblWatchlist;
  ```
- **Thụt lề:** Chuẩn 2 spaces cho mỗi cấp block mã nguồn.
- **Giới hạn độ dài dòng:** Tối đa 100 ký tự. Các hàm khởi tạo bảng phức tạp hoặc lambda sự kiện dài phải xuống dòng hợp lý.
- **Không dùng wildcard import:** Khai báo cụ thể `import javafx.scene.control.Button;` thay vì `import javafx.scene.control.*;`.
- **Lệnh tự kiểm tra trước khi commit:**
  ```bash
  ./gradlew checkstyleMain
  ```

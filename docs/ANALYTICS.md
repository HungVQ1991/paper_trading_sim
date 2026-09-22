# HƯỚNG DẪN KỸ THUẬT & ĐẶC TẢ PHẦN VIỆC: THÀNH VIÊN 3
## PHÂN HỆ: THỐNG KÊ, PHÂN TÍCH HIỆU SUẤT & XUẤT BÁO CÁO (ANALYTICS & EXPORT)

---

## 1. MỤC TIÊU VÀ PHẠM VI TRÁCH NHIỆM
Thành viên 3 chịu trách nhiệm xử lý toàn bộ logic tính toán tài chính sau giao dịch và báo cáo thống kê:
- Hiện thực các thuật toán đo lường hiệu suất giao dịch và quản trị rủi ro (`AnalyticsService`).
- Xây dựng mô-đun kết xuất dữ liệu lịch sử giao dịch ra tập tin định dạng `.csv` (`CsvExporter`) tương thích hoàn toàn với Microsoft Excel.
- Thiết kế giao diện báo cáo chuyên sâu `AnalyticsView.fxml` và bộ điều khiển `AnalyticsController`.
- Xây dựng bộ kiểm thử đơn vị tự động kiểm chứng tính chính xác của các công thức tài chính.

---

## 2. DANH SÁCH TỆP MÃ NGUỒN PHỤ TRÁCH

```text
src/
├── main/
│   ├── java/com/trading/
│   │   ├── analytics/
│   │   │   ├── AnalyticsService.java       # Lớp tiện ích cung cấp các giải thuật thống kê
│   │   │   └── CsvExporter.java            # Xuất dữ liệu lịch sử giao dịch ra chuẩn RFC 4180
│   │   └── ui/
│   │       └── AnalyticsController.java    # Điều khiển cửa sổ hiển thị báo cáo & vẽ đồ thị vốn
│   └── resources/
│       └── fxml/
│           └── AnalyticsView.fxml          # Giao diện tổng kết các chỉ số hiệu suất
└── test/
    └── java/com/trading/analytics/
        ├── AnalyticsServiceTest.java       # Kiểm thử giải thuật Win Rate, Drawdown, P&L
        └── CsvExporterTest.java            # Kiểm thử cấu trúc và tính toàn vẹn của tệp CSV
```

---

## 3. ĐẶC TẢ THUẬT TOÁN & PHƯƠNG THỨC

### 3.1. Phân hệ Thống kê `AnalyticsService.java`

Lớp cung cấp các phương thức tĩnh (`public static`) thao tác trên danh sách `List<Trade>` và chuỗi biến động tài sản `List<Double>`:

#### 1. Tính toán Tỷ lệ thắng (Win Rate)
- **Ký hiệu hàm:** `public static double calculateWinRate(List<Trade> trades)`
- **Quy tắc nghiệp vụ:** Chỉ xét các giao dịch BÁN đã chốt lời/lỗ (`trade.getRealizedPnL() != 0`).
- **Thuật toán:**
  $$\text{Win Rate} = \frac{\text{Số giao dịch có } \text{realizedPnL} > 0}{\text{Tổng số giao dịch BÁN}} \times 100$$
- **Trường hợp biên:** Nếu danh sách rỗng hoặc chưa có lệnh Bán nào được thực hiện, hàm trả về `0.0`.

#### 2. Tính Tổng Lãi/Lỗ ròng (Total Realized P&L)
- **Ký hiệu hàm:** `public static double calculateTotalPnL(List<Trade> trades)`
- **Thuật toán:**
  $$\text{Total P&L} = \sum_{i=1}^{n} \text{trade}_i.\text{getRealizedPnL}()$$

#### 3. Tính Mức sụt giảm tài sản tối đa (Max Drawdown - MDD)
- **Ký hiệu hàm:** `public static double calculateMaxDrawdown(List<Double> equityHistory)`
- **Đầu vào:** Danh sách các giá trị tổng tài sản ghi nhận theo thời gian (ví dụ: sau mỗi giao dịch hoặc định kỳ).
- **Thuật toán:**
  1. Khởi tạo `peak = equityHistory.get(0)`, `maxDrawdown = 0.0`.
  2. Duyệt qua từng giá trị `equity` trong chuỗi:
     - Nếu `equity > peak`: Cập nhật đỉnh vốn mới `peak = equity`.
     - Tính tỷ lệ sụt giảm tại điểm hiện tại:
       $$\text{drawdown} = \frac{\text{peak} - \text{equity}}{\text{peak}} \times 100$$
     - Nếu `drawdown > maxDrawdown`: Cập nhật `maxDrawdown = drawdown`.
  3. Trả về `maxDrawdown` (đơn vị phần trăm, $\ge 0$).
- **Trường hợp biên:** Nếu danh sách có ít hơn 2 phần tử, trả về `0.0`.

#### 4. Tìm kiếm Giao dịch tốt nhất & kém nhất
- `public static Optional<Trade> getBestTrade(List<Trade> trades)`: Tìm lệnh có `realizedPnL` cao nhất.
- `public static Optional<Trade> getWorstTrade(List<Trade> trades)`: Tìm lệnh có `realizedPnL` thấp nhất (khoản lỗ lớn nhất).

---

### 3.2. Mô-đun Xuất báo cáo `CsvExporter.java`

- **Ký hiệu hàm:** `public static void exportTrades(List<Trade> trades, Path targetPath) throws IOException`
- **Yêu cầu định dạng:** Tuân thủ tiêu chuẩn RFC 4180.
- **Tiêu đề cột:**
  ```text
  TradeID,Timestamp,Symbol,Action,Quantity,ExecutedPrice,RealizedPnL
  ```
- **Xử lý mã hóa:** Ghi tệp sử dụng bảng mã `StandardCharsets.UTF_8` có kèm tiền tố BOM (`\uFEFF`) để Microsoft Excel hiển thị ký tự chính xác.
- **Dữ liệu mỗi dòng:**
  ```text
  TR-001,2026-03-24T10:15:30Z,AAPL,BUY,10,182.50,0.00
  TR-002,2026-03-24T14:20:10Z,AAPL,SELL,10,188.00,55.00
  ```

---

### 3.3. Cửa sổ Báo cáo `AnalyticsView.fxml` & `AnalyticsController.java`

- **Thành phần giao diện:**
  - **Khu vực tóm tắt (Cards/Tiles):**
    - Nhãn hiển thị **Tỷ lệ thắng (Win Rate %)**.
    - Nhãn hiển thị **Lãi/Lỗ ròng ($)**: Tự động đổi màu chữ (Màu xanh dương `#26a69a` nếu dương, màu đỏ `#ef5350` nếu âm).
    - Nhãn hiển thị **Tổng số giao dịch** và **Max Drawdown (%)**.
  - **Khu vực đồ thị:**
    - `LineChart` hoặc `AreaChart`: Biểu diễn đường cong tăng trưởng vốn danh mục (*Equity Curve*) theo thời gian.
  - **Khu vực thao tác:**
    - Nút bấm `btnExportCsv`: Kích hoạt `FileChooser` cho phép người dùng chọn thư mục lưu tệp báo cáo `trading_report.csv`.

---

## 4. GIAO DIỆN HỢP ĐỒNG KẾT NỐI (INTERFACE CONTRACTS)

### 1. Tiếp nhận dữ liệu từ Thành viên 1 (`Account` & `Trade`):
- Thành viên 3 gọi: `account.getTradeHistory()` để lấy danh sách giao dịch.
- Mỗi đối tượng `Trade` cung cấp:
  - `trade.getRealizedPnL()`: Dùng để tính tổng lãi/lỗ và tỷ lệ thắng.
  - `trade.isBuy()`: Dùng để xác định chiều giao dịch.
  - `trade.getExecutedPrice()`: Dùng để xuất dữ liệu báo cáo.

### 2. Tích hợp vào Thành viên 4 (Giao diện chính `MainController`):
- `MainController` gắn sự kiện mở cửa sổ `AnalyticsView.fxml` khi người dùng nhấn vào menu "Báo Cáo / Thống Kê":
  ```java
  AnalyticsController controller = loader.getController();
  controller.loadData(account.getTradeHistory(), account.getEquityHistory());
  ```

---

## 5. HƯỚNG DẪN KIỂM THỬ ĐƠN VỊ (UNIT TESTS)

Thành viên 3 xây dựng các ca kiểm thử độc lập tại `src/test/java/com/trading/analytics/`:

1. **`AnalyticsServiceTest.java`**:
   - **Ca kiểm thử Win Rate:**
     - Tạo 4 giao dịch BÁN: 3 giao dịch có `realizedPnL > 0` (+100, +50, +200) và 1 giao dịch có `realizedPnL < 0` (-80).
     - Xác nhận `calculateWinRate(trades)` trả về chính xác `75.0` (dung sai `0.001`).
     - Kiểm tra trường hợp danh sách rỗng $\rightarrow$ trả về `0.0`.
   - **Ca kiểm thử Max Drawdown:**
     - Tạo chuỗi biến động vốn: `[100000.0, 120000.0, 90000.0, 110000.0]`.
     - Phân tích: Đỉnh cao nhất là 120,000; đáy sau đỉnh là 90,000. Mức giảm: $(120,000 - 90,000) / 120,000 = 25.0\%$.
     - Xác nhận `calculateMaxDrawdown(equity)` trả về chính xác `25.0`.

2. **`CsvExporterTest.java`**:
   - Tạo tập danh sách giao dịch mẫu `List<Trade>`.
   - Xuất dữ liệu ra một tệp tạm (`Files.createTempFile("test_trades", ".csv")`).
   - Đọc lại nội dung tệp:
     - Xác nhận dòng đầu tiên chứa đầy đủ các cột tiêu đề tiêu chuẩn.
     - Xác nhận số dòng tương ứng với số lượng giao dịch.
     - Xác nhận các giá trị số và ngày tháng được định dạng chính xác không bị lỗi phân cách.

---

## 6. CÔNG CỤ, FRAMEWORK & THƯ VIỆN CẦN THIẾT

### 6.1. Thư viện phụ thuộc chính (Dependencies tối giản)
- **Xử lý tệp bảng tính CSV (Sử dụng JDK 17/21 thuần, 0 dependency ngoài):**
  - `java.nio.file.Files`, `java.nio.file.Path`: Quản lý đường dẫn và tệp tin hiệu năng cao.
  - `java.io.BufferedWriter`: Ghi từng dòng CSV theo định dạng chuẩn RFC 4180. Tự động chèn tiền tố UTF-8 BOM (`\uFEFF`) để Microsoft Excel hiển thị ký tự tiếng Việt chính xác mà không cần kéo thêm thư viện bên ngoài như Apache Commons CSV hay OpenCSV.
- **Khung kiểm thử tự động (Unit Testing):**
  - `org.junit.jupiter:junit-jupiter:5.10.2`: Sử dụng trực tiếp phương thức kiểm tra số thực tài chính có dung sai tích hợp sẵn:
    ```java
    assertEquals(75.0, winRate, 0.001);
    assertEquals(25.0, maxDrawdown, 0.001);
    ```

### 6.2. Công cụ phát triển hỗ trợ
- **Microsoft Excel / LibreOffice Calc / Google Sheets:** Công cụ trực quan mở và kiểm tra tệp `trading_report.csv` sau khi xuất: kiểm tra định dạng phân tách cột, định dạng ngày tháng ISO 8601 và hiển thị dấu số thập phân.
- **Gluon SceneBuilder 21:** Công cụ kéo thả trực quan để thiết kế cửa sổ báo cáo và gắn biểu đồ `AreaChart` / `LineChart` trong `AnalyticsView.fxml`.

### 6.3. Quy chuẩn viết mã Google Checkstyle (Bắt buộc cho Thành viên 3)
- **Lớp tiện ích (Utility Classes):** Các lớp chứa hàm tĩnh như `AnalyticsService` hoặc `CsvExporter` bắt buộc phải có constructor rỗng `private` để ngăn chặn việc khởi tạo đối tượng ngoài ý muốn:
  ```java
  public final class AnalyticsService {
    private AnalyticsService() {
      // Ngăn chặn khởi tạo instance
    }
    // ...
  }
  ```
- **Thụt lề:** Chuẩn 2 spaces cho mỗi cấp block mã.
- **Giới hạn độ dài dòng:** Tối đa 100 ký tự. Các công thức toán học hoặc chuỗi header CSV dài phải ngắt dòng rõ ràng.
- **Không dùng wildcard import:** Khai báo từng lớp rõ ràng, không dùng `import java.util.*`.
- **Lệnh tự kiểm tra trước khi commit:**
  ```bash
  ./gradlew checkstyleMain
  ```

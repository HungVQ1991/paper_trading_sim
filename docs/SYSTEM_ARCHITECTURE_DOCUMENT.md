# TÀI LIỆU ĐẶC TẢ KIẾN TRÚC VÀ THIẾT KẾ HỆ THỐNG
## HỆ THỐNG GIẢ LẬP GIAO DỊCH CHỨNG KHOÁN (PAPER TRADING SIMULATION SYSTEM)

---

## 1. TỔNG QUAN HỆ THỐNG

### 1.1. Mục đích
Hệ thống **Paper Trading Simulation** là ứng dụng độc lập trên nền tảng desktop được xây dựng bằng ngôn ngữ **Java** và thư viện giao diện **JavaFX**. Mục tiêu của phần mềm là cung cấp môi trường mô phỏng giao dịch chứng khoán theo thời gian thực sử dụng dữ liệu thị trường thực tế hỗ trợ **kết nối đa dạng nhà cung cấp API** (Twelve Data, Finnhub, Alpha Vantage, hoặc bộ sinh dữ liệu Mock nội tuyến), cho phép **chuyển đổi linh hoạt nguồn cấp dữ liệu trực tiếp từ giao diện người dùng (Frontend)** tại thời gian chạy (runtime), đồng thời hỗ trợ thống kê hiệu suất danh mục đầu tư.

### 1.2. Phạm vi dự án
Dự án được triển khai bởi nhóm phát triển gồm **04 thành viên**, tập trung vào các yêu cầu kỹ thuật:
- Áp dụng các nguyên lý cốt lõi của lập trình hướng đối tượng (OOP: Encapsulation, Inheritance, Polymorphism, Abstraction).
- Vận dụng các mẫu thiết kế phần mềm chuẩn mực: **Adapter Pattern** (chuẩn hóa các API sàn khác nhau), **Strategy & Factory Pattern** (quản trị và chuyển đổi động nguồn cấp dữ liệu), **Observer Pattern** (phát luồng dữ liệu giá thời gian thực).
- Xử lý bất đồng bộ và luồng dữ liệu thời gian thực (Concurrency, Event-driven, JavaFX Threading Model).
- Đảm bảo chất lượng mã nguồn thông qua kiểm thử tự động (Unit Testing với JUnit 5).

---

## 2. NGHIỆP VỤ VÀ KHÁI NIỆM TÀI CHÍNH CĂN BẢN

Bảng đối chiếu và định nghĩa các thực thể tài chính áp dụng trong hệ thống:

| Thuật ngữ | Khái niệm kỹ thuật | Cơ chế tính toán trong hệ thống |
|---|---|---|
| **Ticker / Symbol** | Mã định danh duy nhất của chứng khoán niêm yết trên sàn (ví dụ: `AAPL`, `MSFT`, `TSLA`). | Kiểu dữ liệu chuỗi (`String`), đóng vai trò khóa định danh trong danh mục. |
| **Bid / Ask & Spread** | `Bid` là mức giá cao nhất người mua chờ mua; `Ask` là mức giá thấp nhất người bán chào bán. | `Spread = Ask - Bid`. Mô hình khớp lệnh sử dụng giá thị trường tức thời (`Current Price`). |
| **Candlestick (OHLCV)** | Mô hình nến biểu diễn biến động giá trong một chu kỳ thời gian (Mở cửa, Cao nhất, Thấp nhất, Đóng cửa, Khối lượng). | Cung cấp dữ liệu phục vụ trực quan hóa biểu đồ kỹ thuật (`Open`, `High`, `Low`, `Close`, `Volume`). |
| **Market Order** | Lệnh mua hoặc bán thực hiện ngay lập tức tại mức giá tốt nhất hiện có trên thị trường. | Lệnh được khớp tức thời khi tiếp nhận thông tin giá thị trường hiện hành. |
| **Limit Order** | Lệnh mua hoặc bán chỉ thực hiện khi giá thị trường đạt đến mức giá giới hạn chỉ định (`Limit Price`) hoặc tốt hơn. | Lệnh được đưa vào hàng đợi chờ duyệt (`PENDING`); chỉ khớp khi thỏa mãn điều kiện so sánh giá. |
| **Position (Vị thế)** | Trạng thái nắm giữ một khối lượng cổ phiếu cụ thể kèm giá vốn bình quân gia quyền. | Quản lý theo từng mã: `Số lượng` (`Quantity`) và `Giá vốn trung bình` (`Average Entry Price`). |
| **Giá vốn bình quân (Average Price)** | Chi phí trung bình để sở hữu một cổ phiếu sau nhiều lần mua tích lũy. | $\text{AvgPrice}_{mới} = \frac{(\text{Qty}_{cũ} \times \text{AvgPrice}_{cũ}) + (\text{Qty}_{thêm} \times \text{Price}_{mua})}{\text{Qty}_{mới}}$ |
| **Unrealized P&L** | Lãi hoặc lỗ trên sổ sách của các vị thế đang mở, biến động liên tục theo giá thị trường. | $\text{Unrealized PnL} = (\text{CurrentPrice} - \text{AvgPrice}) \times \text{Quantity}$ |
| **Realized P&L** | Lãi hoặc lỗ thực tế đã chốt khi đóng vị thế (bán cổ phiếu). | $\text{Realized PnL} = (\text{SellPrice} - \text{AvgPrice}) \times \text{SoldQuantity}$ |
| **Win Rate (Tỷ lệ thắng)** | Tỷ lệ phần trăm các giao dịch đem lại lợi nhuận dương trên tổng số giao dịch đã chốt. | $\text{Win Rate} = \frac{\text{Số lệnh có Realized PnL} > 0}{\text{Tổng số lệnh đã đóng}} \times 100\%$ |
| **Max Drawdown (MDD)** | Mức độ sụt giảm phần trăm lớn nhất của giá trị tài khoản tính từ đỉnh cao nhất tới đáy thấp nhất. | $\text{Drawdown}_t = \frac{\text{Peak}_t - \text{Equity}_t}{\text{Peak}_t} \times 100\%$ (lấy giá trị cực đại trong lịch sử). |

---

## 3. KIẾN TRÚC HỆ THỐNG VÀ LUỒNG DỮ LIỆU

### 3.1. Phân tầng kiến trúc (Layered Architecture)
Hệ thống được tổ chức thành 4 tầng độc lập theo nguyên lý phân tách trách nhiệm (Separation of Concerns):

```
+---------------------------------------------------------------------------------+
|                          TẦNG GIAO DIỆN (UI LAYER - JavaFX)                     |
|           MainView.fxml  |  OrderDialog.fxml  |  AnalyticsView.fxml             |
|                  Controllers: MainController, AnalyticsController...            |
+---------------------------------------------------------------------------------+
                                         │
                                         ▼ (Data Binding & Action Events)
+---------------------------------------------------------------------------------+
|                       TẦNG DỊCH VỤ & ĐỘNG CƠ (ENGINE & SERVICE)                 |
|             TradingEngine.java        |       AnalyticsService.java             |
+---------------------------------------------------------------------------------+
                 │                                               │
                 ▼ (QuoteListener Events)                        ▼ (State Mutation)
+------------------------------------+          +---------------------------------+
|    TẦNG DỮ LIỆU THỊ TRƯỜNG (DATA)  |          |      TẦNG MÔ HÌNH (DOMAIN)      |
|  MarketServiceProviderManager      |          |  Account.java, Position.java    |
|  ├── TwelveDataWebSocket.java      |          |  Order.java (Market, Limit)     |
|  ├── FinnhubWebSocket.java         |          |  Quote.java, Trade.java         |
|  ├── AlphaVantageRestClient.java   |          +---------------------------------+
|  └── MockMarketService.java        |
+------------------------------------+
```

### 3.2. Mô hình đa luồng và chuyển đổi API động (Threading & Dynamic Switching)
Nhằm đảm bảo giao diện luôn mượt mà và việc chuyển đổi API diễn ra trong suốt với hệ thống:
1. **JavaFX Application Thread:** Tiếp nhận sự kiện tương tác từ người dùng (ví dụ: đổi nguồn API từ ComboBox). Khi chuyển đổi nguồn, UI không bị giật lag vì việc ngắt/kết nối được điều phối bất đồng bộ. Mọi thao tác cập nhật giá lên UI đều thông qua `Platform.runLater()`.
2. **Dynamic Provider Switching (Cơ chế chuyển đổi động):** Bộ quản lý `MarketServiceProviderManager` duy trì danh sách đăng ký của các `QuoteListener` (`TradingEngine` và `MainController`). Khi người dùng chọn một API mới từ giao diện:
   - Dừng (`stop()`) nhà cung cấp dữ liệu hiện hành và ngắt kết nối WebSocket tương ứng.
   - Kích hoạt (`start()`) nhà cung cấp dữ liệu mới với API Key tương ứng.
   - Tự động chuyển giao toàn bộ các `QuoteListener` và đăng ký lại danh sách mã chứng khoán (`subscribe(symbols)`).
3. **Background Daemon Thread:** Mỗi nhà cung cấp dữ liệu mạng (Twelve Data / Finnhub) sở hữu luồng lắng nghe WebSocket độc lập, tự động phân tích cú pháp gói tin JSON thành định dạng `Quote` thống nhất.

---

## 4. THIẾT KẾ CHI TIẾT CÁC LỚP VÀ NGUYÊN LÝ OOP

### 4.1. Phân hệ Mô hình dữ liệu (`com.trading.model`)

#### `public abstract class Order`
Lớp trừu tượng định nghĩa các thuộc tính và hành vi chung của lệnh giao dịch.
- **Thuộc tính:**
  - `private final String orderId`: Mã định danh duy nhất của lệnh.
  - `private final String symbol`: Mã cổ phiếu giao dịch.
  - `private final boolean isBuy`: Chiều giao dịch (`true` đại diện cho MUA, `false` đại diện cho BÁN).
  - `private final int quantity`: Khối lượng cổ phiếu đặt mua/bán.
  - `private OrderStatus status`: Trạng thái lệnh (`PENDING`, `FILLED`, `CANCELLED`, `REJECTED`).
  - `private final Instant createdAt`: Thời điểm khởi tạo lệnh.
- **Phương thức:**
  - Getters / Setters chuẩn hóa (bảo toàn tính đóng gói).
  - `public abstract boolean canExecute(double currentPrice)`: Phương thức trừu tượng xác định điều kiện lệnh đủ điều kiện khớp theo thị giá.

#### `public class MarketOrder extends Order`
- Hiện thực phương thức `canExecute(double currentPrice)`:
  - Giá trị trả về: `true` vô điều kiện, phản ánh bản chất khớp ngay của lệnh thị trường.

#### `public class LimitOrder extends Order`
- **Thuộc tính bổ sung:**
  - `private final double limitPrice`: Mức giá giới hạn người dùng chấp thuận.
- **Phương thức:**
  - `public double getLimitPrice()`: Trả về `double`.
  - `public boolean canExecute(double currentPrice)`:
    - Nếu là lệnh Mua (`isBuy == true`): Trả về `currentPrice <= limitPrice`.
    - Nếu là lệnh Bán (`isBuy == false`): Trả về `currentPrice >= limitPrice`.

#### `public class Position`
Đại diện cho vị thế nắm giữ của một mã cổ phiếu cụ thể.
- **Thuộc tính:**
  - `private final String symbol`: Mã chứng khoán.
  - `private int quantity`: Số lượng cổ phiếu hiện hữu.
  - `private double averagePrice`: Giá vốn bình quân trên mỗi cổ phiếu.
- **Phương thức:**
  - `public void buyMore(int additionalQty, double purchasePrice)`: Cập nhật khối lượng và tính lại giá vốn bình quân gia quyền.
  - `public double sell(int sellQty, double currentPrice)`: Giảm số lượng nắm giữ và trả về số tiền lợi nhuận thực tế thu được (`Realized P&L`).
  - `public double getUnrealizedPnL(double currentPrice)`: Tính toán lãi/lỗ trên sổ sách dựa trên thị giá hiện tại.
  - `public double getMarketValue(double currentPrice)`: Trả về giá trị vốn hóa của vị thế (`quantity * currentPrice`).

#### `public class Account`
Quản lý tổng thể tài sản, tiền mặt và danh mục vị thế của người dùng.
- **Thuộc tính:**
  - `private double cashBalance`: Số dư tiền mặt khả dụng.
  - `private final Map<String, Position> positions`: Bảng ánh xạ danh mục vị thế theo mã cổ phiếu.
  - `private final List<Trade> tradeHistory`: Lịch sử các giao dịch đã khớp.
- **Phương thức:**
  - `public boolean deductCash(double amount)`: Khấu trừ tiền mặt khi mua cổ phiếu (trả về `false` nếu không đủ tiền).
  - `public void addCash(double amount)`: Cộng tiền mặt khi bán cổ phiếu.
  - `public double getTotalValue(Map<String, Double> currentPrices)`: Tính tổng giá trị tài sản ròng (`Cash Balance + Tổng Market Value các vị thế`).

#### `public class Quote`
Chứa thông tin giá thị trường tức thời của một mã chứng khoán.
- **Thuộc tính:**
  - `private final String symbol`: Mã cổ phiếu.
  - `private final double price`: Giá giao dịch gần nhất.
  - `private final double changePercent`: Tỷ lệ thay đổi giá so với giá mở cửa.
  - `private final Instant timestamp`: Thời điểm ghi nhận thông tin.

#### `public class Trade`
Bản ghi bất biến ghi nhận một giao dịch đã thực thi thành công.
- **Thuộc tính:** `tradeId`, `orderId`, `symbol`, `isBuy`, `quantity`, `executedPrice`, `realizedPnL`, `timestamp`.

---

### 4.2. Phân hệ Động cơ giao dịch (`com.trading.engine`)

#### `public class TradingEngine`
Trung tâm điều phối quy trình đặt lệnh, lưu trữ hàng đợi lệnh chờ và xử lý khớp lệnh.
- **Thuộc tính:**
  - `private final Account account`: Đối tượng tài khoản người dùng.
  - `private final List<Order> pendingOrders`: Danh sách các lệnh giới hạn đang chờ kích hoạt.
  - `private final MarketService marketService`: Cổng tiếp nhận dữ liệu thị trường.
- **Phương thức:**
  - `public synchronized boolean placeOrder(Order order, double currentPrice)`: Tiếp nhận lệnh từ người dùng. Thực hiện kiểm tra tính hợp lệ về tài chính (đủ tiền/đủ cổ phiếu). Nếu là `MarketOrder`, thực thi khớp tức thời; nếu là `LimitOrder`, lưu trữ vào `pendingOrders`.
  - `public synchronized void onPriceUpdate(Quote quote)`: Tiếp nhận thông tin giá mới từ nhà cung cấp dữ liệu. Duyệt toàn bộ `pendingOrders` thuộc mã cổ phiếu tương ứng, kích hoạt `order.canExecute(quote.getPrice())`. Khi điều kiện thỏa mãn, thực thi lệnh và loại bỏ khỏi hàng đợi.
  - `public synchronized boolean cancelOrder(String orderId)`: Hủy lệnh chờ theo yêu cầu người dùng.
  - `private void execute(Order order, double executionPrice)`: Cập nhật dòng tiền, thay đổi khối lượng vị thế và phát sinh bản ghi `Trade`.

---

### 4.3. Phân hệ Dữ liệu thị trường (`com.trading.market`)

Áp dụng kết hợp **Adapter Pattern** (chuẩn hóa các giao thức API khác nhau về một giao diện thống nhất), **Strategy Pattern** (đóng gói giải thuật kết nối sàn) và **Registry / Manager Pattern** (quản lý chuyển đổi động giữa các nguồn):

#### 1. `public enum MarketProviderType`
Định danh các nhà cung cấp dữ liệu thị trường được hỗ trợ:
- `TWELVE_DATA`: Kết nối Twelve Data qua WebSocket (`wss://ws.twelvedata.com/v1/quotes/price`).
- `FINNHUB`: Kết nối Finnhub qua WebSocket (`wss://ws.finnhub.io?token=...`).
- `ALPHA_VANTAGE`: Kết nối Alpha Vantage qua REST polling / WebSocket.
- `MOCK_ENGINE`: Bộ sinh giá ngẫu nhiên nội tuyến phục vụ kiểm thử và vận hành ngoại tuyến.

#### 2. `public interface MarketService`
Định nghĩa hợp đồng giao tiếp chuẩn cho mọi adapter sàn giao dịch (Nguyên lý Abstraction):
- `void start()`: Khởi động tiến trình kết nối mạng hoặc bộ phát sinh giá.
- `void stop()`: Hủy kết nối an toàn, giải phóng socket và thread pool.
- `void subscribe(String symbol)`: Đăng ký nhận dữ liệu mã chứng khoán chỉ định.
- `void unsubscribe(String symbol)`: Hủy đăng ký mã chứng khoán.
- `Quote getLatestQuote(String symbol)`: Truy xuất giá tức thời gần nhất trong cache bộ nhớ.
- `void addListener(QuoteListener listener)`: Đăng ký observer tiếp nhận sự kiện giá mới.
- `void removeListener(QuoteListener listener)`: Hủy đăng ký observer.
- `MarketProviderType getProviderType()`: Trả về loại nhà cung cấp tương ứng.

#### 3. `public class MarketServiceProviderManager`
Đóng vai trò trung gian điều phối (Facade & Context) quản lý việc kích hoạt và chuyển đổi động nhà cung cấp:
- **Thuộc tính:**
  - `private MarketService activeService`: Nhà cung cấp dữ liệu đang vận hành.
  - `private final Map<MarketProviderType, MarketService> registeredProviders`: Bảng ánh xạ các adapter sàn đã đăng ký.
  - `private final Set<QuoteListener> globalListeners`: Danh sách các observer cần nhận giá (`TradingEngine`, `MainController`).
  - `private final Set<String> subscribedSymbols`: Danh sách các mã cổ phiếu đang được theo dõi trên hệ thống.
- **Phương thức trọng tâm:**
  - `public synchronized void registerProvider(MarketProviderType type, MarketService service)`: Đăng ký một adapter sàn vào danh mục quản lý.
  - `public synchronized void switchProvider(MarketProviderType newType, String apiKey)`:
    1. Gọi `activeService.stop()` để đóng kết nối của nhà cung cấp hiện thời.
    2. Cập nhật `activeService` sang adapter tương ứng với `newType` (cập nhật `apiKey` nếu có).
    3. Chuyển giao toàn bộ `globalListeners` sang cho `activeService` mới.
    4. Gọi `activeService.start()` và tự động tái đăng ký toàn bộ `subscribedSymbols`.
    5. Thông báo sự kiện đổi nguồn thành công sang cho giao diện người dùng hiển thị trạng thái.
  - `public MarketService getActiveService()`: Truy xuất adapter đang hoạt động.
  - `public MarketProviderType getActiveProviderType()`: Lấy định danh nguồn đang hoạt động.

#### 4. Các lớp hiện thực cụ thể (Adapters):
- `public class TwelveDataWebSocket implements MarketService`: Chuyển đổi bản tin WebSocket từ sàn Twelve Data thành đối tượng `Quote`.
- `public class FinnhubWebSocket implements MarketService`: Chuyển đổi bản tin WebSocket từ sàn Finnhub (chuẩn JSON `{"type":"trade","data":[{"s":"AAPL","p":182.5,"v":100}]}`) thành đối tượng `Quote`.
- `public class MockMarketService implements MarketService`: Thuật toán bước ngẫu nhiên (Random Walk) phát giá mô phỏng theo chu kỳ 1 giây, tự động vận hành không cần API Key hay Internet.

---

### 4.4. Phân hệ Phân tích và Báo cáo (`com.trading.analytics`)

#### `public class AnalyticsService`
Cung cấp các phương thức tĩnh phục vụ tính toán các chỉ số tài chính:
- `public static double calculateWinRate(List<Trade> trades)`: Tính tỷ lệ giao dịch có lợi nhuận.
- `public static double calculateTotalPnL(List<Trade> trades)`: Tính tổng lợi nhuận ròng từ lịch sử giao dịch.
- `public static double calculateMaxDrawdown(List<Double> equityHistory)`: Tính toán độ sụt giảm tối đa của tài khoản từ đỉnh vốn lịch sử.

#### `public class CsvExporter`
- `public static void exportTrades(List<Trade> trades, Path destinationPath) throws IOException`: Xuất toàn bộ dữ liệu lịch sử giao dịch ra tập tin định dạng CSV tương thích với các công cụ phân tích bảng tính.

---

## 5. THIẾT KẾ GIAO DIỆN ĐA CỬA SỔ (FXML WINDOWS)

Hệ thống giao diện người dùng được module hóa thành **04 cửa sổ độc lập**:

```
+---------------------------------------------------------------------------------+
|                                 MainView.fxml                                   |
|  [Menu Bar: File | Settings | Analytics]                                        |
|  -----------------------------------------------------------------------------  |
|  [Header]: Tiền mặt: $45,200 | Tổng tài sản: $105,400 | Lãi/Lỗ ròng: +$5,400    |
|  -----------------------------------------------------------------------------  |
|   [BẢNG THEO DÕI GIÁ]   |              [BIỂU ĐỒ GIÁ THỜI GIAN THỰC]             |
|   Mã    Giá     Biến động|                   (JavaFX LineChart)                 |
|   AAPL  182.5   +1.2%   |                                                       |
|   MSFT  415.0   -0.4%   |                                                       |
|   TSLA  175.2   +3.5%   |                                                       |
|   [Nút: Đặt Lệnh]       |                                                       |
|  -----------------------------------------------------------------------------  |
|   [TAB PANE DƯỚI CÙNG]                                                          |
|   [Tab 1: Vị thế nắm giữ] | [Tab 2: Lệnh chờ khớp] | [Tab 3: Lịch sử giao dịch] |
+---------------------------------------------------------------------------------+
```

### 5.1. `MainView.fxml` (Cửa sổ chính - Dashboard)
- **Header Panel:** 
  - Trình bày trực quan ba chỉ số tài chính trọng tâm: Tiền mặt khả dụng (`Cash Balance`), Tổng giá trị tài khoản (`Total Equity`), và Lợi nhuận danh mục tổng hợp.
  - **Bộ chuyển đổi API nhanh (Quick Provider Switcher):** Gồm `ComboBox<MarketProviderType>` cho phép người dùng chọn nguồn dữ liệu (Twelve Data / Finnhub / Mock) ngay trên thanh công cụ và nhãn trạng thái kết nối (`lblActiveProviderStatus`).
- **Menu Bar:** Bổ sung menu *"Nguồn Dữ Liệu"* chứa các `RadioMenuItem` hỗ trợ chuyển đổi API tức thì bằng một thao tác click.
- **Watchlist Panel:** Hiển thị danh mục theo dõi gồm các mã cổ phiếu định sẵn, mức giá tức thời và tỷ lệ biến động trong phiên.
- **Chart Panel:** Tích hợp `LineChart` hiển thị chuỗi biến động giá thời gian thực của chứng khoán đang chọn.
- **Tab Pane:**
  - *Vị thế hiện tại:* Liệt kê mã, khối lượng, giá vốn bình quân, thị giá và lãi/lỗ chưa thực hiện kèm thao tác bán nhanh.
  - *Lệnh đang chờ:* Quản lý các lệnh `LimitOrder` chưa kích hoạt kèm tùy chọn hủy lệnh.
  - *Lịch sử:* Bảng tra cứu các lệnh đã thực thi thành công.

### 5.2. `OrderDialog.fxml` (Cửa sổ Pop-up Đặt Lệnh)
- Mở dưới dạng hộp thoại phụ thuộc (Modal Dialog).
- Cho phép người dùng lựa chọn mã, chiều giao dịch (Mua/Bán), loại lệnh (`MARKET` hoặc `LIMIT`), giá giới hạn (nếu có) và khối lượng.
- Tự động hiển thị giá trị ước tính của lệnh và kiểm tra tính khả dụng của số dư trước khi kích hoạt gửi lệnh.

### 5.3. `AnalyticsView.fxml` (Cửa sổ Báo Cáo Thống Kê)
- Mở từ thanh điều hướng chính.
- Tổng hợp các chỉ số đo lường hiệu suất: Tỷ lệ thắng (`Win Rate`), Tổng lãi/lỗ thực tế, Mức sụt giảm cực đại (`Max Drawdown`).
- Cung cấp biểu đồ đường tăng trưởng vốn (`Equity Curve`) và nút chức năng xuất báo cáo ra định dạng CSV.

### 5.4. `SettingsView.fxml` (Cửa sổ Cấu Hình Đa Nguồn API)
- Quản lý danh mục khóa bảo mật cho từng sàn:
  - `TextField txtTwelveDataApiKey`: Nhập API Key Twelve Data.
  - `TextField txtFinnhubApiKey`: Nhập API Key Finnhub.
  - `TextField txtAlphaVantageApiKey`: Nhập API Key Alpha Vantage.
- Lựa chọn nhà cung cấp mặc định khi khởi động ứng dụng qua nhóm `ToggleGroup`.
- Tùy chọn chuyển đổi sang chế độ nội tuyến: `Dữ liệu mô phỏng (Mock Engine)`.
- Nút *"Áp Dụng & Kết Nối"* (`btnApplyProvider`): Gọi lệnh chuyển đổi API sang `MarketServiceProviderManager` ngay tại thời gian chạy.
- Chức năng thiết lập lại trạng thái tài khoản về mức vốn ban đầu.

---

## 6. ĐẶC TẢ KIỂM THỬ ĐƠN VỊ (UNIT TESTING - JUNIT 5)

Hệ thống thiết lập bộ kiểm thử tự động nhằm xác minh tính chính xác của các thuật toán và nguyên lý lập trình:

### 6.1. Kiểm thử Đa hình trong Khớp lệnh (`OrderPolymorphismTest.java`)
- **Mục tiêu:** Xác minh tính đúng đắn của phương thức `canExecute(double currentPrice)` trên các lớp con kế thừa từ `Order` mà không cần kiểm tra kiểu thủ công (`instanceof`).
- **Kịch bản:**
  - Lệnh `MarketOrder` luôn trả về `true` với các mức giá kiểm thử khác nhau.
  - Lệnh `LimitOrder` chiều Mua chỉ trả về `true` khi thị giá nhỏ hơn hoặc bằng giá giới hạn.
  - Lệnh `LimitOrder` chiều Bán chỉ trả về `true` khi thị giá lớn hơn hoặc bằng giá giới hạn.

### 6.2. Kiểm thử Tính toán Giá vốn và Lợi nhuận (`PositionCalculationTest.java`)
- **Mục tiêu:** Đảm bảo giải thuật tính giá vốn bình quân gia quyền và lợi nhuận thực tế đạt độ chính xác số học tuyệt đối.
- **Kịch bản:**
  - Thực hiện nhiều giao dịch mua tích lũy với khối lượng và mức giá khác nhau; đối chiếu giá vốn bình quân mới với kết quả công thức lý thuyết.
  - Thực hiện bán một phần khối lượng nắm giữ; kiểm tra giá trị `Realized P&L` thu được và xác nhận giá vốn của số cổ phiếu còn lại không bị thay đổi.

### 6.3. Kiểm thử Động cơ Khớp lệnh (`TradingEngineTest.java`)
- **Mục tiêu:** Kiểm tra quy tắc nghiệp vụ về dòng tiền và quy trình vòng đời của lệnh giao dịch.
- **Kịch bản:**
  - Tiếp nhận lệnh Mua vượt quá số dư tiền mặt khả dụng $\rightarrow$ Xác nhận hệ thống từ chối thực thi và giữ nguyên số dư.
  - Khởi tạo lệnh `LimitOrder` $\rightarrow$ Xác nhận lệnh được chuyển vào trạng thái `PENDING`. Mô phỏng cập nhật dữ liệu giá thỏa mãn $\rightarrow$ Xác nhận lệnh tự động chuyển sang `FILLED` và cập nhật danh mục vị thế.

### 6.4. Kiểm thử Chuyển đổi Nguồn Dữ liệu Động (`MarketProviderSwitchingTest.java`)
- **Mục tiêu:** Đảm bảo quá trình chuyển đổi giữa các API sàn (Twelve Data, Finnhub, Mock) diễn ra thông suốt tại thời gian chạy.
- **Kịch bản:**
  - Đăng ký observer vào `MarketServiceProviderManager`.
  - Khởi động với `MockMarketService` $\rightarrow$ Xác nhận observer nhận được giá từ Mock.
  - Thực hiện gọi `switchProvider(MarketProviderType.FINNHUB, "test-key")` $\rightarrow$ Xác nhận provider cũ bị dừng (`stop()`), provider mới được kích hoạt (`start()`) và toàn bộ observer tiếp tục nhận luồng giá mới mà không bị ngắt quãng.

### 6.5. Kiểm thử Hàm Thống Kê Tài Chính (`AnalyticsServiceTest.java`)
- **Mục tiêu:** Kiểm tra tính chính xác của thuật toán đo lường hiệu suất danh mục.
- **Kịch bản:**
  - Cung cấp tập dữ liệu mẫu gồm các giao dịch có lãi và lỗ $\rightarrow$ Kiểm chứng kết quả tỷ lệ `Win Rate`.
  - Cung cấp chuỗi giá trị tài sản có đỉnh và đáy $\rightarrow$ Kiểm chứng kết quả phần trăm sụt giảm tối đa `Max Drawdown`.

---

## 7. PHÂN CÔNG CÔNG VIỆC VÀ CƠ CHẾ TRIỂN KHAI SONG SONG

Để tối ưu hóa hiệu suất làm việc của nhóm 04 thành viên và loại trừ sự phụ thuộc tuần tự, ranh giới trách nhiệm và hợp đồng giao tiếp (Interface Contracts) được phân định cụ thể:

| Phân vai | Phân hệ phụ trách | Tệp mã nguồn & Giao diện đảm nhiệm | Bộ kiểm thử đơn vị tương ứng |
|---|---|---|---|
| **Kỹ sư Động cơ & Nghiệp vụ (Core & Engine)** | `model/`<br>`engine/` | `Order.java`, `MarketOrder.java`, `LimitOrder.java`<br>`Position.java`, `Account.java`, `Trade.java`<br>`TradingEngine.java` | `OrderPolymorphismTest.java`<br>`PositionCalculationTest.java`<br>`TradingEngineTest.java` |
| **Kỹ sư Tích hợp Dữ liệu (Market Data)** | `market/` | `MarketService.java`, `MarketProviderType.java`<br>`MarketServiceProviderManager.java`<br>`TwelveDataWebSocket.java`, `FinnhubWebSocket.java`<br>`MockMarketService.java`<br>`SettingsView.fxml`, `SettingsController.java` | `MockMarketServiceTest.java`<br>`TwelveDataJsonParserTest.java`<br>`MarketProviderSwitchingTest.java` |
| **Kỹ sư Phân tích & Báo cáo (Analytics)** | `analytics/` | `AnalyticsService.java`<br>`CsvExporter.java`<br>`AnalyticsView.fxml`, `AnalyticsController.java` | `AnalyticsServiceTest.java`<br>`CsvExporterTest.java` |
| **Kỹ sư Giao diện (UI/UX Frontend)** | `ui/` | `MainApp.java`<br>`MainView.fxml` (kèm bộ chuyển API), `MainController.java`<br>`OrderDialog.fxml`, `OrderDialogController.java`<br>`dark-theme.css` | `OrderDialogValidationTest.java` |

### Cơ chế độc lập hóa tiến độ:
- Phân hệ **Core & Engine** kiểm thử toàn diện thông qua JUnit sử dụng các đối tượng dữ liệu độc lập mà không cần khởi chạy giao diện người dùng.
- Phân hệ **Market Data** cung cấp lớp `MockMarketService` ngay từ giai đoạn đầu, cho phép toàn bộ nhóm vận hành kiểm thử tích hợp mà không phụ thuộc vào hạ tầng mạng hay hạn mức API.
- Phân hệ **Analytics** phát triển và kiểm thử thuật toán dựa trên tập dữ liệu giao dịch mẫu (`Mock Trades`).
- Phân hệ **UI/UX** thiết kế bố cục giao diện và liên kết dữ liệu dựa trên các giao diện trừu tượng (`MarketService`, `TradingEngine`) với dữ liệu giả lập trước khi tích hợp chính thức.

---

## 8. CÔNG CỤ, FRAMEWORK VÀ THƯ VIỆN KỸ THUẬT

### 8.1. Môi trường phát triển cốt lõi
- **Ngôn ngữ:** Java 17 hoặc Java 21 (LTS).
- **Công cụ xây dựng (Build Tool):** Gradle (hoặc Maven).
- **Giao diện người dùng:** OpenJFX (JavaFX 21.0.2).
- **Môi trường phát triển (IDE):** IntelliJ IDEA Community / Ultimate, Eclipse hoặc VS Code (kèm Extension Pack for Java).
- **Công cụ thiết kế FXML:** Gluon SceneBuilder (phiên bản 21+).

### 8.2. Danh mục thư viện phụ thuộc tối giản (Minimal Gradle Dependencies)
Tệp `build.gradle` được tinh giản triệt để, loại bỏ các thư viện dư thừa (sử dụng tối đa các thư viện sẵn có của JDK 17/21) và tích hợp **Google Checkstyle**:

```groovy
plugins {
    id 'application'
    id 'checkstyle'                                      // Plugin kiểm tra chuẩn code Google
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'com.trading'
version = '1.0.0'

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.2"
    modules = [ 'javafx.controls', 'javafx.fxml', 'javafx.graphics' ]
}

application {
    mainClass = 'com.trading.MainApp'
}

// Cấu hình Google Checkstyle
checkstyle {
    toolVersion = '10.15.0'
    configFile = file("${rootDir}/config/checkstyle/google_checks.xml")
    maxWarnings = 0                                      // Bắt buộc 0 cảnh báo khi biên dịch
}

dependencies {
    // 1. Phân hệ Thị Trường & Mạng (Thành viên 2)
    implementation 'org.java-websocket:Java-WebSocket:1.5.6'             // Kết nối WebSocket thời gian thực
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0' // Phân tích cú pháp JSON sàn sang POJO
    // Ghi chú: REST API sử dụng trực tiếp java.net.http.HttpClient có sẵn từ JDK 11+, không cần thêm OkHttp.

    // 2. Phân hệ Thống Kê & Giao Diện (Thành viên 3 & 4)
    // Ghi chú: Xuất CSV sử dụng trực tiếp java.io.BufferedWriter / java.nio.file.Files của JDK (chuẩn RFC 4180).
    // Ghi chú: Biểu tượng UI sử dụng trực tiếp ký tự đồ họa Unicode (▲, ▼, ⚙, 📊) và CSS JavaFX.

    // 3. Khung Kiểm Thử Tự Động (Tất cả thành viên)
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'                // Khung kiểm thử tự động JUnit 5
    testImplementation 'org.mockito:mockito-core:5.11.0'                // Tạo đối tượng Mock phụ thuộc
    testImplementation 'org.mockito:mockito-junit-jupiter:5.11.0'
    // Ghi chú: Sử dụng phương thức assertEquals(expected, actual, delta) tích hợp sẵn của JUnit 5.
}

test {
    useJUnitPlatform()
}
```

### 8.3. Bảng phân bổ công cụ tinh gọn theo từng phân vai

| Phân vai | Thư viện bên ngoài cần thiết | Thư viện JDK sẵn có tận dụng | Công cụ hỗ trợ phát triển & kiểm tra code |
|---|---|---|---|
| **Kỹ sư Động cơ (Core & Engine)** | `JUnit 5`, `Mockito` | `java.util.concurrent.*`<br>`java.time.Instant`, `UUID` | **Checkstyle:** `./gradlew checkstyleMain`<br>**JaCoCo:** Đo độ phủ test. |
| **Kỹ sư Thị trường (Market Data)** | `Java-WebSocket 1.5.6`<br>`Jackson Databind 2.17.0` | `java.net.http.HttpClient`<br>`ScheduledExecutorService` | **Postman / wscat:** Kiểm tra kết nối sàn.<br>**Checkstyle:** Kiểm tra quy chuẩn import và đặt tên. |
| **Kỹ sư Thống kê (Analytics)** | `JUnit 5` | `java.nio.file.*`<br>`java.io.BufferedWriter` | **Excel / Google Sheets:** Kiểm tra file CSV.<br>**Checkstyle:** Kiểm tra quy chuẩn class tiện ích. |
| **Kỹ sư Giao diện (UI Frontend)** | `JavaFX 21 (Controls, FXML)` | `Platform.runLater()`<br>Unicode & JavaFX CSS | **SceneBuilder 21:** Thiết kế FXML.<br>**Checkstyle:** Kiểm tra quy chuẩn khai báo biến `@FXML`. |

---

### 8.4. Chuẩn hóa mã nguồn toàn dự án với Google Checkstyle

Toàn bộ 4 thành viên bắt buộc tuân thủ quy chuẩn **Google Java Style Guide** được kiểm soát tự động thông qua task Gradle:
1. **Quy tắc thụt lề (Indentation):** Sử dụng chính xác **2 khoảng trắng (spaces)** cho mỗi mức thụt lề; tuyệt đối không dùng ký tự Tab.
2. **Quy tắc nhập gói (No Wildcard Imports):** Nghiêm cấm dùng `import package.*`. Mọi lớp sử dụng đều phải được khai báo tường minh từng dòng.
3. **Quy ước đặt tên (Naming Conventions):**
   - Tên lớp / Interface: `UpperCamelCase` (ví dụ: `MarketOrder`, `TradingEngine`).
   - Tên phương thức / biến: `lowerCamelCase` (ví dụ: `placeOrder`, `cashBalance`).
   - Tên hằng số: `UPPER_SNAKE_CASE` (ví dụ: `DEFAULT_INITIAL_CASH`).
4. **Khối lệnh và dấu ngoặc (Braces):** Áp dụng K&R style. Mọi câu lệnh điều kiện (`if`, `else`, `for`, `while`) **bắt buộc** phải bao bọc bởi cặp dấu ngoặc nhọn `{ }`, ngay cả khi chỉ có một dòng lệnh thực thi.
5. **Độ dài dòng (Line Length):** Giới hạn tối đa **100 ký tự** trên một dòng mã nguồn.
6. **Lệnh kiểm tra trước khi tạo Pull Request / Commit Git:**
   ```bash
   ./gradlew checkstyleMain checkstyleTest
   ```
   Nếu có bất kỳ vi phạm nào, tiến trình build sẽ dừng lại và báo vị trí lỗi cụ thể (dòng, cột) để lập trình viên sửa ngay.


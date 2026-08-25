# Kịch bản demo BookAura dành cho mentor

Đây là một buổi walkthrough ngắn, có thể lặp lại cho demo local. Kịch bản tách riêng
các luồng thư viện ổn định khỏi những tính năng phụ thuộc credential bên ngoài.
Giao diện mặc định là **Tiếng Việt (VN)**; dùng bộ chuyển **VN / EN** ở thanh điều hướng
để đổi sang tiếng Anh. Lựa chọn ngôn ngữ được lưu trên trình duyệt.

## 0. Khởi động demo

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# terminal thứ hai
cd frontend && npm run dev
```

Mở `http://localhost:5173`. Profile local tạo sẵn tài khoản `admin / admin`; Mailpit
ở `http://localhost:8025`. Nếu IDE đang biên dịch đè vào `backend/target`, hãy dùng JAR
đã đóng gói sạch như hướng dẫn trong README.

## 1. Xác thực và xác minh email

1. Mở **Đăng ký**, tạo một thành viên với số điện thoại tùy chọn rồi gửi biểu mẫu.
2. Mở Mailpit, sao chép liên kết xác minh và xác minh tài khoản. Cho thấy đăng nhập
   trước khi xác minh trả về `EMAIL_NOT_VERIFIED`.
3. Đăng nhập bằng email/mật khẩu. Làm mới trang để minh họa silent refresh: access token
   chỉ nằm trong bộ nhớ và refresh token là cookie HttpOnly.
4. Tùy chọn: dùng nút Google/Facebook khi credential của provider tương ứng đã được cấu hình.
   Cả hai provider đều trả về mã exchange dùng một lần ngắn hạn, không phải JWT trong URL.
5. Luồng điện thoại local tùy chọn: chọn đăng nhập bằng điện thoại, yêu cầu mã, sau đó dùng
   trang **Hộp thư SMS** local chỉ dành cho ADMIN để đọc mã trong bộ nhớ. Mã không bao giờ
   được ghi vào log hoặc database.
6. Trong **Cài đặt tài khoản**, yêu cầu đổi email, đọc mã từ Mailpit, xác nhận và cho thấy
   email hiện tại chỉ thay đổi sau khi xác minh thành công.

## 2. Danh mục công khai và chi tiết sách

1. Mở **Danh mục** khi đang đăng xuất. Tìm theo tên sách/ISBN và kết hợp bộ lọc thể loại,
   năm xuất bản, tình trạng còn sách; chỉ ra rằng bộ lọc và số trang nằm trong URL.
2. Mở chi tiết một cuốn sách. Cho thấy tác giả, thể loại, tồn kho và metadata. Chi tiết
   công khai vẫn chỉ đọc; chỉ thành viên đã xác thực mới có thể mượn.

## 3. Mượn, trả và lịch sử

1. Đăng nhập bằng thành viên đã xác minh và mở **Sách đang mượn**.
2. Mượn một cuốn còn sẵn. Cho thấy lượt mượn đang hoạt động và hạn trả.
3. Trả sách qua hộp thoại xác nhận. Mở **Lịch sử** để cho thấy lượt mượn đã trả được lưu
   bất biến.
4. Giải thích rằng cuộc đua ở bản cuối được quyết định bằng một lần cập nhật tồn kho
   có điều kiện nguyên tử; database cũng ngăn cùng một thành viên mượn trùng một cuốn.

## 4. Không gian quản trị

Đăng nhập bằng `admin / admin` và trình bày:

- **Sách:** tạo/chỉnh sửa/lưu trữ một tựa sách; việc thay đổi tổng số bản vẫn giữ các bản
  đang được mượn.
- **CSV:** nhập `docs/aura-demo-books.csv`; cho thấy lỗi theo dòng và kết quả nguyên tử,
  hoặc toàn bộ thành công hoặc toàn bộ rollback. Header CSV bảy cột cũ vẫn được chấp nhận.
- **Thành viên:** tìm kiếm với các điều kiện tên, email/điện thoại, ngày sinh, tên sách
  đang mượn, trạng thái, vai trò và xác minh; tạo/chỉnh sửa/vô hiệu hóa mà không xóa lịch sử.
- **Lượt mượn:** xem các lượt đang hoạt động/lịch sử và trả thay với quyền ADMIN khi cần.
- **Bảo trì:** bật chế độ bảo trì. API nghiệp vụ thông thường trả về 503 có trace; health
  và endpoint điều khiển được bảo vệ vẫn hoạt động. Tắt lại sau khi demo.

## 5. Shelf Aura và Arcane Opus

1. Mở **Shelf Aura** tại `/aura`, chọn tâm trạng, chủ đề tùy chọn, thời gian đọc và độ sâu.
2. Cho thấy tối đa sáu thẻ được xếp hạng, phân rã điểm, lý do dễ đọc, tag phù hợp và tình
   trạng còn sách. Chọn **Danh sách** để minh họa luồng nguồn dễ tiếp cận.
3. Trên trình duyệt desktop có WebGL2, mở kệ Arcane Opus: bìa sách tạo thủ tục nhưng vẫn
   đọc được, xem trước khi hover/focus, chọn bằng bàn phím, panel đọc cố định và nhấp tới
   chi tiết sách.
4. Thu nhỏ xuống mobile để cho thấy carousel đơn giản. Bật reduced motion trong hệ điều
   hành/DevTools (hoặc dùng trình duyệt không có WebGL2) để cho thấy hệ thống tự động quay
   về danh sách 2D.

## 6. Công tắc P2 tùy chọn và giới hạn trung thực

### Engine ngữ nghĩa local thử nghiệm

Khởi động lại backend bằng:

```bash
AURA_RECOMMENDATION_ENGINE=embedding ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Engine này là bộ chấm điểm vector có dấu 128 chiều, xác định và chạy hoàn toàn offline.
Nó không gọi HTTP tới AI, trả tối đa sáu sách đang hoạt động, gắn nhãn semantic affinity
và giữ phân rã điểm luật ở trạng thái trung tính. Xóa biến môi trường hoặc đặt
`AURA_RECOMMENDATION_ENGINE=rule` để quay lại engine luật dễ giải thích (mặc định).

### Brevo SMS

Brevo SMS tính phí trả trước và dự án không giả định có credit miễn phí. Chỉ khi có API key,
sender đã được phê duyệt và credit được cung cấp riêng bên ngoài repository mới cấu hình:

```bash
SMS_PROVIDER=brevo
BREVO_SMS_API_KEY=<được cung cấp bên ngoài git>
BREVO_SMS_SENDER=BookAura
# tùy chọn: BREVO_SMS_BASE_URL=https://api.brevo.com/v3
```

Profile không phải local/test khi đó dùng `POST /v3/transactionalSMS/sms`; local/test luôn
dùng fake sender. Cấu hình thiếu hoặc không được hỗ trợ vẫn trả về `SMS_DELIVERY_UNAVAILABLE`.
Không thực hiện request live cho buổi demo này nếu chưa có credential/credit được cho phép,
và tuyệt đối không dán key hoặc OTP vào git, issue hay ảnh chụp màn hình.

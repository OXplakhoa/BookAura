# Hỏi đáp BookAura dành cho mentor

## Vì sao dùng JWT kết hợp refresh cookie?

Access JWT có thời hạn 15 phút, dùng cho việc phân quyền API và chỉ nằm trong bộ nhớ module
của frontend. Refresh token dạng opaque có thời hạn bảy ngày, dùng cookie HttpOnly,
SameSite=Lax, Secure ngoài local, được xoay vòng sau mỗi lần refresh và chỉ lưu dưới dạng
SHA-256. Nếu refresh token bị dùng lại, toàn bộ refresh family sẽ bị thu hồi. Logout cũng
blacklist `jti` của access token, vì vậy tính stateless của JWT không làm suy yếu logout.
Frontend dùng một refresh promise dùng chung để nhiều request hết hạn không xoay token
song song.

## Xử lý đồng thời khi mượn sách như thế nào?

Chỉ dùng một chiến lược: cập nhật có điều kiện trong PostgreSQL và kiểm tra số dòng bị ảnh
hưởng. Borrow chạy `available_quantity = available_quantity - 1 WHERE available_quantity > 0`;
return chỉ chuyển lượt mượn đang hoạt động và tăng tồn kho khi lần cập nhật có điều kiện thắng.
Partial unique index trên `(member_profile_id, book_id)` đang hoạt động đóng cuộc đua tạo lượt
mượn trùng. Không kết hợp pessimistic lock và optimistic lock.

## Bằng chứng cho transaction rollback là gì?

Test loan cố tình ném exception sau các mutation tồn kho/lượt mượn rồi kiểm tra cả hai mutation
đều rollback. Test CSV cố tình lỗi sau `saveAll+flush` và kiểm tra books/authors/categories
đều rollback. Test delivery phone failure kiểm tra row OTP mới được rollback khi provider lỗi;
điều này ngăn một mã chưa gửi vẫn có thể sử dụng.

## CSV import an toàn như thế nào?

Apache Commons CSV dùng parser streaming, kiểm tra header legacy hoặc extended chính xác,
giới hạn file chặt chẽ dưới 5 MiB và tối đa 10.000 dòng, chỉ giữ các model đã validate trong
bộ nhớ có giới hạn, kiểm tra ISBN trùng bằng `Set`, bulk-resolve quan hệ ISBN/tác giả/thể loại
và commit trong một transaction. Lỗi theo dòng được trả về mà không lộ entity hay ghi một phần
dữ liệu.

## Recommendation có thật sự là AI không?

Engine mặc định `RuleBasedRecommendationEngine` có tính xác định và dễ giải thích: mỗi điểm
đều ánh xạ vào một quy tắc mood/theme/time/intensity. `AURA_RECOMMENDATION_ENGINE=embedding`
chọn implementation pseudo-embedding local thử nghiệm. Engine chuẩn hóa mood/theme/text sách
và metadata số trang thô thành vector có dấu 128 chiều, xác định và chạy offline, sau đó xếp
hạng bằng cosine similarity. Nó không có provider AI bên ngoài, secret hay network call; tối đa
sáu sách đang hoạt động.

Lý do của engine semantic ghi `Semantic affinity: N%`; chỉ các token/tag giao nhau thực sự mới
được trả về trong `matchedTags`, còn các trường rule breakdown giữ trung tính thay vì giả vờ
semantic match đã kiếm được điểm `+4 theme`.

## Điều gì xảy ra khi WebGL không khả dụng?

Các thẻ gợi ý 2D hiện có là source of truth và là list view rõ ràng. Three.js và React Three
Fiber nằm sau lazy route boundary nên không làm phình bundle chính. Kiểm tra WebGL2,
`prefers-reduced-motion`, lỗi lazy chunk hoặc lỗi render đều tự động chọn fallback 2D. Scene
3D dùng control/panel DOM bình thường để hiển thị giải thích dễ đọc và link trực tiếp tới chi
 tiết sách; 3D chỉ là enhancement, không phải con đường tương tác duy nhất.

## Brevo SMS có live và miễn phí không?

Không. Mặc định không claim live delivery. Brevo SMS tính phí trả trước. Profile local/test
luôn inject `FakeSmsSender`, kể cả khi có biến Brevo. Ngoài local/test, chỉ cấu hình
`SMS_PROVIDER=brevo` cùng `BREVO_SMS_API_KEY` không trống mới chọn `BrevoSmsSender`; thiếu
credential hoặc provider không được hỗ trợ sẽ chọn `UnavailableSmsSender` và trả về
`SMS_DELIVERY_UNAVAILABLE`.

Lỗi 4xx/5xx, timeout và network của Brevo đều ánh xạ về cùng một lỗi an toàn. Số điện thoại
thô, OTP, API key và response body của provider không được log hoặc trả về. Smoke test thật
cần API key, sender đã được duyệt và credit trả trước được cung cấp riêng.

## Vì sao transaction boundary của SMS quan trọng?

Tạo token và gửi tới provider diễn ra trong transaction của `PhoneOtpService.request`. Khi gửi
thành công, hash/expiry/attempt state chỉ commit sau khi service kết thúc. Khi provider lỗi,
exception thoát ra và row mới rollback. Cooldown được bắt và trả lời enumeration-safe mà không
đánh dấu outer transaction là rollback-only.

## Trong repository có những secret nào?

Không có secret nào. `.env.example` chỉ chứa placeholder rỗng và các giá trị mặc định không
nhạy cảm. JWT secret, OAuth credential, Brevo key, database password và OTP phải nằm trong
process environment hoặc file local không được track. Fake outbox chỉ là công cụ demo local,
được bảo vệ bởi ADMIN; OTP thô không bao giờ được ghi vào log.

## Chuyển đổi ngôn ngữ hoạt động thế nào?

Giao diện khởi động mặc định bằng **Tiếng Việt (VN)**. Bộ chuyển ngôn ngữ **VN / EN** nằm
trên public header, màn hình xác thực và dashboard; lựa chọn được lưu trong browser để lần
mở sau giữ nguyên. Nó chỉ dịch copy của frontend, không thay đổi dữ liệu sách, tên tác giả,
tag hoặc mã lỗi nghiệp vụ do backend trả về.

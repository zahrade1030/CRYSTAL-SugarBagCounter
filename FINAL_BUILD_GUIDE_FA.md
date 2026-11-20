# راهنمای نسخه نهایی CRYSTAL.BIN

## اصلاحات اعمال‌شده در نسخه 1.1.0

- یکپارچه‌سازی زبان فارسی/انگلیسی بین صفحه فعال‌سازی، صفحه اصلی، تنظیمات و برچسب‌های Overlay.
- ذخیره تنظیمات خطوط و ROI به‌صورت نسبی تا با تغییر اندازه فریم دوربین جابه‌جایی کمتری داشته باشند.
- کاهش اندازه فریم پردازش‌شده دوربین به حداکثر 1280 پیکسل برای کاهش فشار حافظه و CPU.
- اصلاح پیش‌پردازش YOLO با حفظ نسبت تصویر و Letterbox.
- اصلاح decoding خروجی YOLO برای مدل تک‌کلاسه فعلی و پشتیبانی از خروجی‌های دارای Objectness.
- شمارش عبور از هر دو مرز محدوده، در هر دو جهت، و یک‌بار برای هر Track.
- اصلاح پیش‌بینی Track در فریم‌های ازدست‌رفته.
- جلوگیری از ثبت تکراری شمارش یک Session در تاریخچه.
- ثبت Session ناتمام هنگام بسته‌شدن Activity، در صورت امکان.
- نمایش Session جاری در تاریخچه، آمار و خروجی Excel، حتی قبل از توقف شمارش.
- اصلاح گزارش Excel: عنوان، سربرگ، جدول دارای Border و Style، ردیف مجموع، Freeze Header، فیلتر و راست‌به‌چپ فارسی.
- نمایش اطلاعات لایسنس در صفحه About و امکان تمدید/جایگزینی لایسنس از داخل برنامه.
- اصلاح اندازه‌های صفحه فعال‌سازی با واحد dp و بررسی لایسنس در Thread پس‌زمینه.
- هماهنگ‌سازی Theme مشکی/طلایی.
- حذف License Generator قدیمی VEA که با قالب CRYSTAL.BIN سازگار نبود.
- افزایش نسخه برنامه به 1.1.0.
- به‌روزرسانی GitHub Actions برای ساخت Debug و Release.

## ساخت APK تستی نصب‌پذیر

### GitHub Actions

1. فایل‌های این پروژه را در Repository خود قرار بده و Push کن.
2. در GitHub وارد تب **Actions** شو.
3. Workflow با نام **Build CRYSTAL.BIN APKs** را انتخاب کن.
4. روی **Run workflow** بزن و Branch را انتخاب کن.
5. بعد از موفقیت Build، Artifact با نام `CRYSTAL-BIN-APKs-v1.1.0` را دانلود کن.
6. داخل Artifact، فایل `app-debug.apk` برای نصب و تست روی گوشی موجود است.

## Release

Workflow، فایل Release را نیز می‌سازد. اگر چهار Secret زیر در GitHub تنظیم نشده باشند، `app-release-unsigned.apk` بدون امضا تولید می‌شود:

- `CRYSTAL_KEYSTORE_BASE64`
- `CRYSTAL_KEYSTORE_PASSWORD`
- `CRYSTAL_KEY_ALIAS`
- `CRYSTAL_KEY_PASSWORD`

برای انتشار تجاری، Keystore اختصاصی بساز و آن را به‌صورت Secret در GitHub قرار بده؛ فایل یا رمز Keystore را داخل Repository نگذار. از Keystore نسخه پشتیبان امن نگه‌دار؛ گم‌شدن آن امکان به‌روزرسانی امضاشده را از بین می‌برد.

ساخت Keystore روی ویندوز (در یک محل امن خارج از Repository):

```powershell
keytool -genkeypair -v -keystore crystal-bin-release.jks -alias crystalbin -keyalg RSA -keysize 2048 -validity 10000
```

مقدار Base64 فایل Keystore را در PowerShell بگیر:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("crystal-bin-release.jks"))
```

سپس در Repository > Settings > Secrets and variables > Actions، چهار Secret بالا را بساز. Keystore و رمزها را به من یا داخل چت ارسال نکن.

## تست الزامی روی گوشی/کارخانه

- فعال‌سازی و تمدید لایسنس.
- مجوز دوربین و شروع Preview.
- تنظیم خطوط و ROI.
- شمارش عبور کیسه در جهت واقعی نوار نقاله.
- بررسی شمارش تکراری و کیسه‌هایی که موقتاً پشت هم/پشت مانع قرار می‌گیرند.
- توقف، شروع Session جدید و Reset.
- تاریخچه و آمار روزانه/هفتگی/ماهانه.
- خروجی Excel در هر دو زبان و بازکردن فایل با Excel یا LibreOffice.
- تنظیم تصویر راهنما، شفافیت و Zoom.
- اجرای طولانی‌مدت و بررسی گرما، سرعت و مصرف حافظه.

## محدودیت بررسی فعلی

در محیط فعلی Android SDK و Gradle نصب نیستند و دسترسی شبکه برای دریافت آن‌ها وجود ندارد؛ بنابراین APK در این محیط ساخته یا روی گوشی اجرا نشده است. تست‌های مستقل Tracker و اعتبار XML گزارش Excel انجام شده‌اند، اما Build واقعی و دقت مدل باید با GitHub Actions و گوشی/ویدیوی کارخانه تأیید شوند.

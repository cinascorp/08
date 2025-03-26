<?php
// اتصال به دیتابیس یا فایل JSON برای بررسی کدها
$codesFile = 'https://github.com/cinascorp/08/blob/codes.json';  // فرضاً کدها در یک فایل JSON ذخیره می‌شوند
$codes = json_decode(file_get_contents($codesFile), true);

if (isset($_GET['code'])) {
    $code = $_GET['code'];
    if (isset($codes[$code]) && !$codes[$code]['used']) {
        // کد معتبر است و هنوز استفاده نشده است
        // علامت‌گذاری کد به عنوان استفاده‌شده
        $codes[$code]['used'] = true;
        file_put_contents($codesFile, json_encode($codes, JSON_PRETTY_PRINT));
        echo json_encode(['success' => true]);
    } else {
        echo json_encode(['success' => false]);
    }
}
?>

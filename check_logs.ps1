# Скрипт для проверки логов сервера

Write-Host "=== Проверка логов сервера ===" -ForegroundColor Cyan
Write-Host ""

# Проверяем, существует ли файл логов
if (Test-Path "logs/application.log") {
    Write-Host "✓ Файл логов найден: logs/application.log" -ForegroundColor Green
    Write-Host ""
    
    # Показываем последние 50 строк
    Write-Host "Последние 50 строк логов:" -ForegroundColor Yellow
    Write-Host "----------------------------------------"
    Get-Content logs/application.log -Tail 50
    Write-Host "----------------------------------------"
    Write-Host ""
    
    # Ищем ошибки
    Write-Host "Поиск ошибок (ERROR):" -ForegroundColor Yellow
    $errors = Select-String -Path logs/application.log -Pattern "ERROR" | Select-Object -Last 10
    if ($errors) {
        $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    } else {
        Write-Host "Ошибок не найдено" -ForegroundColor Green
    }
    Write-Host ""
    
    # Ищем исключения
    Write-Host "Поиск исключений (Exception):" -ForegroundColor Yellow
    $exceptions = Select-String -Path logs/application.log -Pattern "Exception" | Select-Object -Last 10
    if ($exceptions) {
        $exceptions | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    } else {
        Write-Host "Исключений не найдено" -ForegroundColor Green
    }
    Write-Host ""
    
    # Размер файла
    $fileSize = (Get-Item logs/application.log).Length / 1KB
    Write-Host "Размер файла логов: $([math]::Round($fileSize, 2)) KB" -ForegroundColor Cyan
    
} else {
    Write-Host "⚠ Файл логов не найден!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Возможные причины:" -ForegroundColor Yellow
    Write-Host "1. Приложение еще не запускалось после настройки логирования"
    Write-Host "2. Приложение запущено, но логи еще не записались"
    Write-Host ""
    Write-Host "Что делать:" -ForegroundColor Cyan
    Write-Host "1. Запустите приложение (mvn spring-boot:run или через IDE)"
    Write-Host "2. Выполните несколько действий в приложении"
    Write-Host "3. Запустите этот скрипт снова"
    Write-Host ""
    Write-Host "Логи также выводятся в консоль IDE или терминал, где запущено приложение!" -ForegroundColor Green
}

Write-Host ""
Write-Host "Для мониторинга логов в реальном времени используйте:" -ForegroundColor Cyan
Write-Host "  Get-Content logs/application.log -Wait" -ForegroundColor White


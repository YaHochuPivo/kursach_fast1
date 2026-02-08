package com.example.project2;

import com.example.project2.model.AppUser;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@SpringBootApplication
@EnableAsync
public class Project2Application {

    public static void main(String[] args) {
        SpringApplication.run(Project2Application.class, args);
    }

    @Bean
    CommandLineRunner seedData(AppUserRepository userRepo, PropertyRepository propertyRepo, PasswordEncoder encoder, JdbcTemplate jdbcTemplate) {
        return args -> {
            // Добавляем колонку promoted если её нет
            try {
                jdbcTemplate.execute("ALTER TABLE properties ADD COLUMN IF NOT EXISTS promoted BOOLEAN NOT NULL DEFAULT false");
                System.out.println("✓ Колонка promoted добавлена или уже существует");
            } catch (Exception e) {
                System.out.println("⚠ Колонка promoted уже существует или ошибка: " + e.getMessage());
            }
            
            // Делаем колонку deal_id в таблице chats nullable (для чатов по объявлениям без сделок)
            try {
                jdbcTemplate.execute("ALTER TABLE chats ALTER COLUMN deal_id DROP NOT NULL");
                System.out.println("✓ Колонка deal_id в таблице chats теперь nullable");
            } catch (Exception e) {
                System.out.println("⚠ Не удалось изменить колонку deal_id (возможно, уже nullable): " + e.getMessage());
            }
            
            // Создаем таблицу для отслеживания прочитанных сообщений
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS chat_reads (
                        id BIGSERIAL PRIMARY KEY,
                        chat_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        last_read_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(chat_id, user_id),
                        FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
                    )
                    """);
                System.out.println("✓ Таблица chat_reads создана или уже существует");
            } catch (Exception e) {
                System.out.println("⚠ Таблица chat_reads уже существует или ошибка: " + e.getMessage());
            }
            
            // Убеждаемся, что уникальное ограничение существует
            try {
                jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS chat_reads_chat_user_unique 
                    ON chat_reads(chat_id, user_id)
                    """);
                System.out.println("✓ Уникальный индекс chat_reads_chat_user_unique создан или уже существует");
            } catch (Exception e) {
                System.out.println("⚠ Не удалось создать уникальный индекс (возможно, уже существует): " + e.getMessage());
            }
            
            // Создаем таблицу для истории просмотров
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS property_view_history (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        property_id BIGINT NOT NULL,
                        viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        view_count INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
                    )
                    """);
                System.out.println("✓ Таблица property_view_history создана или уже существует");
            } catch (Exception e) {
                System.out.println("⚠ Таблица property_view_history уже существует или ошибка: " + e.getMessage());
            }
            
            // Создаем индексы для property_view_history
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_property ON property_view_history(user_id, property_id)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_viewed_at ON property_view_history(user_id, viewed_at DESC)");
                System.out.println("✓ Индексы для property_view_history созданы или уже существуют");
            } catch (Exception e) {
                System.out.println("⚠ Не удалось создать индексы для property_view_history: " + e.getMessage());
            }
            
            // Создаем таблицу для сохраненных поисковых запросов
            try {
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS saved_searches (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        search_name VARCHAR(255),
                        search_params TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        last_used_at TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """);
                System.out.println("✓ Таблица saved_searches создана или уже существует");
            } catch (Exception e) {
                System.out.println("⚠ Таблица saved_searches уже существует или ошибка: " + e.getMessage());
            }
            
            // Добавляем новые колонки для детальной информации о недвижимости
            String[] newColumns = {
                "year_built INTEGER",
                "building_material VARCHAR(255)",
                "condition VARCHAR(255)",
                "plot_area FLOAT",
                "land_category VARCHAR(255)",
                "property_class VARCHAR(10)",
                "parking VARCHAR(255)",
                "bedrooms INTEGER",
                "bathrooms INTEGER",
                "window_view VARCHAR(255)",
                "furnished BOOLEAN",
                "renovation VARCHAR(255)",
                "heating VARCHAR(255)",
                "water_supply VARCHAR(255)",
                "sewerage VARCHAR(255)",
                "gas VARCHAR(255)",
                "electricity VARCHAR(255)",
                "amenities TEXT"
            };
            
            for (String columnDef : newColumns) {
                String columnName = columnDef.split(" ")[0];
                try {
                    jdbcTemplate.execute("ALTER TABLE properties ADD COLUMN IF NOT EXISTS " + columnDef);
                    System.out.println("✓ Колонка " + columnName + " добавлена или уже существует");
                } catch (Exception e) {
                    System.out.println("⚠ Колонка " + columnName + " уже существует или ошибка: " + e.getMessage());
                }
            }
            
            System.out.println("=== Инициализация пользователей ===");
            System.out.println("Количество пользователей в БД: " + userRepo.count());

            if (userRepo.count() == 0) {
                System.out.println("Создание базовых пользователей (без объявлений)...");
                try {
                    AppUser admin = new AppUser();
                    admin.setEmail("admin@realestate.com");
                    admin.setPasswordHash(encoder.encode("Admin@123"));
                    admin.setFirstName("Администратор");
                    admin.setLastName("Системы");
                    admin.setPhone("+7-999-000-0001");
                    admin.setRole(UserRole.ADMIN);

                    AppUser realtor = new AppUser();
                    realtor.setEmail("realtor@realestate.com");
                    realtor.setPasswordHash(encoder.encode("Realtor@123"));
                    realtor.setFirstName("Анна");
                    realtor.setLastName("Петрова");
                    realtor.setPhone("+7-999-000-0002");
                    realtor.setRole(UserRole.REALTOR);
                    realtor.setRealtorLicense("RL-2024-001");

                    AppUser user = new AppUser();
                    user.setEmail("user@realestate.com");
                    user.setPasswordHash(encoder.encode("User@123"));
                    user.setFirstName("Иван");
                    user.setLastName("Иванов");
                    user.setPhone("+7-999-000-0003");
                    user.setRole(UserRole.USER);

                    userRepo.save(admin);
                    userRepo.save(realtor);
                    userRepo.save(user);
                    System.out.println("✓ Пользователи созданы");
                } catch (Exception e) {
                    System.err.println("Ошибка при создании пользователей: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Отныне объявления не очищаются на старте, данные сохраняются в БД.
            // Код удаления проблемных объявлений "Ул. Волынская" удален, так как он удалял все новые объявления с таким адресом.
            
            // Устанавливаем автором всех объявлений без автора - риелтора tamara@list.ru
            try {
                Optional<AppUser> tamaraOpt = userRepo.findByEmail("tamara@list.ru");
                if (tamaraOpt.isPresent()) {
                    AppUser tamara = tamaraOpt.get();
                    List<com.example.project2.model.Property> propertiesWithoutUser = propertyRepo.findAll().stream()
                            .filter(p -> p.getUser() == null)
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (!propertiesWithoutUser.isEmpty()) {
                        System.out.println("Найдено объявлений без автора: " + propertiesWithoutUser.size());
                        for (com.example.project2.model.Property prop : propertiesWithoutUser) {
                            prop.setUser(tamara);
                            // Если tamara - риелтор, также устанавливаем realtor
                            if (tamara.getRole() == UserRole.REALTOR) {
                                prop.setRealtor(tamara);
                            }
                            propertyRepo.save(prop);
                        }
                        propertyRepo.flush();
                        System.out.println("✓ Автор установлен для " + propertiesWithoutUser.size() + " объявлений");
                    }
                } else {
                    System.out.println("⚠ Риелтор tamara@list.ru не найден, пропускаем установку автора");
                }
            } catch (Exception e) {
                System.err.println("Ошибка при установке автора для объявлений без автора: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}

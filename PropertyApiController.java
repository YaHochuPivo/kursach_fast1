package com.example.project2.controller;

import com.example.project2.dto.PropertyDetailResponse;
import com.example.project2.model.AppUser;
import com.example.project2.model.Property;
import com.example.project2.model.PropertyStatus;
import com.example.project2.model.UserRole;
import com.example.project2.repository.AppUserRepository;
import com.example.project2.service.AdminLogService;
import com.example.project2.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api/properties")
public class PropertyApiController {

    private static final Logger logger = LoggerFactory.getLogger(PropertyApiController.class);
    
    private final PropertyRepository propertyRepository;
    private final AppUserRepository userRepository;
    private final AdminLogService adminLogService;

    public PropertyApiController(PropertyRepository propertyRepository, AppUserRepository userRepository, AdminLogService adminLogService) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.adminLogService = adminLogService;
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','REALTOR','USER')")
    @Transactional
    public ResponseEntity<Property> archive(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).build();
            }
            String email = auth.getName();
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (propertyOpt.isEmpty()) return ResponseEntity.status(404).build();
            AppUser current = userOpt.get();
            Property p = propertyOpt.get();
            if (current.getRole() != UserRole.ADMIN) {
                if (p.getUser() == null || !p.getUser().getId().equals(current.getId())) return ResponseEntity.status(403).build();
            }
            p.setStatus(PropertyStatus.archived);
            Property saved = propertyRepository.save(p);
            propertyRepository.flush();
            adminLogService.log(current, "PROPERTY_ARCHIVE", "PROPERTY", saved.getId(), null);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Ошибка архивирования объявления id={}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('ADMIN','REALTOR','USER')")
    @Transactional
    public ResponseEntity<Property> unarchive(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).build();
            }
            String email = auth.getName();
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (propertyOpt.isEmpty()) return ResponseEntity.status(404).build();
            AppUser current = userOpt.get();
            Property p = propertyOpt.get();
            if (current.getRole() != UserRole.ADMIN) {
                if (p.getUser() == null || !p.getUser().getId().equals(current.getId())) return ResponseEntity.status(403).build();
            }
            if (p.getStatus() == PropertyStatus.archived) {
                p.setStatus(PropertyStatus.active);
            }
            Property saved = propertyRepository.save(p);
            propertyRepository.flush();
            adminLogService.log(current, "PROPERTY_UNARCHIVE", "PROPERTY", saved.getId(), null);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Ошибка разархивирования объявления id={}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping
    public List<Property> list() {
        return propertyRepository.findLatestActive();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDetailResponse> get(@PathVariable Long id) {
        // Используем метод с JOIN FETCH для загрузки user и realtor
        return propertyRepository.findByIdWithUser(id)
                .map(property -> {
                    PropertyDetailResponse response = new PropertyDetailResponse();
                    response.setId(property.getId());
                    response.setType(property.getType());
                    response.setCategory(property.getCategory());
                    response.setAddress(property.getAddress());
                    response.setCity(property.getCity());
                    response.setDistrict(property.getDistrict());
                    response.setArea(property.getArea());
                    response.setRooms(property.getRooms());
                    response.setFloor(property.getFloor());
                    response.setFloorsTotal(property.getFloorsTotal());
                    response.setPrice(property.getPrice());
                    response.setDescription(property.getDescription());
                    response.setImageUrls(property.getImageUrls());
                    response.setPromoted(property.getPromoted());
                    
                    // Заполняем информацию об авторе
                    if (property.getUser() != null) {
                        response.setAuthorEmail(property.getUser().getEmail());
                        response.setAuthorFirstName(property.getUser().getFirstName());
                        response.setAuthorLastName(property.getUser().getLastName());
                    }
                    response.setIsRealtor(property.getRealtor() != null);
                    
                    // Заполняем дополнительные поля
                    response.setKitchenArea(property.getKitchenArea());
                    response.setYearBuilt(property.getYearBuilt());
                    response.setBuildingMaterial(property.getBuildingMaterial());
                    response.setCondition(property.getCondition());
                    response.setPlotArea(property.getPlotArea());
                    response.setLandCategory(property.getLandCategory());
                    response.setPropertyClass(property.getPropertyClass());
                    response.setParking(property.getParking());
                    response.setBedrooms(property.getBedrooms());
                    response.setBathrooms(property.getBathrooms());
                    response.setWindowView(property.getWindowView());
                    response.setFurnished(property.getFurnished());
                    response.setRenovation(property.getRenovation());
                    response.setHeating(property.getHeating());
                    response.setWaterSupply(property.getWaterSupply());
                    response.setSewerage(property.getSewerage());
                    response.setGas(property.getGas());
                    response.setElectricity(property.getElectricity());
                    response.setAmenities(property.getAmenities());
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.<PropertyDetailResponse>notFound().build());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> create(@RequestBody Property p) {
        try {
            logger.info("=== НАЧАЛО СОЗДАНИЯ ОБЪЯВЛЕНИЯ ===");
            logger.info("Создание объявления: {}", p.getAddress());
            
            // Устанавливаем автора объявления (обязательно!)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            logger.debug("Пользователь для создания объявления: {}", email);
            
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                logger.error("ОШИБКА: Пользователь не найден: {}", email);
                return ResponseEntity.status(500).body("{\"error\":\"Пользователь не найден\"}");
            }
            
            AppUser user = userOpt.get();
            p.setUser(user);
            logger.debug("Пользователь установлен: ID={}, Email={}", user.getId(), user.getEmail());
            
            // Если пользователь - риелтор, также устанавливаем realtor
            if (user.getRole() == UserRole.REALTOR) {
                p.setRealtor(user);
                logger.debug("Риелтор установлен: ID={}", user.getId());
            }
            
            // Убеждаемся, что статус установлен
            if (p.getStatus() == null) {
                p.setStatus(com.example.project2.model.PropertyStatus.active);
            }
            
            // Убеждаемся, что promoted установлен
            if (p.getPromoted() == null) {
                p.setPromoted(false);
            }
            
            // Очищаем imageUrls от пробелов
            if (p.getImageUrls() != null) {
                String imageUrls = p.getImageUrls().trim();
                if (!imageUrls.isEmpty()) {
                    String[] urls = imageUrls.split(",");
                    StringBuilder cleanedUrls = new StringBuilder();
                    for (String url : urls) {
                        String trimmed = url.trim();
                        if (!trimmed.isEmpty()) {
                            if (cleanedUrls.length() > 0) {
                                cleanedUrls.append(",");
                            }
                            cleanedUrls.append(trimmed);
                        }
                    }
                    p.setImageUrls(cleanedUrls.toString());
                }
            }
            
            // Устанавливаем createdDate если не установлен
            if (p.getCreatedDate() == null) {
                p.setCreatedDate(java.time.LocalDateTime.now());
            }
            
            // Проверяем, что все обязательные поля установлены перед сохранением
            logger.debug("Перед сохранением - User ID: {}, Status: {}, Promoted: {}, CreatedDate: {}", 
                    p.getUser() != null ? p.getUser().getId() : "NULL", 
                    p.getStatus(), p.getPromoted(), p.getCreatedDate());
            
            if (p.getUser() == null) {
                logger.error("ОШИБКА: User не установлен перед сохранением!");
                return ResponseEntity.status(500).body("{\"error\":\"Ошибка: пользователь не установлен\"}");
            }
            
            // Сохраняем объявление
            logger.info("Сохранение объявления в EntityManager...");
            Property saved = propertyRepository.save(p);
            logger.info("Объявление сохранено в EntityManager с ID: {}", saved.getId());
            
            // Явно синхронизируем с БД (flush синхронизирует изменения с БД)
            // @Transactional автоматически закоммитит транзакцию после успешного завершения метода
            logger.debug("Выполнение flush для синхронизации с БД...");
            propertyRepository.flush();
            logger.info("Flush выполнен - изменения синхронизированы с БД");
            
            logger.info("Объявление сохранено с ID: {}, Address: {}, User ID: {}", 
                    saved.getId(), saved.getAddress(), 
                    saved.getUser() != null ? saved.getUser().getId() : "NULL");
            
            // Проверяем, что объявление действительно сохранено в БД (после flush)
            // Это проверка внутри той же транзакции, поэтому должно работать
            logger.debug("Проверка сохранения объявления в БД...");
            Optional<Property> verifySaved = propertyRepository.findById(saved.getId());
            if (verifySaved.isEmpty()) {
                logger.error("ОШИБКА: Объявление не найдено в БД после flush! ID: {}", saved.getId());
                return ResponseEntity.status(500).body("{\"error\":\"Не удалось сохранить объявление в базу данных\"}");
            }
            
            Property verified = verifySaved.get();
            logger.info("Проверка сохранения - Найдено объявление ID: {}, Address: {}, User ID: {}", 
                    verified.getId(), verified.getAddress(),
                    verified.getUser() != null ? verified.getUser().getId() : "NULL");
            
            // Выводим информацию о транзакции
            logger.debug("Транзакция будет закоммичена автоматически после успешного завершения метода");
            
            // Создаем упрощенный ответ без сложных связей
            Property responseProperty = new Property();
            responseProperty.setId(saved.getId());
            responseProperty.setType(saved.getType());
            responseProperty.setCategory(saved.getCategory());
            responseProperty.setAddress(saved.getAddress());
            responseProperty.setCity(saved.getCity());
            responseProperty.setDistrict(saved.getDistrict());
            responseProperty.setArea(saved.getArea());
            responseProperty.setRooms(saved.getRooms());
            responseProperty.setFloor(saved.getFloor());
            responseProperty.setFloorsTotal(saved.getFloorsTotal());
            responseProperty.setPrice(saved.getPrice());
            responseProperty.setImageUrls(saved.getImageUrls());
            responseProperty.setDescription(saved.getDescription());
            responseProperty.setStatus(saved.getStatus());
            responseProperty.setPromoted(saved.getPromoted());
            responseProperty.setCreatedDate(saved.getCreatedDate());
            // Копируем дополнительные поля
            responseProperty.setKitchenArea(saved.getKitchenArea());
            responseProperty.setYearBuilt(saved.getYearBuilt());
            responseProperty.setBuildingMaterial(saved.getBuildingMaterial());
            responseProperty.setCondition(saved.getCondition());
            responseProperty.setPlotArea(saved.getPlotArea());
            responseProperty.setLandCategory(saved.getLandCategory());
            responseProperty.setPropertyClass(saved.getPropertyClass());
            responseProperty.setParking(saved.getParking());
            responseProperty.setBedrooms(saved.getBedrooms());
            responseProperty.setBathrooms(saved.getBathrooms());
            responseProperty.setWindowView(saved.getWindowView());
            responseProperty.setFurnished(saved.getFurnished());
            responseProperty.setRenovation(saved.getRenovation());
            responseProperty.setHeating(saved.getHeating());
            responseProperty.setWaterSupply(saved.getWaterSupply());
            responseProperty.setSewerage(saved.getSewerage());
            responseProperty.setGas(saved.getGas());
            responseProperty.setElectricity(saved.getElectricity());
            responseProperty.setAmenities(saved.getAmenities());
            
            logger.info("=== УСПЕШНОЕ СОЗДАНИЕ ОБЪЯВЛЕНИЯ ===");
            logger.info("ID объявления: {}, Address: {}", saved.getId(), saved.getAddress());
            logger.info("Транзакция будет закоммичена после успешного завершения метода");

            // Admin log
            adminLogService.log(user, "PROPERTY_CREATE", "PROPERTY", saved.getId(), "Address: " + saved.getAddress());
            
            return ResponseEntity.created(URI.create("/v1/api/properties/" + saved.getId())).body(responseProperty);
        } catch (Exception e) {
            logger.error("=== ОШИБКА ПРИ СОЗДАНИИ ОБЪЯВЛЕНИЯ ===", e);
            logger.error("Ошибка: {}, Класс: {}", e.getMessage(), e.getClass().getName());
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR', 'USER')")
    @Transactional
    public ResponseEntity<Property> update(@PathVariable Long id, @RequestBody Property p) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            
            Optional<Property> existingOpt = propertyRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.<Property>notFound().build();
            }
            
            Property existing = existingOpt.get();
            
            // Проверяем, что пользователь может редактировать только свои объявления (кроме ADMIN)
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                if (user.getRole() != UserRole.ADMIN) {
                    // Проверяем, что объявление принадлежит пользователю
                    if (existing.getUser() == null || !existing.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.<Property>status(403).build();
                    }
                }
            }
            
            p.setId(existing.getId());
            // Сохраняем существующие связи
            p.setUser(existing.getUser());
            p.setRealtor(existing.getRealtor());
            
            // Убеждаемся, что статус установлен
            if (p.getStatus() == null) {
                p.setStatus(existing.getStatus());
            }
            
            // Убеждаемся, что promoted установлен
            if (p.getPromoted() == null) {
                p.setPromoted(existing.getPromoted() != null ? existing.getPromoted() : false);
            }
            
            // Сохраняем createdDate если не установлен
            if (p.getCreatedDate() == null) {
                p.setCreatedDate(existing.getCreatedDate() != null ? existing.getCreatedDate() : java.time.LocalDateTime.now());
            }
            
            Property saved = propertyRepository.save(p);
            
            // Явно сохраняем изменения в БД
            propertyRepository.flush();
            
            System.out.println("Объявление обновлено с ID: " + saved.getId());
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении объявления: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{id}/promote")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR', 'USER')")
    @Transactional
    public ResponseEntity<Property> promoteProperty(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(403).build();
            }
            
            AppUser currentUser = userOpt.get();
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            
            if (propertyOpt.isEmpty()) {
                return ResponseEntity.<Property>notFound().build();
            }
            
            Property property = propertyOpt.get();
            
            // ADMIN может продвигать любые объявления
            // Остальные могут продвигать только свои объявления
            if (currentUser.getRole() != UserRole.ADMIN) {
                // Проверяем, что объявление принадлежит пользователю
                if (property.getUser() == null || !property.getUser().getId().equals(currentUser.getId())) {
                    logger.warn("Попытка продвинуть чужое объявление: User ID={}, Property User ID={}", 
                            currentUser.getId(), property.getUser() != null ? property.getUser().getId() : "NULL");
                    return ResponseEntity.<Property>status(403).build();
                }
            }
            
            property.setPromoted(true);
            Property saved = propertyRepository.save(property);
            propertyRepository.flush();
            
            logger.info("Объявление ID={} продвинуто пользователем {}", saved.getId(), email);
            // Admin log
            adminLogService.log(currentUser, "PROPERTY_PROMOTE", "PROPERTY", saved.getId(), null);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Ошибка при продвижении объявления ID={}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{id}/unpromote")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR', 'USER')")
    @Transactional
    public ResponseEntity<Property> unpromoteProperty(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Optional<AppUser> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(403).build();
            }
            
            AppUser currentUser = userOpt.get();
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            
            if (propertyOpt.isEmpty()) {
                return ResponseEntity.<Property>notFound().build();
            }
            
            Property property = propertyOpt.get();
            
            // ADMIN может снимать продвижение с любых объявлений
            // Остальные могут снимать продвижение только со своих объявлений
            if (currentUser.getRole() != UserRole.ADMIN) {
                // Проверяем, что объявление принадлежит пользователю
                if (property.getUser() == null || !property.getUser().getId().equals(currentUser.getId())) {
                    logger.warn("Попытка снять продвижение с чужого объявления: User ID={}, Property User ID={}", 
                            currentUser.getId(), property.getUser() != null ? property.getUser().getId() : "NULL");
                    return ResponseEntity.<Property>status(403).build();
                }
            }
            
            property.setPromoted(false);
            Property saved = propertyRepository.save(property);
            propertyRepository.flush();
            
            logger.info("Продвижение снято с объявления ID={} пользователем {}", saved.getId(), email);
            // Admin log
            adminLogService.log(currentUser, "PROPERTY_UNPROMOTE", "PROPERTY", saved.getId(), null);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Ошибка при снятии продвижения объявления ID={}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR', 'USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            return ResponseEntity.<Void>notFound().build();
        }
        
        Property property = propertyOpt.get();
        
        // Проверяем, что пользователь может удалять только свои объявления (кроме ADMIN)
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            if (user.getRole() != UserRole.ADMIN) {
                // Проверяем, что объявление принадлежит пользователю
                if (property.getUser() == null || !property.getUser().getId().equals(user.getId())) {
                    return ResponseEntity.<Void>status(403).build();
                }
            }
        }
        
        propertyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

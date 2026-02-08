package com.example.project2.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "properties")
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Тип недвижимости обязателен")
    @Size(max = 50, message = "Тип недвижимости слишком длинный")
    private String type; // квартира, дом, коммерческая

    @NotBlank(message = "Категория обязательна")
    @Size(max = 50, message = "Категория слишком длинная")
    private String category; // Новостройка/Вторичка

    @NotBlank(message = "Адрес обязателен")
    @Size(max = 255, message = "Адрес слишком длинный")
    private String address; // Полный адрес

    private String city; // Город

    private String district; // Район

    @NotNull(message = "Площадь обязательна")
    @Positive(message = "Площадь должна быть положительной")
    private Float area; // общая площадь

    @Positive(message = "Площадь кухни должна быть положительной")
    private Float kitchenArea; // площадь кухни

    @Positive(message = "Количество комнат должно быть положительным")
    private Integer rooms;

    @Positive(message = "Этаж должен быть положительным")
    private Integer floor;

    @Positive(message = "Этажность должна быть положительной")
    private Integer floorsTotal; // этажность дома

    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительной")
    private Float price; // базовая цена

    @Positive(message = "Цена должна быть положительной")
    private Float priceMin; // цена от

    @Positive(message = "Цена должна быть положительной")
    private Float priceMax; // цена до

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String imageUrls; // список URL через запятую

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status = PropertyStatus.active;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtor_id")
    @JsonIgnore
    private AppUser realtor;

    @Column(nullable = false)
    private Boolean promoted = false; // Продвинутое объявление

    // Дополнительные поля для детальной информации
    private Integer yearBuilt; // Год постройки
    private String buildingMaterial; // Материал дома (Деревянный, Кирпичный, Панельный и т.д.)
    private String condition; // Состояние (Можно жить, Требует ремонта, Без ремонта и т.д.)
    private Float plotArea; // Площадь участка (для домов, в сотках)
    private String landCategory; // Категория земель (для домов)
    private String propertyClass; // Класс недвижимости (A, B, C для коммерческих)
    private String parking; // Парковка (количество мест или тип)
    private Integer bedrooms; // Количество спален (для домов)
    private Integer bathrooms; // Количество санузлов
    private String windowView; // Вид из окон (для квартир)
    private Boolean furnished; // Продаётся с мебелью
    private String renovation; // Ремонт (Дизайнерский, Косметический, Без ремонта)
    private String heating; // Отопление (Центральное, Автономное, Газовое)
    private String waterSupply; // Водоснабжение (Центральное, Скважина)
    private String sewerage; // Канализация (Центральная, Септик)
    private String gas; // Газ (Магистральный, Баллонный, Нет)
    private String electricity; // Электричество (Есть, Нет)
    @Column(columnDefinition = "TEXT")
    private String amenities; // Дополнительные удобства (JSON или через запятую: Терраса, Гараж, Баня и т.д.)

    public Property() {
        this.createdDate = LocalDateTime.now();
        this.promoted = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public Float getArea() { return area; }
    public void setArea(Float area) { this.area = area; }

    public Float getKitchenArea() { return kitchenArea; }
    public void setKitchenArea(Float kitchenArea) { this.kitchenArea = kitchenArea; }

    public Integer getRooms() { return rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public Integer getFloorsTotal() { return floorsTotal; }
    public void setFloorsTotal(Integer floorsTotal) { this.floorsTotal = floorsTotal; }

    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }

    public Float getPriceMin() { return priceMin; }
    public void setPriceMin(Float priceMin) { this.priceMin = priceMin; }

    public Float getPriceMax() { return priceMax; }
    public void setPriceMax(Float priceMax) { this.priceMax = priceMax; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }

    public PropertyStatus getStatus() { return status; }
    public void setStatus(PropertyStatus status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public AppUser getRealtor() { return realtor; }
    public void setRealtor(AppUser realtor) { this.realtor = realtor; }

    public Boolean getPromoted() { return promoted; }
    public void setPromoted(Boolean promoted) { this.promoted = promoted; }

    // Getters and Setters для новых полей
    public Integer getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(Integer yearBuilt) { this.yearBuilt = yearBuilt; }

    public String getBuildingMaterial() { return buildingMaterial; }
    public void setBuildingMaterial(String buildingMaterial) { this.buildingMaterial = buildingMaterial; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Float getPlotArea() { return plotArea; }
    public void setPlotArea(Float plotArea) { this.plotArea = plotArea; }

    public String getLandCategory() { return landCategory; }
    public void setLandCategory(String landCategory) { this.landCategory = landCategory; }

    public String getPropertyClass() { return propertyClass; }
    public void setPropertyClass(String propertyClass) { this.propertyClass = propertyClass; }

    public String getParking() { return parking; }
    public void setParking(String parking) { this.parking = parking; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public String getWindowView() { return windowView; }
    public void setWindowView(String windowView) { this.windowView = windowView; }

    public Boolean getFurnished() { return furnished; }
    public void setFurnished(Boolean furnished) { this.furnished = furnished; }

    public String getRenovation() { return renovation; }
    public void setRenovation(String renovation) { this.renovation = renovation; }

    public String getHeating() { return heating; }
    public void setHeating(String heating) { this.heating = heating; }

    public String getWaterSupply() { return waterSupply; }
    public void setWaterSupply(String waterSupply) { this.waterSupply = waterSupply; }

    public String getSewerage() { return sewerage; }
    public void setSewerage(String sewerage) { this.sewerage = sewerage; }

    public String getGas() { return gas; }
    public void setGas(String gas) { this.gas = gas; }

    public String getElectricity() { return electricity; }
    public void setElectricity(String electricity) { this.electricity = electricity; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    @Transient
    public String getTitleForCard() {
        try {
            if (city != null && district != null && type != null) {
                return type + " · " + city + ", " + district;
            }
            if (address != null && !address.trim().isEmpty()) {
                return address;
            }
            return "Объект недвижимости";
        } catch (Exception e) {
            return "Объект недвижимости";
        }
    }

    @Transient
    public String getFirstImageUrl() {
        try {
            if (imageUrls == null || imageUrls.isBlank()) return null;
            String[] parts = imageUrls.split(",");
            if (parts.length == 0) return null;
            String url = parts[0].trim();
            return url.isEmpty() ? null : url;
        } catch (Exception e) {
            return null;
        }
    }

    @Transient
    public List<String> getImageUrlList() {
        List<String> result = new ArrayList<>();
        if (imageUrls == null || imageUrls.isBlank()) return result;
        Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(result::add);
        return result;
    }
}

package com.example.project2.dto;

import java.util.List;

public class PropertyDetailResponse {
    private Long id;
    private String type;
    private String category;
    private String address;
    private String city;
    private String district;
    private Float area;
    private Integer rooms;
    private Integer floor;
    private Integer floorsTotal;
    private Float price;
    private String description;
    private String imageUrls;
    private Boolean promoted;
    private String authorEmail;
    private String authorFirstName;
    private String authorLastName;
    private Boolean isRealtor;
    
    // Дополнительные поля
    private Float kitchenArea;
    private Integer yearBuilt;
    private String buildingMaterial;
    private String condition;
    private Float plotArea;
    private String landCategory;
    private String propertyClass;
    private String parking;
    private Integer bedrooms;
    private Integer bathrooms;
    private String windowView;
    private Boolean furnished;
    private String renovation;
    private String heating;
    private String waterSupply;
    private String sewerage;
    private String gas;
    private String electricity;
    private String amenities;

    // Getters and Setters
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
    public Integer getRooms() { return rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public Integer getFloorsTotal() { return floorsTotal; }
    public void setFloorsTotal(Integer floorsTotal) { this.floorsTotal = floorsTotal; }
    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public Boolean getPromoted() { return promoted; }
    public void setPromoted(Boolean promoted) { this.promoted = promoted; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getAuthorFirstName() { return authorFirstName; }
    public void setAuthorFirstName(String authorFirstName) { this.authorFirstName = authorFirstName; }
    public String getAuthorLastName() { return authorLastName; }
    public void setAuthorLastName(String authorLastName) { this.authorLastName = authorLastName; }
    public Boolean getIsRealtor() { return isRealtor; }
    public void setIsRealtor(Boolean isRealtor) { this.isRealtor = isRealtor; }

    // Getters and Setters для дополнительных полей
    public Float getKitchenArea() { return kitchenArea; }
    public void setKitchenArea(Float kitchenArea) { this.kitchenArea = kitchenArea; }

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

    public List<String> getImageUrlList() {
        return imageUrls != null && !imageUrls.isBlank()
                ? List.of(imageUrls.split(","))
                : List.of();
    }
}


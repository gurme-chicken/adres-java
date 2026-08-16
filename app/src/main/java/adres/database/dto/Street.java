package adres.database.dto;

import com.google.gson.annotations.SerializedName;

public class Street {

    @SerializedName("posta_Kodu")
    private String postalCode;
    @SerializedName("mahalleAdi")
    private String neighborhoodName;
    @SerializedName("sokakAdi")
    private String streetName;
    @SerializedName(value = "ilKod", alternate = "il_kod")
    private int provinceCode;
    @SerializedName(value = "ilceKod", alternate = "ilce_kod")
    private int districtCode;

    public Street() {
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getNeighborhoodName() {
        return neighborhoodName;
    }

    public void setNeighborhoodName(String neighborhoodName) {
        this.neighborhoodName = neighborhoodName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }

    public int getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(int districtCode) {
        this.districtCode = districtCode;
    }
}

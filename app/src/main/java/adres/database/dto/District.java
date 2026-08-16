package adres.database.dto;

import com.google.gson.annotations.SerializedName;

public class District {

    @SerializedName("kod")
    private int code;
    @SerializedName("ad")
    private String name;
    @SerializedName(value = "ilKod", alternate = "il_kod")
    private int provinceCode;

    public District() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}

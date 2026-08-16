package adres.database.dto;

import com.google.gson.annotations.SerializedName;

public class Province {

    @SerializedName("kod")
    private int code;
    @SerializedName("ad")
    private String name;

    public Province() {
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
}

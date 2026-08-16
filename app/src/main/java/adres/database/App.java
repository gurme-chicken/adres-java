package adres.database;

import adres.database.dto.District;
import adres.database.dto.Province;
import adres.database.dto.Street;
import adres.database.service.PttApiService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static final Gson GSON = new Gson();
    private static final Type PROVINCE_LIST = new TypeToken<List<Province>>() {
    }.getType();
    private static final Type DISTRICT_LIST = new TypeToken<List<District>>() {
    }.getType();
    private static final Type STREET_LIST = new TypeToken<List<Street>>() {
    }.getType();

    private static final Path DATA_DIR = Path.of("data");
    private static final Path CACHE_DIR = DATA_DIR.resolve("cache");

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "fetch";
        Files.createDirectories(CACHE_DIR);
        if (mode.equals("sql")) {
            SqlDumpGenerator.generate(DATA_DIR);
        } else {
            fetchAll();
            SqlDumpGenerator.generate(DATA_DIR);
        }
    }

    private static void fetchAll() throws Exception {
        PttApiService service = new PttApiService();

        Path provinceFile = CACHE_DIR.resolve("iller.json");
        List<Province> provinces;
        if (Files.exists(provinceFile)) {
            provinces = GSON.fromJson(Files.readString(provinceFile), PROVINCE_LIST);
            log("İller cache'ten okundu: " + provinces.size());
        } else {
            provinces = service.getProvinces();
            Files.writeString(provinceFile, GSON.toJson(provinces), StandardCharsets.UTF_8);
            log("İller çekildi: " + provinces.size());
        }

        List<District> districts = new ArrayList<>();
        for (Province p : provinces) {
            Path file = CACHE_DIR.resolve("ilce_" + p.getCode() + ".json");
            List<District> list;
            if (Files.exists(file)) {
                list = GSON.fromJson(Files.readString(file), DISTRICT_LIST);
            } else {
                list = service.getDistricts(p.getCode());
                Files.writeString(file, GSON.toJson(list), StandardCharsets.UTF_8);
                log("İlçeler çekildi: " + p.getName() + " -> " + list.size() + " ilçe");
            }
            districts.addAll(list);
        }
        log("Toplam ilçe: " + districts.size());

        int done = 0;
        int total = districts.size();
        for (District d : districts) {
            Path file = CACHE_DIR.resolve("sokak_" + d.getProvinceCode() + "_" + d.getCode() + ".json");
            if (Files.exists(file)) {
                done++;
                continue;
            }
            try {
                List<Street> list = service.getStreets(d.getProvinceCode(), d.getCode());
                Files.writeString(file, GSON.toJson(list), StandardCharsets.UTF_8);
                done++;
                if (done % 10 == 0) {
                    log("Sokak ilerleme: " + done + "/" + total);
                }
            } catch (Exception e) {
                log("HATA: il=" + d.getProvinceCode() + " ilçe=" + d.getCode() + " (" + d.getName() + ") -> " + e.getMessage());
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log("Çekim bitti: " + done + "/" + total + " ilçe");
    }

    private static void log(String message) {
        System.out.println(message);
        try {
            Files.writeString(DATA_DIR.resolve("fetch.log"), message + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}

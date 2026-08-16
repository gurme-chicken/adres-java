package adres.database;

import adres.database.dto.District;
import adres.database.dto.Province;
import adres.database.dto.Street;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SqlDumpGenerator {

    private static final Gson GSON = new Gson();
    private static final Type PROVINCE_LIST = new TypeToken<List<Province>>() {
    }.getType();
    private static final Type DISTRICT_LIST = new TypeToken<List<District>>() {
    }.getType();
    private static final Type STREET_LIST = new TypeToken<List<Street>>() {
    }.getType();

    public static void generate(Path dataDir) throws IOException {
        Path cacheDir = dataDir.resolve("cache");
        Path output = dataDir.resolve("adres_database.sql");

        List<Province> provinces = GSON.fromJson(Files.readString(cacheDir.resolve("iller.json")), PROVINCE_LIST);
        provinces.sort(Comparator.comparingInt(Province::getCode));

        List<District> districts = new ArrayList<>();
        for (Province p : provinces) {
            Path file = cacheDir.resolve("ilce_" + p.getCode() + ".json");
            if (Files.exists(file)) {
                districts.addAll(GSON.fromJson(Files.readString(file), DISTRICT_LIST));
            }
        }
        districts.sort(Comparator.comparingInt(District::getCode));

        long counter = 0;
        try (BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writeHeader(w);
            w.write("CREATE TABLE public.iller (\n" +
                    "    kod integer NOT NULL,\n" +
                    "    adi text NOT NULL\n" +
                    ");\n\n");
            w.write("ALTER TABLE ONLY public.iller\n" +
                    "    ADD CONSTRAINT iller_pkey PRIMARY KEY (kod);\n\n");

            w.write("CREATE TABLE public.ilceler (\n" +
                    "    kod integer NOT NULL,\n" +
                    "    il_kod integer NOT NULL,\n" +
                    "    adi text NOT NULL\n" +
                    ");\n\n");
            w.write("ALTER TABLE ONLY public.ilceler\n" +
                    "    ADD CONSTRAINT ilceler_pkey PRIMARY KEY (kod);\n\n");
            w.write("ALTER TABLE ONLY public.ilceler\n" +
                    "    ADD CONSTRAINT ilceler_il_kod_fkey FOREIGN KEY (il_kod) REFERENCES public.iller(kod);\n\n");

            w.write("CREATE TABLE public.sokaklar (\n" +
                    "    id bigint NOT NULL,\n" +
                    "    il_kod integer NOT NULL,\n" +
                    "    ilce_kod integer NOT NULL,\n" +
                    "    mahalle_adi text,\n" +
                    "    sokak_adi text,\n" +
                    "    posta_kodu text\n" +
                    ");\n\n");
            w.write("CREATE SEQUENCE public.sokaklar_id_seq\n" +
                    "    START WITH 1\n" +
                    "    INCREMENT BY 1\n" +
                    "    NO MINVALUE\n" +
                    "    NO MAXVALUE\n" +
                    "    CACHE 1;\n\n");
            w.write("ALTER SEQUENCE public.sokaklar_id_seq OWNED BY public.sokaklar.id;\n\n");
            w.write("ALTER TABLE ONLY public.sokaklar ALTER COLUMN id SET DEFAULT nextval('public.sokaklar_id_seq'::regclass);\n\n");
            w.write("ALTER TABLE ONLY public.sokaklar\n" +
                    "    ADD CONSTRAINT sokaklar_pkey PRIMARY KEY (id);\n\n");
            w.write("ALTER TABLE ONLY public.sokaklar\n" +
                    "    ADD CONSTRAINT sokaklar_il_kod_fkey FOREIGN KEY (il_kod) REFERENCES public.iller(kod);\n\n");
            w.write("ALTER TABLE ONLY public.sokaklar\n" +
                    "    ADD CONSTRAINT sokaklar_ilce_kod_fkey FOREIGN KEY (ilce_kod) REFERENCES public.ilceler(kod);\n\n");

            w.write("COPY public.iller (kod, adi) FROM stdin;\n");
            for (Province p : provinces) {
                w.write(p.getCode() + "\t" + escape(p.getName()) + "\n");
            }
            w.write("\\.\n\n");

            w.write("COPY public.ilceler (kod, il_kod, adi) FROM stdin;\n");
            for (District d : districts) {
                w.write(d.getCode() + "\t" + d.getProvinceCode() + "\t" + escape(d.getName()) + "\n");
            }
            w.write("\\.\n\n");

            w.write("COPY public.sokaklar (id, il_kod, ilce_kod, mahalle_adi, sokak_adi, posta_kodu) FROM stdin;\n");
            for (District d : districts) {
                Path file = cacheDir.resolve("sokak_" + d.getProvinceCode() + "_" + d.getCode() + ".json");
                if (!Files.exists(file)) {
                    continue;
                }
                List<Street> streets = GSON.fromJson(Files.readString(file), STREET_LIST);
                for (Street s : streets) {
                    counter++;
                    w.write(counter + "\t" + s.getProvinceCode() + "\t" + s.getDistrictCode() + "\t"
                            + escape(s.getNeighborhoodName()) + "\t" + escape(s.getStreetName()) + "\t" + escape(s.getPostalCode()) + "\n");
                }
            }
            w.write("\\.\n\n");

            // sequence'i son id'ye eşitlemezsen sonraki ekelmelerde çakışıyor
            w.write("SELECT pg_catalog.setval('public.sokaklar_id_seq', " + counter + ", true);\n\n");
            w.write("COMMIT;\n");
        }
        System.out.println("SQL dump üretildi: " + output.toAbsolutePath() + " (" + provinces.size() + " il, "
                + districts.size() + " ilce, " + counter + " sokak)");
    }

    private static void writeHeader(BufferedWriter w) throws IOException {
        w.write("--\n" +
                "-- PostgreSQL database dump\n" +
                "--\n\n");
        w.write("BEGIN;\n\n");
        w.write("SET statement_timeout = 0;\n" +
                "SET lock_timeout = 0;\n" +
                "SET idle_in_transaction_session_timeout = 0;\n" +
                "SET client_encoding = 'UTF8';\n" +
                "SET standard_conforming_strings = on;\n" +
                "SELECT pg_catalog.set_config('search_path', '', false);\n\n");
    }

    static String escape(String s) {
        if (s == null) {
            return "\\N";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}

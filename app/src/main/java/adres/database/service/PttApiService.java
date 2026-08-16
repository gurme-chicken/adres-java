package adres.database.service;

import adres.database.dto.District;
import adres.database.dto.Province;
import adres.database.dto.Street;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PttApiService {

    private static final String URL = "https://www.ptt.gov.tr/api/posta-kodu";
    private static final int MAX_ATTEMPTS = 6;

    private final HttpClient client;
    private final Gson gson = new Gson();

    public PttApiService() {
        this.client = HttpClient.newBuilder()
                .sslContext(createSslContext())
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public List<Province> getProvinces() {
        Type type = new TypeToken<List<Province>>() {
        }.getType();
        List<Province> all = gson.fromJson(postWithRetry("{\"action\":\"iller\"}"), type);
        return all.stream().filter(p -> p.getCode() != -1).toList();
    }

    public List<District> getDistricts(int provinceCode) {
        Type type = new TypeToken<List<District>>() {
        }.getType();
        String body = "{\"action\":\"ilceler\",\"il_kodu\":\"" + provinceCode + "\"}";
        List<District> all = gson.fromJson(postWithRetry(body), type);
        List<District> list = new ArrayList<>();
        for (District d : all) {
            if (d.getCode() != -1) {
                d.setProvinceCode(provinceCode);
                list.add(d);
            }
        }
        return list;
    }

    public List<Street> getStreets(int provinceCode, int districtCode) {
        Type type = new TypeToken<List<Street>>() {
        }.getType();
        String body = "{\"action\":\"postakodu\",\"il_kodu\":\"" + provinceCode + "\",\"ilce_kodu\":\"" + districtCode + "\"}";
        List<Street> list = gson.fromJson(postWithRetry(body), type);
        for (Street s : list) {
            s.setProvinceCode(provinceCode);
            s.setDistrictCode(districtCode);
        }
        return list;
    }

    private String postWithRetry(String body) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .timeout(Duration.ofSeconds(180))
                        .header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:149.0) Gecko/20100101 Firefox/149.0")
                        .header("Accept", "*/*")
                        .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8;en;q=0.7")
                        .header("Content-Type", "application/json")
                        .header("Sec-Fetch-Dest", "empty")
                        .header("Sec-Fetch-Mode", "cors")
                        .header("Sec-Fetch-Site", "same-origin")
                        .header("Priority", "u=0")
                        .header("Referer", "https://www.ptt.gov.tr/posta-kodu")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body();
                }
                lastError = new RuntimeException("HTTP " + response.statusCode());
            } catch (Exception e) {
                lastError = e;
            }
            // arka arkaya istek atınca blokluyor, o yüzden biraz bekliyoruz
            try {
                Thread.sleep(2000L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("İstek kesildi", ie);
            }
        }
        throw new RuntimeException("PTT API isteği başarısız: " + body + " -> " + lastError.getMessage(), lastError);
    }

    // ptt.gov.tr'deki GlobalSign OV sertifikası JVM'in default truststore'unda yok,
    // o yüzden kök sertifikayı pakete ekleyip ek truststore olarak kulanıyoruz
    private SSLContext createSslContext() {
        try {
            X509TrustManager defaultTm = (X509TrustManager) defaultTrustManager();

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream is = getClass().getResourceAsStream("/globalsign-ov-2018.crt")) {
                ks.setCertificateEntry("globalsign-ov-2018", cf.generateCertificate(is));
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            X509TrustManager custom = (X509TrustManager) tmf.getTrustManagers()[0];

            CompositeTrustManager composite = new CompositeTrustManager(defaultTm, custom);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{composite}, null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("SSL bağlamı kurulamadı", e);
        }
    }

    private TrustManager defaultTrustManager() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        return tmf.getTrustManagers()[0];
    }

    private static class CompositeTrustManager implements X509TrustManager {
        private final X509TrustManager[] managers;

        CompositeTrustManager(X509TrustManager... managers) {
            this.managers = managers;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            managers[0].checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException lastError = null;
            for (X509TrustManager m : managers) {
                try {
                    m.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    lastError = e;
                }
            }
            throw lastError;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}

package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.service.ComplaintCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ComplaintCategoryServiceImpl implements ComplaintCategoryService {

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    @Override
    public String categorize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<String> matched = categorizeAll(text);
        return matched.isEmpty() ? "Diğer" : matched.get(0);
    }

    @Override
    public List<String> categorizeAll(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(TR_LOCALE);
        List<String> categories = new ArrayList<>();

        if (contains(normalized, "bekleme", "bekledik", "bekledim", "waiting", "wait", "sira", "sıra", "gecik")) {
            categories.add("Bekleme süresi");
        }
        if (contains(normalized, "personel", "iletişim", "iletisim", "staff", "communication", "davranış", "davranis", "ilgisiz", "kaba")) {
            categories.add("Personel iletişimi");
        }
        if (contains(normalized, "bilgi", "bilgilendirme", "information", "açıklama", "aciklama", "yönlendirme", "yonlendirme")) {
            categories.add("Bilgilendirme eksikliği");
        }
        if (contains(normalized, "temizlik", "temiz", "clean", "kirlilik", "kirli", "hijyen")) {
            categories.add("Temizlik");
        }
        if (contains(normalized, "oda", "room", "ortam", "fiziksel", "ses", "gürültü", "gurultu", "soğuk", "soguk", "sıcak", "sicak", "yatak", "koltuk")) {
            categories.add("Fiziksel ortam");
        }
        if (contains(normalized, "yoğun", "yogun", "kalabalık", "kalabalik", "crowd", "busy")) {
            categories.add("Yoğunluk");
        }
        if (contains(normalized, "randevu", "appointment", "booking")) {
            categories.add("Randevu süreci");
        }
        return categories;
    }

    private boolean contains(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}

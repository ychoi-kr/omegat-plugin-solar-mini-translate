package kr.ychoi.otplugin;

import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.util.Language;
import org.omegat.util.WikiGet;
import org.json.*;

/*
 * SOLAR Translate plugin for OmegaT
 * based on https://github.com/ychoi-kr/omegat-plugin-openai-translate
 * licensed under GNU GPLv2 and modified by ychoi
 */


@SuppressWarnings({"unchecked", "rawtypes"})
public class SolarMiniTranslate extends BaseTranslate {

    private static final String API_URL = "https://api.upstage.ai/v1/solar/chat/completions";
    private static final String API_KEY = System.getProperty("solar.api.key");

    private static final Map<String, String> translationCache = new HashMap<>();

    @Override
    protected String getPreferenceName() {
        return "allow_solar_translate";
    }

    public String getName() {
        if (API_KEY == null) {
            return "Solar Mini Translate (API Key Required)";
        } else {
            return "Solar Mini Translate";
        }
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        if (API_KEY == null) {
            return "";
        }

        String lvSourceLang = sLang.getLanguageCode().substring(0, 2).toLowerCase();
        String lvTargetLang = tLang.getLanguageCode().substring(0, 2).toLowerCase();

        String model;
        JSONArray messages = new JSONArray();

        // 한영 번역 설정
        if (lvSourceLang.equals("ko") && lvTargetLang.equals("en")) {
            model = "solar-1-mini-translate-koen";
            messages.put(new JSONObject().put("role", "user").put("content", "아버지가 방에 들어가셨다"));
            messages.put(new JSONObject().put("role", "assistant").put("content", "Father went into his room"));
            messages.put(new JSONObject().put("role", "user").put("content", text));
        }
        // 영한 번역 설정
        else if (lvSourceLang.equals("en") && lvTargetLang.equals("ko")) {
            model = "solar-1-mini-translate-enko";
            messages.put(new JSONObject().put("role", "user").put("content", "Father went into his room"));
            messages.put(new JSONObject().put("role", "assistant").put("content", "아버지가 방에 들어가셨다"));
            messages.put(new JSONObject().put("role", "user").put("content", text));
        } else {
            return "Unsupported language pair";
        }

        String cacheKey = lvSourceLang + '-' + lvTargetLang + text;
        String cachedResult = translationCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + API_KEY);

        String body = new JSONObject()
                .put("model", model)
                .put("messages", messages)
                .toString();

        System.out.println(body);
        String response = WikiGet.postJSON(API_URL, body, headers);
        JSONObject jsonResponse = new JSONObject(response);
        // 새 API 응답 구조에 맞게 번역된 텍스트 추출
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (choices.length() > 0) {
            JSONObject choice = choices.getJSONObject(0);
            if (choice.has("message")) {
            	JSONObject message = choice.getJSONObject("message");
                String translatedText = message.getString("content").trim();
                translationCache.put(cacheKey, translatedText);
                return translatedText;
            }
        }
        return "Translation failed";
    }
}


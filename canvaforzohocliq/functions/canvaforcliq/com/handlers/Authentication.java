package com.handlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.zc.cliq.enums.FORM_FIELD_TEXT_FORMAT;
import com.zc.cliq.enums.FORM_FIELD_TYPE;
import com.zc.cliq.objects.CommandSuggestion;
import com.zc.cliq.objects.Form;
import com.zc.cliq.objects.FormActionsObject;
import com.zc.cliq.objects.FormInput;
import com.zc.cliq.objects.FormValue;
import com.zc.cliq.requests.CommandHandlerRequest;
import com.zc.cliq.util.ZCCliqUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCTable;
import com.zc.component.object.ZCRowObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.zc.component.zcql.ZCQL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.TimeZone;


public class Authentication {
    public static boolean isAuthorized(String zohoUserId) throws  Exception {
        String query = String.format("SELECT * FROM authentication WHERE zoho_user_id = '%s'", zohoUserId);
        ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);
        return rowList.isEmpty();
    }
    public static  Map<String, Object> sendAuthorizeMessage(String url) throws Exception {
        String authorizeMessage = """
        {
          "text": "To use the Canva extension, you need to authorize it. Click the button below to proceed.",
          "bot": {
            "name": "Canva",
            "image": "https://res.cloudinary.com/dui2rawnc/image/upload/v1734008771/canva-removebg-preview_1_j2fvg7.png"
          },
          "card": {
            "title": "Canva Extension Authorization",
            "theme": "prompt"
          },
          "buttons": [
            {
              "label": "Authorize",
              "hint": "",
              "type": "+",
              "action": {
                "type": "open.url",
                "data": {
                  "web": "%s"
                }
              }
            }
          ]
        }
        """.formatted(url);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> response = objectMapper.readValue(authorizeMessage,new TypeReference<Map<String, Object>>() {});
        return response;
    }

    public static String authenticate(String zohoUserId) throws Exception{
        String query = String.format("SELECT * FROM authentication WHERE zoho_user_id = '%s'", zohoUserId);
        ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);
        ZCRowObject row = rowList.get(0);
        String tokenExpiry = (String) row.get("token_expiry");

        if (!isAccessTokenExpired(tokenExpiry.strip())) {
            String accessToken = (String) row.get("access_token");
            return accessToken;
        }

        String authHeader = (String) row.get("auth_header");
        String refreshToken = (String) row.get("refresh_token");

        String body = String.join("&",
                "grant_type=" + URLEncoder.encode("refresh_token", StandardCharsets.UTF_8),
                "refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.canva.com/rest/v1/oauth/token"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.body());
        insertData(zohoUserId, response);
        return jsonNode.get("access_token").asText();
    }

    public static boolean insertData(String zohoUserId,HttpResponse<String> response) throws  Exception{
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body());

            String query = String.format("SELECT * FROM authentication WHERE zoho_user_id = '%s'", zohoUserId);
            ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);

            Long rowId = Long.parseLong((String) rowList.get(0).get("ROWID"));

            ZCObject object = ZCObject.getInstance();
            ZCTable table = object.getTable(27074000000009053L);

            List<ZCRowObject> rows = new ArrayList<>();
            ZCRowObject row = ZCRowObject.getInstance();

            row.set("access_token", jsonNode.get("access_token").asText());
            row.set("refresh_token", jsonNode.get("refresh_token").asText());

            int expiresInSeconds = Integer.parseInt(jsonNode.get("expires_in").asText());
            String expiresIn = getExpiresInTimeAsString(expiresInSeconds);
            row.set("token_expiry", expiresIn);
            row.set("ROWID", rowId);
            rows.add(row);
            table.updateRows(rows);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public static String getExpiresInTimeAsString(int expiresInSeconds) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime updatedDateTime = currentDateTime.plusSeconds(expiresInSeconds);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return updatedDateTime.format(formatter);
    }

    private static boolean isAccessTokenExpired(String tokenExpiryInString) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime tokenExpiryDateTime = LocalDateTime.parse(tokenExpiryInString, formatter);

        LocalDateTime currentDateTime = LocalDateTime.now();

        return !currentDateTime.isBefore(tokenExpiryDateTime);
    }

    public static Map<String, Object> authorize(String zohoUserId) throws Exception{
        String clientId =  System.getenv("client_id");
        String clientSecret =  System.getenv("client_secret");
        String scope =  System.getenv("scope");
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
        String state = generateState();

        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        ZCObject object = ZCObject.getInstance();
        ZCTable table = object.getTable(27074000000009053L);
        ZCRowObject row = ZCRowObject.getInstance();
        row.set("zoho_user_id" , zohoUserId);
        row.set("state", state);
        row.set("code_verifier", codeVerifier);
        table.insertRow(row);


        String authorizationUrl = "https://www.canva.com/api/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&scope=" + encodedScope
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256"
                + "&state=" + state;
        return Authentication.sendAuthorizeMessage(authorizationUrl);
    }
    private static String generateCodeVerifier() throws Exception{
        byte[] randomBytes = new byte[64];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
                .replace('+', '-').replace('/', '_');
    }

    private static String generateCodeChallenge(String codeVerifier)  throws Exception{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes("UTF-8"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
                .replace('+', '-').replace('/', '_');
    }
    public static String generateState()  throws Exception{
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[96];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
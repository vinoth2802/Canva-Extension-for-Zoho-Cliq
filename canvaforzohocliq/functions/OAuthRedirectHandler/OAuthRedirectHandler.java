import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.catalyst.advanced.CatalystAdvancedIOHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.zc.component.zcql.ZCQL;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCTable;
import com.zc.component.object.ZCRowObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.TimeZone;


public class OAuthRedirectHandler implements CatalystAdvancedIOHandler {
	private static final Logger LOGGER = Logger.getLogger(OAuthRedirectHandler.class.getName());
	
	@Override
    public void runner(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			switch(request.getRequestURI()) {
				case "/": {
					String state = request.getParameter("state");
					String code = request.getParameter("code");
					String clientId = System.getenv("client_id");
					String clientSecret = System.getenv("client_secret");
					String[] details = getZohoUserIdAndCodeVerifier(state);
					String zohoUserId = details[0];
					String codeVerifier = details[1];

					// Create Basic Auth Header
					String credentials = clientId + ":" + clientSecret;
					String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
					String authHeader = "Basic " + encodedCredentials;

					// Build the POST request body
					String body = String.join("&",
							"grant_type=" + URLEncoder.encode("authorization_code", StandardCharsets.UTF_8),
							"code_verifier=" + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8),
							"code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
					);

					// Build the HTTP request
					HttpClient client = HttpClient.newHttpClient();
					HttpRequest request1 = HttpRequest.newBuilder()
							.uri(URI.create("https://api.canva.com/rest/v1/oauth/token"))
							.header("Authorization", authHeader)
							.header("Content-Type", "application/x-www-form-urlencoded")
							.POST(HttpRequest.BodyPublishers.ofString(body))
							.build();

					// Send the request and handle the response
					HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
					if (insertData(zohoUserId, authHeader,response1)) {
						response.setContentType("text/html");
						response.getWriter().write(getSuccessPage());
					} else {
						response.getWriter().write("Failed to save data.");
					}
					break;	
				}
				default: {
					response.setStatus(404);
					response.getWriter().write("You might find the page you are looking for at \"/\" path");
				}
			}
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception in OAuthRedirectHandler",e);
			response.setStatus(500);
			response.getWriter().write("Internal server error");
		}
	}
	public static String[] getZohoUserIdAndCodeVerifier(String state) throws Exception{
			String query = "SELECT * FROM authentication";
			ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);
			String codeVerifier = "";
			String zohoUserId = "";
			for (ZCRowObject row : rowList) {
				if (row.get("state").equals(state)) {
					codeVerifier =  (String) row.get("code_verifier");
					zohoUserId = (String) row.get("zoho_user_id");
				}
			}
			return new String[] {zohoUserId, codeVerifier};
	}

	public static boolean insertData(String zohoUserId, String authHeader,HttpResponse<String> response1) throws  Exception{
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(response1.body());

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
			row.set("auth_header", authHeader);
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
	private static String  getSuccessPage() {
		return """
				<!DOCTYPE html>
				               <html>
				               <head>
				               <link rel="icon" type="image/png" href="https://res.cloudinary.com/dui2rawnc/image/upload/v1734008771/canva-removebg-preview_1_j2fvg7.png">
				                   <title>Authorization Successful</title>
				                   <style>
				                       body {
				                           margin: 0;
				                           padding: 0;
				                           box-sizing: border-box;
				                           font-family: Roboto;
				                           font-size: 16px;
				                           position: relative;
				                           display: flex;
				                           align-items: center;
				                           justify-content: center;
				                           flex-direction: column;
				                           height: 100vh;
				                       }
				                       h1 {
				                           background: linear-gradient(45deg, #0182ad, #4313c6);
				                           -webkit-background-clip: text;
				                           color: transparent;
				                           font-size: 30px;
				                       }
				                       p{
				                           color: #1e0641;
				                           font-size: 20px;
				                       }
				                       img{
				                           height: 15vh;
				                           margin-bottom: 2rem;
				                       }
				                   </style>
				               </head>
				               <body>
				                   <img src="https://res.cloudinary.com/dui2rawnc/image/upload/v1733939743/checked_sefcrz.png" alt="">
				                   <h1>Authorization Successful !</h1>
				                   <p>You can now go back to Zoho Cliq and access Canva</p>
				               </body>
				               </html>
				    """;
	}
}
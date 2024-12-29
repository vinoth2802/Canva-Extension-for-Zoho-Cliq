//$Id$
package com.handlers;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.zc.cliq.objects.DateTimeObject;
import com.zc.cliq.objects.ButtonObject;
import com.zc.cliq.objects.*;
import com.zc.cliq.enums.*;
import com.zc.cliq.requests.CommandHandlerRequest;
import com.zc.cliq.util.ZCCliqUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCTable;
import com.zc.component.object.ZCRowObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zc.component.zcql.ZCQL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CommandHandler implements com.zc.cliq.interfaces.CommandHandler {
	@Override
	public Map<String, Object> executionHandler(CommandHandlerRequest req) throws Exception {
		String zohoUserId = req.getUser().getId();
		Map<String, Object> response = new HashMap<>();
		if (Authentication.isAuthorized(zohoUserId)) {
			return  Authentication.authorize(zohoUserId);
		}

		String accessToken = Authentication.authenticate(zohoUserId);

		try {

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://api.canva.com/rest/v1/designs"))
					.header("Authorization", "Bearer " + accessToken)
					.GET()
					.build();

			HttpResponse<String> jsonResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
			// Root Level
			response.put("text", "Here is the list of your canva projects. You can view or edit the projects.");

			// Bot Map
			Map<String, String> botMap = new HashMap<>();
			botMap.put("name", "Canva");
			botMap.put("image", "https://res.cloudinary.com/dui2rawnc/image/upload/v1734008771/canva-removebg-preview_1_j2fvg7.png");
			response.put("bot", botMap);

			// Card Map
			Map<String, String> cardMap = new HashMap<>();
			cardMap.put("theme", "modern-inline");
			response.put("card", cardMap);

			// Slides List
			List<Map<String, Object>> slidesList = new ArrayList<>();
			Map<String, Object> slide = new HashMap<>();
			slide.put("type", "table");
			slide.put("title", "List of your canva projects");

			// Data Map
			Map<String, Object> dataMap = new HashMap<>();

			// Headers List
			List<String> headers = Arrays.asList("Project Name", "View", "Edit");
			dataMap.put("headers", headers);

			// Rows List
			List<Map<String, Object>> rows = new ArrayList<>();
			ObjectMapper objectMapper = new ObjectMapper();

			List<String> titles = new ArrayList<>();
			List<String> viewUrls = new ArrayList<>();
			List<String> editUrls = new ArrayList<>();

			JsonNode rootNode = objectMapper.readTree(jsonResponse.body());
			JsonNode itemsArray = rootNode.path("items");
			for (JsonNode item : itemsArray) {
				String title = (item.has("title") && !item.get("title").isNull()) ? item.get("title").asText() : "Untitled";

				JsonNode urlsNode = item.path("urls");
				String viewUrl = (urlsNode.has("view_url") && !urlsNode.get("view_url").isNull()) ? urlsNode.get("view_url").asText() : "";
				String editUrl = (urlsNode.has("edit_url") && !urlsNode.get("edit_url").isNull()) ? urlsNode.get("edit_url").asText() : "";


				titles.add(title);
				viewUrls.add(viewUrl);
				editUrls.add(editUrl);
			}

			for (int i = 0; i < titles.size(); i++) {
				String title = titles.get(i);
				String viewUrl = viewUrls.get(i);
				String editUrl = editUrls.get(i);

//				Button viewButton = getButton(viewUrl,"view");
//				Button editButton = getButton(editUrl, "edit");

				String tinyViewUrl = getTinrUrl(viewUrl);
				String tinyEditUrl = getTinrUrl(editUrl);
				Map<String, Object> row = new HashMap<>();
				row.put("Project Name", title);
				row.put("View",  (!tinyViewUrl.equals("")) ? tinyViewUrl : viewUrl);
				row.put("Edit",  (!tinyEditUrl.equals("")) ? tinyEditUrl : editUrl);

				rows.add(row);
			}
			dataMap.put("rows", rows);

			slide.put("data", dataMap);
			slidesList.add(slide);

			response.put("slides", slidesList);
		}
		catch (Exception e){
			response.put("text", e.getMessage());
		}
		return response;
	}
	public String getTinrUrl(String url) {
		String apiEndpoint = "https://api.tinyurl.com/create";
		String accessToken = System.getenv("tinyUrlToken");

		try {
			String jsonPayload = String.format("{\"url\": \"%s\"}", url);

			HttpClient client = HttpClient.newHttpClient();

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiEndpoint))
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(response.body());
			String tinyUrl = rootNode.path("data").path("tiny_url").asText();
			return tinyUrl;
		} catch (Exception e) {
			return "";
		}
	}
//	private Button getButton(String url, String buttonType) {
//
//		Button newButton = new Button();
//		ButtonObject button = new ButtonObject();
//		button.setLabel(buttonType);
//		button.setType(BUTTON_TYPE.GREEN_OUTLINE);
//
//		Action action = new Action();
//		action.setType(ACTION_TYPE.OPEN_URL);
//
//		ActionData data = new ActionData();
//		data.setWeb(url);
//
//		action.setData(data);
//		button.setAction(action);
//
//		newButton.setButtonObject(button);
//		return newButton;
//	}
	@Override
	public List<CommandSuggestion> suggestionHandler(CommandHandlerRequest req) {
		List<CommandSuggestion> suggestionList = new ArrayList<CommandSuggestion>();
		if (req.getName().equals("catalystresource")) {
			CommandSuggestion sugg1 = CommandSuggestion.getInstance("API doc", "Catalyst API documentation", "https://www.zohowebstatic.com/sites/default/files/styles/product-home-page/public/catalyst-icon.png");
			CommandSuggestion sugg2 = CommandSuggestion.getInstance("CLI doc", "Catalyst CLI documentation", "https://www.zohowebstatic.com/sites/default/files/styles/product-home-page/public/catalyst-icon.png");
			CommandSuggestion sugg3 = CommandSuggestion.getInstance("Help doc", "Catalyst Help documentation", "https://www.zohowebstatic.com/sites/default/files/styles/product-home-page/public/catalyst-icon.png");
			suggestionList.add(sugg1);
			suggestionList.add(sugg2);
			suggestionList.add(sugg3);
		}
		return suggestionList;
	}

	private static Map<String, Object> getForm() {
		Form form = Form.getInstance();
		form.setTitle("Asset Request");
		form.setHint("Raise your asset request");
		form.setName("ID");
		form.setButtonLabel("Raise Request");
		form.setVersion(1);

		FormActionsObject actions = FormActionsObject.getInstance();
		actions.setSubmitAction("formFunctionLatest"); // ** ENTER YOUR FORM FUNCTION NAME HERE **

		form.setActions(actions);

		FormInput username = FormInput.getIntance();
		username.setType(FORM_FIELD_TYPE.TEXT);
		username.setName("username");
		username.setLabel("Name");
		username.setHint("Please enter your name");
		username.setPlaceholder("John Reese");
		username.setMandatory(true);
		username.setValue("Harold Finch");
		form.addFormInput(username);

		FormInput email = FormInput.getIntance();
		email.setType(FORM_FIELD_TYPE.TEXT);
		email.setFormat(FORM_FIELD_TEXT_FORMAT.EMAIL);
		email.setName("email");
		email.setLabel("Email");
		email.setHint("Enter your email address");
		email.setPlaceholder("johnreese@poi.com");
		email.setMandatory(true);
		email.setValue("haroldfinch@samaritan.com");
		form.addFormInput(email);

		FormInput assetType = FormInput.getIntance();
		assetType.setType(FORM_FIELD_TYPE.SELECT);
		assetType.setTriggerOnChange(true);
		assetType.setName("asset-type");
		assetType.setLabel("Asset Type");
		assetType.setHint("Choose your request asset type");
		assetType.setPlaceholder("Mobile");
		assetType.setMandatory(true);
		assetType.addOption(new FormValue("Laptop", "laptop"));
		assetType.addOption(new FormValue("Mobile", "mobile"));
		form.addFormInput(assetType);

		return ZCCliqUtil.toMap(form);
	}
}

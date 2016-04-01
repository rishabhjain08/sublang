package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

@Singleton
public class SubtitleProcessor {

	private static final String translateUrl = "https://www.googleapis.com/language/translate/v2";
	@Inject WSClient ws;
	@Inject ConfigLoader configLoader;
	
	
	public List<String> extractSubtitles (File f) throws Exception
	{
		return extractSubtitles(new FileReader(f));
	}
	
	public List<String> extractSubtitles (String content) throws Exception
	{
		return extractSubtitles(new StringReader(content));
	}

	public List<String> extractSubtitles (Reader contentReader) throws Exception
	{
		List<String> subtitles = new LinkedList<String>();
		BufferedReader reader = new BufferedReader(contentReader);
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			int subtitleIndex = Integer.parseInt(line);
			String duration = reader.readLine();
			String bits = null;
			String subtitle = "";
			while ((bits = reader.readLine()) != null && !bits.isEmpty())
			{
				subtitle += bits + "\n";
			}
			subtitles.add(subtitle);
			play.Logger.info("YOLO: " + subtitle);
		}
		reader.close();
		return subtitles;
	}
	
	public String buildSubtitleFileContent (List<String> subtitles, File f) throws Exception
	{
		return buildSubtitleFileContent(subtitles, new FileReader(f));
	}
	
	public String buildSubtitleFileContent (List<String> subtitles, String content) throws Exception
	{
		return buildSubtitleFileContent(subtitles, new StringReader(content));
	}

	public String buildSubtitleFileContent (List<String> subtitles, Reader contentReader) throws Exception
	{
		String data = "";
		String line = null;
		BufferedReader reader = new BufferedReader(contentReader);
		Iterator<String> itr = subtitles.iterator();
		while ((line = reader.readLine()) != null)
		{
			int subtitleIndex = Integer.parseInt(line);
			String duration = reader.readLine();
			String bits = null;
			while ((bits = reader.readLine()) != null && !bits.isEmpty());
			data += subtitleIndex + "\n";
			data += duration + "\n";
			data += itr.next() + "\n\n";
		}
		reader.close();
		return data;
	}
	
	public List<String> translateSubtitles (List<String> subtitles, String toLanguage) throws Exception
	{
		List<String> translatedSubtitles = null;
		List<String> allTranslatedSubtitles = new LinkedList<String>();
		int charCount = 0;
		int startIndex = 0;
		int endIndex = 0;
		int index = 0;
		for (String subtitle : subtitles)
		{
			charCount += subtitle.length();
			if (charCount > 5000 || index - startIndex > 5)
			{
				endIndex = index;
				String translateData = mergeSubtitles(subtitles.subList(startIndex, endIndex));
				String translatedData = translateSubSubtitles(translateData, toLanguage);
				translatedSubtitles = unmergeSubtitles(translatedData);
				if (translatedSubtitles == null)
					return null;
				allTranslatedSubtitles.addAll(translatedSubtitles);
				startIndex = index;
				charCount = subtitle.length();
			}
			index++;
		}
		if (startIndex < subtitles.size())
		{
			String translateData = mergeSubtitles(subtitles.subList(startIndex, subtitles.size()));
			String translatedData = translateSubSubtitles(translateData, toLanguage);
			translatedSubtitles = unmergeSubtitles(translatedData);
			if (translatedSubtitles == null)
				return null;
			allTranslatedSubtitles.addAll(translatedSubtitles);
		}
		return allTranslatedSubtitles;
	}
	
	private List<String> translateSubSubtitles (List<String> jobs, String toLanguage) throws Exception
	{
		List<String> translatedSubtitles = new LinkedList<String>();
		WSRequest request = ws.url(translateUrl);
		request.setHeader("X-HTTP-Method-Override", "GET");
		request.setQueryParameter("key", configLoader.googleApiKey);
		request.setQueryParameter("target", toLanguage);
		for (String job : jobs)
		{
			request.setQueryParameter("q", job);
		}
		Promise<WSResponse> promise = request.post("");
		WSResponse response = promise.get(10000);
		play.Logger.info("RESPONSE: " + response.getStatus() + "");
		play.Logger.info("RESPONSE: " + response.getBody() + "");
		if (response.getStatus() != 200)
			return null;
		JsonNode responseBody = response.asJson();
		if (responseBody == null 
				|| responseBody.get("data") == null
				|| responseBody.get("data").get("translations") == null)
			return null;
		ArrayNode translatedText = (ArrayNode) responseBody.get("data").get("translations");
		Iterator<JsonNode> itr = translatedText.iterator();
		for (; itr.hasNext(); )
		{
			translatedSubtitles.add(itr.next().get("translatedText").asText());
		}
		return translatedSubtitles;
	}

	private String translateSubSubtitles (String job, String toLanguage) throws Exception
	{
		WSRequest request = ws.url(translateUrl);
		request.setHeader("X-HTTP-Method-Override", "GET");
		request.setQueryParameter("key", configLoader.googleApiKey);
		request.setQueryParameter("target", toLanguage);
		request.setQueryParameter("q", job);
		Promise<WSResponse> promise = request.post("");
		WSResponse response = promise.get(10000);
		play.Logger.info("RESPONSE: " + response.getStatus() + "");
		play.Logger.info("RESPONSE: " + response.getBody() + "");
		if (response.getStatus() != 200)
			return null;
		JsonNode responseBody = response.asJson();
		if (responseBody == null 
				|| responseBody.get("data") == null
				|| responseBody.get("data").get("translations") == null)
			return null;
		ArrayNode translatedText = (ArrayNode) responseBody.get("data").get("translations");
		return translatedText.get(0).get("translatedText").asText();
	}
	
	private static String mergeSubtitles (List<String> subtitles)
	{
		String data = "";
		for (String subtitle : subtitles)
		{
			data += subtitle + "\n";
		}
		return data;
	}
	
	private static List<String> unmergeSubtitles (String data) throws Exception
	{
		List<String> subtitles = new LinkedList<String>();
		BufferedReader reader = new BufferedReader(new StringReader(data));
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			String subLine = "";
			String subtitle = line + "\n";
			while ((subLine = reader.readLine()) != null && !subLine.isEmpty())
			{
				subtitle += subLine + "\n";
			}
			subtitles.add(subtitle);
		}
		return subtitles;
	}
}

package controllers;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import play.*;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.*;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import utils.SubtitleProcessor;
import views.html.*;

public class Application extends Controller {

	@Inject SubtitleProcessor subtitleProcessor;

	public Result index() {
        return ok(index.render("Your new application is ready."));
    }

	public Result homepage() {
        return ok(homepage.render());
    }

	public Result convertSubtitleLanguage () throws Exception 
    {
    	MultipartFormData body = request().body().asMultipartFormData();
    	Map<String, String[]> params = body.asFormUrlEncoded();
    	String toLanguage = (String) params.get("to_lang")[0];
    	String subtitleData = params.get("data")[0];
    	play.Logger.info(params.keySet() + " YOLO " + toLanguage + " -- " );
    	FilePart data = body.getFile("data_file");
    	String content = null;
    	if (subtitleData != null)
    	{
    		List<String> subtitles = subtitleProcessor.extractSubtitles(subtitleData);
    		subtitles = subtitles == null ? null: subtitleProcessor.translateSubtitles(subtitles, toLanguage);
    		content = subtitles == null ? null : subtitleProcessor.buildSubtitleFileContent(subtitles, subtitleData);
    	}
    	else if (data != null)
    	{
    		File dataFile = data.getFile();
    		String dataFileName = data.getFilename();
    		List<String> subtitles = subtitleProcessor.extractSubtitles(dataFile);
    		subtitles = subtitles == null ? null: subtitleProcessor.translateSubtitles(subtitles, toLanguage);
    		content = subtitles == null ? null : subtitleProcessor.buildSubtitleFileContent(subtitles, dataFile);
    		//response().setHeader("Content-Disposition", "attachment; filename=" + toLanguage + "-" + dataFileName);
    	}
//		response().setHeader("Content-Disposition", "attachment; filename=" + toLanguage + "-" + dataFileName);
		return ok(content);
    }
}

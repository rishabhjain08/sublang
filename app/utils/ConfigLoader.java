package utils;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.Configuration;

@Singleton
public class ConfigLoader {

	public static String googleApiKey;
	
	@Inject
	public ConfigLoader (Configuration configuration)
	{
		googleApiKey = configuration.getString("prod.google.api.key");
	}
}

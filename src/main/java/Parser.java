import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author DEVIAPHAN Singleton class which works with JSON/XML parsing
 */
public class Parser {
	private static final Parser INSTANCE = new Parser();
	private JSONObject jObject;
	private XMLObject xmlObject;

	private Parser() {
		jObject = JSONObject.getInstance();
		xmlObject = XMLObject.getInstance();
	}

	/**
	 * @return a reference to a class instance
	 */
	public static Parser getInstance() {
		return INSTANCE;
	}

	/**
	 * Here point of start parsing JSON to XML
	 * 
	 * @param path path to the JSON file
	 * @return xml formed file
	 * @throws JSONException
	 * @throws IOException
	 */
	public String jsonToXML(String path) throws IOException, JSONException {
		String json = fileToString(path);
		if (json.isEmpty()) {
			throw new JSONException("JSON is empty");
		}
		ArrayList<Token> tokens = jObject.buildTokens(json);
		String xml = xmlObject.parseTokens(tokens);
		return xml;
	}

	private String fileToString(String path) throws IOException, JSONException {
		String json = null;
		File f = new File(path);
		if (f.exists() && !f.isDirectory()) {
			json = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8).replaceAll("\\s\\s+", "");
		} else {
			throw new JSONException("Path to JSON file not exists");
		}
		return json;
	}
}

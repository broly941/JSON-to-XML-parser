import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author DEVIAPHAN The class conducts XML parsing
 */
public class XMLObject {
	private static final XMLObject INSTANCE = new XMLObject();
	private final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

	private XMLObject() {}
	
	/**
	 * Returns a reference to a class instance
	 * 
	 * @return INSTANSE
	 */
	public static XMLObject getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Here point of start parsing array tokens to XML
	 * 
	 * @param tokens file-based tokens array
	 * @return xml string
	 */
	public String parseTokens(ArrayList<Token> tokens) {
		ArrayDeque<String> stackArrKeys = new ArrayDeque<>();
		StringBuilder xml = new StringBuilder();
		String key = null;
		AtomicInteger ci = new AtomicInteger();

		try {
			xml = parseAt(tokens, stackArrKeys, key, ci);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		headerRootTag(xml, tokens);
		return xml.toString();
	}

	private StringBuilder parseAt(ArrayList<Token> tokens, ArrayDeque<String> stackArrKeys, String key, AtomicInteger ci)
			throws JSONException {
		Type tokenType = tokens.get(ci.get()).getType();

		if (tokenType.equals(Type.OpenObject)) {
			ci.set(ci.get() + 1);
			return parseObj(tokens, stackArrKeys, key, ci);
		} else if (tokenType.equals(Type.OpenArray)) {
			ci.set(ci.get() + 1);
			return parseArray(tokens, stackArrKeys, key, ci);
		} else {
			throw new JSONException("Called parseAt() not in the beginning of object or array, but at " + ci.get());
		}
	}

	private StringBuilder parseObj(ArrayList<Token> tokens, ArrayDeque<String> stackArrKeys, String key, AtomicInteger ci)
			throws JSONException {
		StringBuilder xml = new StringBuilder();
		Token token;
		Type tokenType;

		while (!tokens.get(ci.get()).getType().equals(Type.CloseObject)) {
			token = tokens.get(ci.get());
			tokenType = token.getType();

			if (tokenType.equals(Type.KEY)) {
				key = token.getStr();
			} else if (tokenType.equals(Type.STR)) {
				xml.append(wrapperString(key, token.getStr()));
			} else if (tokenType.equals(Type.NUMB)) {
				xml.append(wrapperString(key, token.getStr()));
			} else if (!tokenType.equals(Type.Comma)) {
				findObjArrInParseObj(tokenType, stackArrKeys, key, xml, tokens, ci);
			}
			ci.set(ci.get() + 1);
		}
		return xml;
	}

	private void findObjArrInParseObj(Type tokenType, ArrayDeque<String> stackArrKeys, String key, StringBuilder xml, ArrayList<Token> tokens, AtomicInteger ci) throws JSONException {
		if (tokenType.equals(Type.OpenArray)) {
			stackArrKeys.push(key);
			xml.append(parseAt(tokens, stackArrKeys, key, ci));
		} else if (tokenType.equals(Type.OpenObject)) {
			wrapObjInParseObj(key, xml, tokens, stackArrKeys, ci);
		} else {
			xml.append(parseAt(tokens, stackArrKeys, key, ci));
		}
	}

	private void wrapObjInParseObj(String key, StringBuilder xml, ArrayList<Token> tokens, ArrayDeque<String> stackArrKeys, AtomicInteger ci) throws JSONException {
		String tempArrKey = key;
		xml.append(wrapOpen(tempArrKey));
		xml.append(parseAt(tokens, stackArrKeys, key, ci));
		xml.append(wrapClose(tempArrKey));
	}

	private StringBuilder parseArray(ArrayList<Token> tokens, ArrayDeque<String> stackArrKeys, String key, AtomicInteger ci)
			throws JSONException {
		StringBuilder xml = new StringBuilder();
		Token token;
		Type tokenType;

		while (!tokens.get(ci.get()).getType().equals(Type.CloseArray)) {
			token = tokens.get(ci.get());
			tokenType = token.getType();

			if (tokenType.equals(Type.NUMB)) {
				xml.append(wrapperString(key, token.getStr()));
				ci.set(ci.get() + 1);
			} else if (tokenType.equals(Type.STR)) {
				xml.append(wrapperString(key, token.getStr()));
				ci.set(ci.get() + 1);
			} else if (tokenType.equals(Type.Comma) || tokenType.equals(Type.CloseObject)) {
				ci.set(ci.get() + 1);
			} else if (tokenType.equals(Type.OpenObject)) {
				wrapObjInParseArr(xml, stackArrKeys, tokens, key, ci);
			}
		}
		stackArrKeys.pop();
		return xml;
	}

	private void wrapObjInParseArr(StringBuilder xml, ArrayDeque<String> stackArrKeys, ArrayList<Token> tokens, String key, AtomicInteger ci) throws JSONException {
		xml.append(wrapOpen(stackArrKeys.getFirst()));
		xml.append(parseAt(tokens, stackArrKeys, key, ci));
		xml.append(wrapClose(stackArrKeys.getFirst()));
	}

	private String wrapperString(String key, String value) {
		return "<" + key + ">" + value + "</" + key + ">\n";
	}

	private String wrapOpen(String key) {
		return "<" + key + ">\n";
	}

	private String wrapClose(String key) {
		return "</" + key + ">\n";
	}

	private StringBuilder headerRootTag(StringBuilder xml, ArrayList<Token> tokens) {
		if (!tokens.get(2).getType().equals(Type.OpenObject)) {
			xml.insert(0, wrapOpen("root"));
			xml.append(wrapClose("root"));
		}
		xml.insert(0, header);
		return xml;
	}
}

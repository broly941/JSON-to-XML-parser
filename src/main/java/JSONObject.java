import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author DEVIAPHAN The Singleton class conducts lexical analysis and JSON
 *         parsing
 */
public class JSONObject {
	private static final JSONObject INSTANCE = new JSONObject();

	private final String numericChrRegex = "^\\d+$";
	private final String numericStrRegex = "^(?:(?:\\-{1})?\\d+(?:\\.{1}\\d+)?)$";
	private final String spaceRegex = "^\\s$";
	private final char colon = ':';
	private final Predicate<Character> numChrPredicate = (chr) -> Character.toString(chr).matches(numericChrRegex);
	private final Predicate<String> numStrPredicate = (str) -> str.matches(numericStrRegex);
	private final Predicate<Character> spacePredicate = (chr) -> Character.toString(chr).matches(spaceRegex);
	private final Predicate<Character> minusPredicate = (chr) -> chr == '-';
	private final Predicate<Character> dotPredicae = (chr) -> chr == '.';
	private final Predicate<Character> delimeterPreciate = (chr) -> chr == ',';
	private final Predicate<Character> quotePredicate = (chr) -> chr == '\"';
	private final Predicate<Character> boolPredicate = (chr) -> {
		Pattern p = Pattern.compile("[true|false]", Pattern.CASE_INSENSITIVE);
		String str = Character.toString(chr);
		Matcher m = p.matcher(str);
		return m.matches();
	};
	private final Predicate<Character> openObjectPredicate = (chr) -> chr == '{';
	private final Predicate<Character> closeObjectPredicate = (chr) -> chr == '}';
	private final Predicate<Character> openArrayPredicate = (chr) -> chr == '[';
	private final Predicate<Character> closeArrayPredicate = (chr) -> chr == ']';

	private JSONObject() {
	}

	/**
	 * @return a reference to a class instance
	 */
	public static JSONObject getInstance() {
		return INSTANCE;
	}

	/**
	 * Lexical file analysis. The main loop in which we compare character by
	 * character and pass in the desired method
	 *
	 * @param json receive file
	 * @return file-based token array
	 * @throws JSONException
	 * @throws IOException
	 */
	public ArrayList<Token> buildTokens(String json) throws JSONException, IOException {
		ArrayList<Token> tokens = new ArrayList<>();
		ArrayList<Predicate<Character>> predicateList = new ArrayList<>();
		AtomicInteger ci = new AtomicInteger();
		char character;

		while (ci.get() < json.length()) {
			character = json.charAt(ci.get());

			if (quotePredicate.test(character)) {
				addString(json, ci, character, tokens, predicateList);
			} else if (openObjectPredicate.test(character)) {
				addOpenObject(character, ci, tokens);
			} else if (closeObjectPredicate.test(character)) {
				addCloseObject(json, ci, tokens);
			} else if (openArrayPredicate.test(character)) {
				addOpenArray(tokens, ci);
			} else if (closeArrayPredicate.test(character)) {
				addCloseArray(json, ci, character, tokens);
			} else if (delimeterPreciate.test(character)) {
				addDelimiter(json, ci, character, tokens);
			} else if (numChrPredicate.test(character) || minusPredicate.test(character)) {
				addNumeric(json, ci, character, tokens, predicateList);
			} else if (boolPredicate.test(character)) {
				addBool(json, ci, character, tokens, predicateList);
			} else if (spacePredicate.test(character)) {
				skipSpace(json, ci, character);
			} else {
				throw new JSONException("Wtf is this: " + character + " at " + ci.get());
			}
		}
		anyUnclosedObj(tokens);
		return tokens;
	}

	private void skipSpace(String json, AtomicInteger ci, char character) throws JSONException {
		int i = ci.get();
		do {
			i++;
			if (i + 1 > json.length()) {
				throw new JSONException("The object must not end with spaces at " + i);
			}
			character = json.charAt(i);
		} while (spacePredicate.test(character));
		ci.set(i);
	}

	private void addOpenObject(char character, AtomicInteger ci, ArrayList<Token> tokens) throws JSONException {
		if (ci.get() == 0 || tokens.get(tokens.size() - 1).getType().equals(Type.KEY)) {
			addToken(Type.OpenObject, Type.OpenObject.getCode(), ci, true, tokens, true);
		} else {
			addOpenObjInArr(ci, tokens);
		}
	}

	private void addOpenObjInArr(AtomicInteger ci, ArrayList<Token> tokens) throws JSONException {
		if (isInsideArr(Type.OpenObject, tokens, ci.get())) {
			addToken(Type.OpenObject, Type.OpenObject.getCode(), ci, true, tokens, true);
		} else {
			throw new JSONException("Cannot add object to array with key at " + ci);
		}
	}

	private void addCloseObject(String json, AtomicInteger ci, ArrayList<Token> tokens) throws JSONException {
		int index = ci.get();
		if (isInside(tokens, index, Type.OpenObject, Type.OpenArray, "object", true)) {
			Type lastTokenType = tokens.get(tokens.size() - 1).getType();
			int length = json.length() - 1;

			if (lastTokenType.equals(Type.KEY)) {
				throw new JSONException("it is impossible to add a close object before the key");
			} else if (index == length) {
				addToken(Type.CloseObject, Type.CloseObject.getCode(), ci, true, tokens, false);
			} else if (index + 1 <= length) {
				addTokenIfThereCloseElement(json.charAt(ci.get() + 1), ci, "close object", Type.CloseObject,
						Type.CloseObject.getCode(), tokens, true, false);
			}
		}
	}

	private void addTokenIfThereCloseElement(char character, AtomicInteger ci, String element, Type type, String value,
			ArrayList<Token> tokens, boolean inc, boolean opened) throws JSONException {
		if (delimeterPreciate.test(character) || closeArrayPredicate.test(character)
				|| closeObjectPredicate.test(character)) {
			addToken(type, value, ci, inc, tokens, opened);
		} else {
			throw new JSONException(
					"The " + element + " must be followed by a closing character \",]}\" at " + ci.get());
		}
	}

	private void addOpenArray(ArrayList<Token> tokens, AtomicInteger ci) throws JSONException {
		int index = ci.get();
		if (tokens.get(tokens.size() - 1).getType().equals(Type.KEY)) {
			if (isInside(tokens, index, Type.OpenObject, Type.OpenArray, "array", false)) {
				addToken(Type.OpenArray, Type.OpenArray.getCode(), ci, true, tokens, true);
			}
		} else {
			throw new JSONException("Missing key before array at " + index);
		}
	}

	private void addCloseArray(String json, AtomicInteger ci, char character, ArrayList<Token> tokens)
			throws JSONException {
		int index = ci.get();
		if (isInside(tokens, index, Type.OpenArray, Type.OpenObject, "array", true)) {
			if (index + 1 <= json.length() - 1) {
				addTokenIfThereCloseElement(json.charAt(index + 1), ci, "close array", Type.CloseArray,
						Type.CloseArray.getCode(), tokens, true, false);
			} else {
				throw new JSONException("Array cannot be the end of file");
			}
		}
	}

	private void addDelimiter(String json, AtomicInteger ci, char character, ArrayList<Token> tokens)
			throws JSONException {
		int index = ci.get();
		if (index + 1 > json.length() - 1) {
			throw new JSONException("Comma cannot be the end of file");
		}

		Type lastTokenType = tokens.get(tokens.size() - 1).getType();
		String nextChar = Character.toString(json.charAt(index + 1));
		String exceptionBefore = "it is impossible to add a comma before the ";
		String exceptionAfter = "it is impossible to add a comma if there is a/an ";

		if (lastTokenType.equals(Type.KEY)) {
			throw new JSONException(exceptionBefore + "key at " + index);
		} else if (lastTokenType.equals(Type.Comma)) {
			throw new JSONException(exceptionBefore + "comma at" + index);
		} else if (nextChar.equals(Type.CloseArray.getCode()) || nextChar.equals(Type.CloseObject.getCode())) {
			throw new JSONException(exceptionAfter + "close object/array" + index);
		} else if (lastTokenType.equals(Type.OpenArray) || lastTokenType.equals(Type.OpenObject)) {
			throw new JSONException(exceptionAfter + "open object/array" + index);
		}
		addToken(Type.Comma, Type.Comma.getCode(), ci, true, tokens, false);
	}

	private void addString(String json, AtomicInteger ci, char character, ArrayList<Token> tokens,
			ArrayList<Predicate<Character>> predicateList) throws JSONException {
		ci.set(ci.get() + 1);
		character = json.charAt(ci.get());

		predicateList.clear();
		predicateList.add(quotePredicate);
		String stringValue = composeStringValue(json, character, ci, predicateList, true);

		if (stringValue.isEmpty()) {
			throw new JSONException("Empty a key or value " + stringValue);
		}

		Type lastTokenType = tokens.get(tokens.size() - 1).getType();
		boolean equalKeyType = lastTokenType.equals(Type.KEY);
		int index = ci.get() + 1;
		character = json.charAt(index);

		if (character == colon) {
			addStrIfPreviouseValueKey(lastTokenType, ci, tokens, stringValue, equalKeyType);
		} else if (equalKeyType) {
			if (index <= json.length() - 1) {
				addTokenIfThereCloseElement(character, ci, "string value", Type.STR, stringValue, tokens, true, false);
			}
		} else {
			addValueIfInArr(tokens, ci, character, stringValue, Type.STR, true, "string value", Type.STR, json);
		}
	}

	private void addStrIfPreviouseValueKey(Type lastTokenType, AtomicInteger ci, ArrayList<Token> tokens,
			String stringValue, boolean equalKeyType) throws JSONException {
		if (!equalKeyType) {
			ci.set(ci.get() + 1);
			if (isInside(tokens, ci.get(), Type.OpenObject, Type.OpenArray, "key", false)) {
				addToken(Type.KEY, stringValue, ci, true, tokens, false);
			}
		} else {
			throw new JSONException(
					"Already have a key and can not add another one: " + stringValue + " at " + ci.get());
		}
	}

	private void addNumeric(String json, AtomicInteger ci, char character, ArrayList<Token> tokens,
			ArrayList<Predicate<Character>> predicateList) throws JSONException {
		predicateList.clear();
		predicateList.add(numChrPredicate);
		predicateList.add(dotPredicae);
		predicateList.add(minusPredicate);
		String stringValue = composeStringValue(json, character, ci, predicateList, false);

		int index = ci.get();
		Type lastTokenType = tokens.get(tokens.size() - 1).getType();
		boolean equalKeyType = lastTokenType.equals(Type.KEY);
		character = json.charAt(index);

		if (numStrPredicate.test(stringValue)) {
			if (equalKeyType) {
				if (index + 1 <= json.length() - 1) {
					addTokenIfThereCloseElement(character, ci, "numeric value", Type.NUMB, stringValue, tokens, false,
							false);
				}
			} else if (!equalKeyType) {
				addValueIfInArr(tokens, ci, character, stringValue, Type.NUMB, false, "numeric value", Type.NUMB, json);
			}
		} else {
			throw new JSONException("This is not a number at " + index);
		}
	}

	private void addValueIfInArr(ArrayList<Token> tokens, AtomicInteger ci, char character, String stringValue,
			Type type, boolean increment, String typeOfValue, Type innerType, String json) throws JSONException {
		int index = ci.get();
		if (isInside(tokens, index, Type.OpenArray, Type.OpenObject, "value", innerType)) {
			if (index + 1 <= json.length() - 1) {
				addTokenIfThereCloseElement(character, ci, "numeric value", Type.NUMB, stringValue, tokens, true,
						false);
			}
		} else {
			throw new JSONException(
					"The value can not be without a key and outside the array: " + stringValue + " at " + index);
		}
	}

	private void addBool(String json, AtomicInteger ci, char character, ArrayList<Token> tokens,
			ArrayList<Predicate<Character>> predicateList) throws JSONException {
		predicateList.clear();
		predicateList.add(boolPredicate);
		String stringValue = composeStringValue(json, character, ci, predicateList, false);

		int index = ci.get();
		Type lastTokenType = tokens.get(tokens.size() - 1).getType();
		boolean equalKeyType = lastTokenType.equals(Type.KEY);
		character = json.charAt(index);

		if ("true".equals(stringValue.toLowerCase()) || "false".equals(stringValue.toLowerCase())) {
			if (equalKeyType) {
				if (index + 1 <= json.length() - 1) {
					addTokenIfThereCloseElement(character, ci, "boolean value", Type.STR, stringValue, tokens, false,
							false);
				}
			} else if (!equalKeyType) {
				addValueIfInArr(tokens, ci, character, stringValue, Type.STR, false, "bool value", Type.STR, json);
			}
		} else {
			throw new JSONException("Value is not boolean: " + stringValue + " at " + index);
		}
	}

	private String composeStringValue(String json, char character, AtomicInteger ci,
			ArrayList<Predicate<Character>> predicateList, boolean string) throws JSONException {
		StringBuilder stringField = new StringBuilder();
		int index = 0;
		boolean infinity = true;
		do {
			stringField.append(character);
			ci.set(ci.get() + 1);
			index = ci.get();
			if (index + 1 > json.length()) {
				throw new JSONException("The file must not end with \" at " + index);
			}
			character = json.charAt(index);

			int length = predicateList.size() - 1;
			for (int i = 0; i <= length; i++) {
				if (string) {
					if (!predicateList.get(i).test(character)) {
						break;
					} else {
						infinity = false;
						break;
					}
				} else {
					if (predicateList.get(i).test(character)) {
						break;
					}
					if (i == length) {
						infinity = false;
					}
				}
			}

		} while (infinity);

		String stringValue = stringField.toString();
		return stringValue;
	}

	private boolean isInsideArr(Type innerType, ArrayList<Token> tokens, int ci) throws JSONException {
		int i = tokens.size() - 1;
		while (i >= 0) {
			Token token = tokens.get(i);
			if (token.getType().equals(Type.OpenArray) && token.isOpen()) {
				return setInnetType(innerType, ci, token);
			}
			i--;
		}
		throw new JSONException("Cannot add " + innerType.getCode() + " without key at " + ci);
	}

	private boolean setInnetType(Type innerType, int ci, Token token) throws JSONException {
		if (token.isInnerType(null)) {
			token.setInnerType(innerType);
			return true;
		} else if (token.isInnerType(innerType)) {
			return true;
		} else {
			throw new JSONException("Cannot add " + innerType.getCode() + " to array at " + ci);
		}
	}

	private boolean isInside(ArrayList<Token> tokens, int ci, Type firstType, Type secondType, String element,
			Type innerType) throws JSONException {
		int i = tokens.size() - 1;
		while (i >= 0) {
			Token token = tokens.get(i);
			Type tokenType = token.getType();
			boolean isOpen = token.isOpen();
			if (isOpen) {
				if (setInnerType(firstType, secondType, tokenType, ci, element, token, innerType)) {
					return true;
				} else {
					break;
				}
			}
			i--;
		}
		return false;
	}

	private boolean isInside(ArrayList<Token> tokens, int ci, Type firstType, Type secondType, String element,
			boolean close) throws JSONException {
		int i = tokens.size() - 1;
		while (i >= 0) {
			Token token = tokens.get(i);
			Type tokenType = token.getType();
			if (token.isOpen()) {
				if (tokenType.equals(firstType)) {
					if (close) {
						token.Close();
					}
					return true;
				} else {
					if (close) {
						throw new JSONException("Missing start of " + element + " at " + ci);
					}
					throw new JSONException("The " + element + " cannot be inside another array at " + ci);
				}
			}
			i--;
		}
		throw new JSONException("No open " + element + " at " + ci);
	}

	private boolean setInnerType(Type firstType, Type secondType, Type tokenType, int ci, String element, Token token,
			Type innerType) throws JSONException {
		if (tokenType.equals(firstType)) {
			return setInnetType(innerType, ci, token);
		} else {
			return false;
		}
	}

	private void anyUnclosedObj(ArrayList<Token> tokens) throws JSONException {
		int i = tokens.size() - 1;
		while (i >= 0) {
			Token token = tokens.get(i);
			if (token.getType().equals(Type.OpenObject) && token.isOpen()) {
				throw new JSONException("Unclosed object at " + i);
			} else if (token.getType().equals(Type.OpenArray) && token.isOpen()) {
				throw new JSONException("Unclosed array at " + i);
			}
			i--;
		}
	}

	private void addToken(Type type, String str, AtomicInteger ci, boolean inc, ArrayList<Token> tokens,
			boolean opened) {
		tokens.add(new Token(type, str, opened));
		if (inc != false) {
			ci.set(ci.get() + 1);
		}
	}
}
public enum Type {
	OpenObject ("{"), 
	CloseObject ("}"),
	OpenArray ("["),
	CloseArray ("]"),
	Comma (","),
	STR ("STR"),
	NUMB ("NUMB"),
	KEY ("KEY");
	
	private String code;
	
	Type (String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
}

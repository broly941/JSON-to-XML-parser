
import java.io.IOException;

/**
 * @author DEVIAPHAN
 */
public class Main {
    /**
     * It takes the path to the JSON file
     *
     * @param args command line value
     */
    public static void main(String[] args) {
        Parser parser = Parser.getInstance();

//		if (args.length != 0) {
//			String xml = null;
//			try {
//				xml = parser.jsonToXML(args[0]);
//			} catch (IOException | JSONException e) {
//				e.printStackTrace();
//			}
//			System.out.println("Parsing completed");
//		} else {
//			System.out.println("args is Empty");
//		}

        String xml = null;
        try {
            xml = parser.jsonToXML(".\\resourse\\JSONFile4");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        System.out.println(xml);
    }
}

package getQuotazioni;

//BufferedReader //FileReader //FileWriter //PrintWriter
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {

    public static void main(String[] args) throws IOException {

	// Make a URL to the web page
	// URL url = new
	// URL("http://stackoverflow.com/questions/6159118/using-java-to-pull-data-from-a-webpage");
	// URL url = new
	// URL("http://www.borsaitaliana.it/borsa/fondi/dettaglio/1FADB253518.html?lang=it");
	// URL url = new URL("http://www.calvino.ge.it/");
	// System.out.println("Arg0: " + args[0]);

	URL url = new URL(args[0]);

	// Get the input stream through URL Connection
	URLConnection con = url.openConnection();
	InputStream is = con.getInputStream();

	try {
	    String tabella = ExtractFromStream(false, "<th>Ultima", "Rendimenti", is);
	    // String tabella = readFile();
	    // importo (senza punti decimali e con il punto a separare i decimali)
	    System.out.print(ExtractVal(tabella, 1).replace(".", "").replace(',', '.'));
	    System.out.print(",");
	    // data
	    System.out.println(ExtractVal(tabella, 3));
	} catch (Exception e) {
	    System.out.println("ERROR reading from:" + args[0]);
	}
    }

    static String readFile() throws IOException {
	byte[] encoded = Files.readAllBytes(Paths.get("./tabellaReal"));
	return new String(encoded);
    }
    // <td>4,926</td> QUESTO
    // <td>4,931</td>
    // <td class="name">EUR</td>
    // <td>10/11/17</td> QUESTO

    public static String ExtractVal(String s, int n) {
	// <td>.*<\/td>
	int i = 0;
	String str = s;
	while (i < n) {
	    str = str.substring(str.indexOf("<td>") + 4);
	    i++;
	}
	return str.substring(0, str.indexOf("</td>"));
    }

    public static String ExtractFromStream(boolean include, String firstWord, String lastWord, InputStream is)
	    throws Exception {

	StringBuilder outString = new StringBuilder();
	boolean selecting = false;
	boolean searching = true;
	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	Pattern firstPattern = Pattern.compile(".*" + firstWord);
	Pattern endPattern = Pattern.compile(lastWord + ".*");
	String line = br.readLine();
	while (searching && (line != null)) {
	    if (!selecting) {
		Matcher m = firstPattern.matcher(line);
		while (m.find()) {
		    if (m.group().length() != 0) {
			String str = m.group();
			if (include) {
			    outString.append(firstWord);
			}
			selecting = true;
		    }
		}
	    }
	    if (selecting) {
		Matcher m = endPattern.matcher(line);
		while (m.find()) {
		    if (m.group().length() != 0) {
			searching = false;
			outString.append(line.substring(0, m.start()));
			if (include) {
			    outString.append(lastWord);
			}
		    }
		}
		if (searching) {
		    outString.append(line);
		}
	    }
	    line = br.readLine();
	}
	br.close();
	return outString.toString();
    }

}

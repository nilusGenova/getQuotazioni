package getQuotazioni;

//BufferedReader //FileReader //FileWriter //PrintWriter
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class App {

    // arguments:
    // inputfile outputfile
    public static void main(String[] args) {

	if ((args.length != 2)) {
	    System.out.println("ERROR wrong parameters: inputfile outputfile");
	    System.out.println("   Input file format:");
	    System.out.println("      IdQuotaz,name,href,observer,refValue,percTolerance,alertFile");
	    System.out.println("   Output file format:");
	    System.out.println("      IdQuotaz,name,quote,date");
	    return;
	}
	try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
	    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(args[1]), true));

	    // Input file format:
	    // 0:IdQuotaz 1:name 2:href 3:observer 4:refValue 5:percTolerance 6:alertFile
	    for (String line; (line = br.readLine()) != null;) {
		if (line.charAt(0) != '#') {
		    String[] array = line.split(",");
		    String quotation = getQuotation(array[2], array[0], "1".equals(array[3]),
			    Double.parseDouble(array[4]), Integer.parseInt(array[5]), array[6]);
		    // Output file format:
		    // dbId,name,quote,date
		    writer.write(array[0] + "," + array[1] + "," + quotation + "\n");
		}
	    }
	    writer.close();
	    br.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("ERROR reading from file:" + args[0]);
	}
    }

    private static String getQuotation(final String urlAddr, final String id, final boolean observer,
	    final Double refValue, final Integer percTolerance, final String alertFilename) {
	final String importo;
	final String data;

	// Make a URL to the web page
	// URL url = new
	// URL("http://stackoverflow.com/questions/6159118/using-java-to-pull-data-from-a-webpage");
	// URL url = new
	// URL("http://www.borsaitaliana.it/borsa/fondi/dettaglio/1FADB253518.html?lang=it");
	// URL url = new URL("http://www.calvino.ge.it/");
	// System.out.println("Arg0: " + args[0]);

	URL url;
	String tabella = null;
	try {
	    url = new URL(urlAddr);

	    // Get the input stream through URL Connection
	    URLConnection con = url.openConnection();
	    String redirect = con.getHeaderField("Location");
	    if (redirect != null) {
		con = new URL(redirect).openConnection();
	    }
	    InputStream is = con.getInputStream();

	    tabella = ExtractFromStream(false, "Ultima", "</table>", is); // "Rendimenti"
	    if (!"".equals(tabella)) {
		// String tabella = readFile();
		// importo (senza punti e con il punto a separare i decimali)
		importo = ExtractVal(tabella, 6).replace(".", "").replace(',', '.');
		// data
		data = ExtractVal(tabella, 9);
		if (observer) {
		    checkForAlert(id, data, importo, refValue, percTolerance, alertFilename);
		}
		return importo + "," + data;
	    } else {
		System.out.println("ERROR no extraction from:" + urlAddr);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("ERROR reading from:" + urlAddr);
	    System.out.println("table extracted from web:" + tabella);
	}
	return "0,12/12/66";
    }

    private String readFile() throws IOException {
	byte[] encoded = Files.readAllBytes(Paths.get("./tabellaReal"));
	return new String(encoded);
    }

    // <td>4,926</td> QUESTO
    // <td>4,931</td>
    // <td class="name">EUR</td>
    // <td>10/11/17</td> QUESTO
    //
    // Adesso:
    // <span class="t-text -center -size-xs"><strong>Ultima</strong></span>
    // </th>
    // <th>
    // <span class="t-text -center -size-xs"><strong>Precedente</strong></span>
    // </th>
    // <th>
    // <span class="t-text -center -size-xs"><strong>Valuta</strong></span>
    // </th>
    // <th>
    // <span class="t-text -center -size-xs"><strong>Data</strong></span>
    // </th>
    // <th>
    // <span class="t-text -center -size-xs"><strong>Variazione</strong></span>
    // </th>
    // </tr>
    // </thead>
    // <tbody>
    // <tr>
    // <td>
    // <span class="t-text -center -size-xs">116,27</span> QUESTO
    // </td>
    // <td>
    // <span class="t-text -center -size-xs">115,98</span>
    // </td>
    // <td>
    // <span class="t-text -center -size-xs">EUR</span>
    // </td>
    // <td>
    // <span class="t-text -center -size-xs">03/07/19</span> QUESTO
    // </td>
    // <td>
    // <span class="t-text -center -size-xs">+0,25</span>
    // </td>
    // </tr>
    // </tbody>
    // <td>
    private static String ExtractVal(String s, int n) {
	final String matchingPatternBegin = "<span class=\"t-text -center -size-xs\">";
	final String matchingPatternEnd = "</span>";
	// <td>.*<\/td>
	int i = 0;
	String str = s;
	while (i < n) {
	    try {
		str = str.substring(str.indexOf(matchingPatternBegin) + matchingPatternBegin.length());
	    } catch (Exception e) {
		System.out.println("ERROR extracting " + n + " from " + s);
		throw e;
	    }
	    i++;
	}
	return str.substring(0, str.indexOf(matchingPatternEnd));
    }

    private static String ExtractFromStream(boolean include, String firstWord, String lastWord, InputStream is)
	    throws Exception {

	StringBuilder outString = new StringBuilder();
	boolean selecting = false;
	boolean searching = true;
	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	Pattern firstPattern = Pattern.compile(".*" + firstWord);
	Pattern endPattern = Pattern.compile(lastWord + ".*");
	String line = br.readLine();
	while (searching && (line != null)) {
	    // System.out.println(line);
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

    // alert.txt file format:
    // each line: #id #data #val #refValue
    // #id quotazione
    // #data quotazione
    // #val valore quotazione
    // #refValue valore di riferimento
    private static void checkForAlert(final String id, final String data, final String importo, final Double refValue,
	    final Integer percTolerance, final String alertFilename) {
	final Double val = Double.parseDouble(importo);
	// System.out.print("val=" + val + " refVal=" + refValue + " diff=" +
	// Math.abs(val - refValue) + " perc="
	// + (refValue * percTolerance / 100));
	if (Math.abs(val - refValue) > (refValue * percTolerance / 100)) {
	    BufferedWriter writer = null;
	    try {
		File alertFile = new File(alertFilename);
		writer = new BufferedWriter(new FileWriter(alertFile, true));
		writer.write(id + " " + data + " " + importo + " " + refValue + "\n");
	    } catch (Exception e) {
		e.printStackTrace();
	    } finally {
		try {
		    // Close the writer regardless of what happens...
		    writer.close();
		} catch (Exception e) {
		}
	    }
	}
    }

}

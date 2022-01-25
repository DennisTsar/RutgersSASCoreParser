import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class Main {
    static String[] cores = new String[]{"AHo","AHp","AHq","AHr","CCD","CCO","HST","ITR","NS","QQ","QR","SCL","WCd","WCr","WC","W"};
    static String sep = "\t";
    public static void main(String[]args){

        String response = makeRequest("2022","1","NB");
        String parsed = parseResponse(response);
        System.out.println(parsed);
        writeToCSV(parsed,"results.csv");
    }
    public static String makeRequest(String year,String term, String campus){
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://sis.rutgers.edu/soc/api/courses.json?year="+year+"&term="+term+"&campus="+campus))
                .build();

        try {
            HttpResponse<InputStream> responseStream = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // print status code
            System.out.println(responseStream.statusCode());

            //decode response
            GZIPInputStream zip = new GZIPInputStream(responseStream.body());
            return new String(zip.readAllBytes());
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String parseResponse(String response){
        try {
            JSONArray json = (JSONArray) new JSONParser().parse(response);
            return getJSONStream(json)
                    .map(RUClass::new)
                    .filter(i -> i.getCodes().size()!=0)//Removes classes that meet no reqs
                    .map(Main::parseCodes)//Transforms into csv line
                    .collect(Collectors.joining("\n"));
        }
        catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void writeToCSV(String s,String outputFile){
        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter(outputFile));
            bf.write("Code"+sep+"Name"+sep+String.join(sep, cores));
            bf.newLine();
            bf.write(s);
            bf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String parseCodes(RUClass ruClass){
        String s = Arrays.stream(cores)
                .map(i ->(containsCode(ruClass.getCodes(),i) ? "✓":""))
                .collect(Collectors.joining(sep));
        return ruClass.getName()+sep+s;
    }
    private static boolean containsCode(JSONArray arr, String s){
        return getJSONStream(arr).anyMatch(i -> i.get("code").equals(s));
    }
    private static Stream<JSONObject> getJSONStream(JSONArray arr){
        return (Stream<JSONObject>)arr.stream();
    }

    private static class RUClass{
        String name;
        JSONArray codes;

        public RUClass(JSONObject json) {
            name = parseName(json);
            if(json.get("courseString").equals("01:198:142"))
                System.out.println(json);
            codes = (JSONArray)json.get("coreCodes");
        }

        private static String parseName(JSONObject i){
            Object fullTitle = i.get("expandedTitle");
            String fullName = fullTitle.equals("") ? (String) i.get("title") : (String) fullTitle;
            return i.get("courseString")+sep+fullName.replaceAll("\\s+", " ").trim();
        }


        public String getName() {
            return name;
        }

        public JSONArray getCodes() {
            return codes;
        }
    }
}

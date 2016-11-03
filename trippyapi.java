import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
public class trippyapi {

	private final String USER_AGENT = "";
	private static String Latitude = "";
	private static String Longitude = "";
	private static ArrayList<String> place_key = new ArrayList<String>();
	private static ArrayList<String> review_key = new ArrayList<String>();
	private static ArrayList<String> suggest_key = new ArrayList<String>();
	public static void main(String[] args) throws Exception {

		/* For testing just input the name of location and get name of resturants and reviews for those restaurants
		0 -> Get lattitude and lonitude
		1-> For searching type of places, current testing only for resturants
		2-> Get reviews about certain place you searched
		3 -> Get photo details about the place
		4 -> Place Autofill  
		*/
		// for testing purpose only
		ArrayList<String> search_type = new ArrayList<String>();
		search_type.add("restaurant");
		search_type.add("museum");
		String Location_A = "University+Dr,+College+Station,+TX";
		String Location_B = "College+Station,+TX";
		String Location_C = "New+York,+NY";
		trippyapi http = new trippyapi();
		String jsonStringLatLong = new String();
		jsonStringLatLong = http.sendGet(Location_C,0,"");
		http.formatString(jsonStringLatLong,0);
	//	System.out.println(Location_A+" :");
	//	System.out.println(Latitude);
	//	System.out.println(Longitude);
		String jsonStringplacetype = new String();
		for(String type : search_type) {
			System.out.println("********* "+type +" *************");
			place_key.clear();
			review_key.clear();	
			jsonStringplacetype = http.sendGet("",1,type);	
			http.formatString(jsonStringplacetype,1);
			for(String s : place_key) {
				if(s.contains("name")) System.out.println("=========================");
				if(s.contains("reference") && !s.contains("photo_reference")) {
					String place_ref = http.extract_placerefer(s);
					//System.out.println(place_ref);
					review_key.clear();
					String place_review = http.sendGet(place_ref,2,"");
					http.formatString(place_review,2);
					http.displayreview();	
				} else if (s.contains("photo_reference") ) {
					String photo_content = http.sendGet("",3,"");
					http.formatString(photo_content,3);
				} else {
					System.out.println(s);
				}
			}
		}
		
		// place suggestion API
		
		String Keyword = "Evans";
		jsonStringLatLong = http.sendGet(Location_B,0,"");	
		http.formatString(jsonStringLatLong,0);
		String placeautofill  = http.sendGet(Keyword,4,"");
		http.formatString(placeautofill,4);
		System.out.println("Suggestions for keyword "+Keyword+" In " + Location_B);
		for(String s : suggest_key) {
			System.out.println("-----------------------");
			System.out.println(s);
		}	
	}
	private void displayreview() {
		for(String s : review_key) {
			if(s.contains("author_name")) System.out.println("--------------------------------");
			System.out.println(s);
		}
	}	
	private String extract_placerefer(String ref) {
		ref = ref.substring(15,ref.length()-1);
		return ref;
	}
	// Format Json String
	private void formatString(String jsonString,int action) {
		String[] parts = jsonString.split(",");
		boolean location_flag=false;
		boolean review_flag = false;
		String fn_loc = "";
		for(int i=0;i<parts.length;i++) {
			parts[i]=parts[i].trim();
			if(action == 0) {
				// find lat and long for a place
				if(location_flag) {
					fn_loc+=parts[i];
					location_flag=false;
				}
				if(parts[i].contains("location")) {
					fn_loc += parts[i];
					//System.out.println(parts[i]);
					location_flag=true;
				}		
			} else if(action == 1) {
				// find place type
				if(parts[i].contains("name") || parts[i].contains("vicinity") || parts[i].contains("reference") || parts[i].contains("photo_reference") || parts[i].contains("rating")) {
					//if(parts[i].contains("name")) System.out.println("====================");
					//System.out.println(parts[i]);
					place_key.add(parts[i]);
				}
			} else if(action == 2) {
				// find place review
				//System.out.println(parts[i]);
				if(parts[i].contains("author_name")) {
					review_flag=false;	 
					review_key.add(parts[i]);
				} else if(parts[i].contains("profile_photo_url") /*|| parts[i].contains("rating") */) {
					//if(parts[i].contains("name")) System.out.println("====================");
					//System.out.println(parts[i]);
					review_key.add(parts[i]);
				} else if(parts[i].contains("text") && !parts[i].contains("weekday_text")) {	
					review_key.add(parts[i]);
					review_flag=true;
				} else if(parts[i].contains("time")) {
					String text_one = parts[i];
					int index = text_one.indexOf("time");
					text_one = text_one.substring(0,index);
					//review_key.add(text_one);	
					review_flag=false;
				}
			} else if(action == 3) {
				// find the phto content about that page
				//System.out.println(parts[i]);
			} else if(action == 4) {
				// display suggestions about the places	
				if(parts[i].contains("description")) {
					String toadd = parts[i];
					int index = toadd.indexOf("description");
					toadd = toadd.substring(index+16);	
					suggest_key.add(toadd);
				}
				//System.out.println(parts[i]);
			}
		}
		//System.out.println(fn_loc);
		if(action == 0) {
			String[] partA = fn_loc.split("}");
			String top = partA[0];
			int lat_index = top.indexOf("lat");
			int long_index = top.indexOf("lng");
			String lat = top.substring(lat_index+6,Math.min(lat_index+long_index-lat_index-1,top.length()));
			Latitude = lat.trim();
			//System.out.println(lat);	
			String lng = top.substring(long_index+6);
			Longitude = lng.trim();
			//System.out.println(lng);	
		}
	}
	// HTTP GET request
	private String sendGet(String item,int action,String type) throws Exception {

		String url="";
		if(action==0) {
			// get longitude and latitude
			url = "https://maps.googleapis.com/maps/api/geocode/json?address="+item+"&key=AIzaSyC8Nlx49SS-wHBupDoPP1iAHL2P-GqayZ8";
		} else if ( action == 1) {
			// get the name of popular places type like resturants , sports etc within particular radius 
			url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+Latitude+","+Longitude+"&radius=500&type="+type+"&keyword=&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw";

		} else if(action == 2) {
			// get review about certain place you have searched
			url = "https://maps.googleapis.com/maps/api/place/details/json?reference="+item+"&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw";
		} else if(action == 3) {
			// get photo details about the place
			url = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=CnRtAAAATLZNl354RwP_9UKbQ_5Psy40texXePv4oAlgP4qNEkdIrkyse7rPXYGd9D_Uj1rVsQdWT4oRz4QrYAJNpFX7rzqqMlZw2h2E2y5IKMUZ7ouD_SlcHxYq1yL4KbKUv3qtWgTK0A6QbGh87GB3sscrHRIQiG2RrmU_jF4tENr9wGS_YxoUSSDrYjWmrNfeEHSGSc3FyhNLlBU&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw";
		} else if(action == 4) {
			url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input="+item+"&types=&location="+Latitude+","+Longitude+"&radius=500&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw"; 
		}
		// google reviews APIs
		// get places details
		//String url = "https://maps.googleapis.com/maps/api/place/details/json?reference=CmRYAAAAciqGsTRX1mXRvuXSH2ErwW-jCINE1aLiwP64MCWDN5vkXvXoQGPKldMfmdGyqWSpm7BEYCgDm-iv7Kc2PF7QA7brMAwBbAcqMr5i1f4PwTpaovIZjysCEZTry8Ez30wpEhCNCXpynextCld2EBsDkRKsGhSLayuRyFsex6JA6NPh9dyupoTH3g&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw";
		// get latitude
		//String url = "https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=AIzaSyC8Nlx49SS-wHBupDoPP1iAHL2P-GqayZ8";
		// get places
		//String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=500&type=restaurant&keyword=cruise&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw";
		// get photos
		//"https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=CnRtAAAATLZNl354RwP_9UKbQ_5Psy40texXePv4oAlgP4qNEkdIrkyse7rPXYGd9D_Uj1rVsQdWT4oRz4QrYAJNpFX7rzqqMlZw2h2E2y5IKMUZ7ouD_SlcHxYq1yL4KbKUv3qtWgTK0A6QbGh87GB3sscrHRIQiG2RrmU_jF4tENr9wGS_YxoUSSDrYjWmrNfeEHSGSc3FyhNLlBU&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw"

		// place suggestions
	       //"https://maps.googleapis.com/maps/api/place/autocomplete/json?input=Amoeba&types=establishment&location=37.76999,-122.44696&radius=500&key=AIzaSyB3RLg8vZLUTJ2sRpM9WBQlzXpy-EbeOEw"	
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'GET' request to URL : " + url);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		//System.out.println(response.toString());
		return response.toString();

	}
}


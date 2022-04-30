package com.sunny.translation;
import android.app.Activity;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.ArrayList;

@DesignerComponent(version = 1,
        description = "Extension to translate text with Microsoft Translation API <br> Developed by Sunny Gupta",
        nonVisible = true,
        iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png",
        category = ComponentCategory.EXTENSION)
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "okhttp3.jar,okio.jar,json-simple-1.1.jar")
public class MicrosoftTranslator extends AndroidNonvisibleComponent {
    private final Activity activity;
    private String apiKey = "";
    private String resourceLocation = "";
    private final OkHttpClient client = new OkHttpClient();
    public MicrosoftTranslator(ComponentContainer container){
        super(container.$form());
        activity = container.$context();
    }
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,defaultValue = "")
    @SimpleProperty(description = "Sets  subscription api key")
    public void SubscriptionKey(String key){
        apiKey = key;
    }

    @SimpleProperty(description = "Gets subscription api key")
    public String SubscriptionKey(){
        return apiKey;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,defaultValue = "")
    @SimpleProperty(description = "Sets resource location")
    public void ResourceLocation(String location){
        resourceLocation = location;
    }

    @SimpleProperty(description = " Gets resource location")
    public String ResourceLocation(){
        return resourceLocation;
    }
    private void postError(final String functionName,final String errorMessage){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GotError(functionName, errorMessage);
            }
        });
    }
    @SimpleEvent(description = "Event raised when an error occurs")
    public void GotError(String functionName,String errorMessage){
        EventDispatcher.dispatchEvent(this,"GotError",functionName,errorMessage);
    }
    @SimpleFunction(description = "Translates given text into target language")
    public void Translate(final String srcLang,final String text,final String toLangList){
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUrl.Builder url = new HttpUrl.Builder()
                            .scheme("https")
                            .host("api.cognitive.microsofttranslator.com")
                            .addPathSegment("/translate")
                            .addQueryParameter("api-version", "3.0")
                            .addQueryParameter("from", srcLang);
                    for (String str :
                            toLangList.split(",")) {
                        url.addQueryParameter("to", str);
                    }
                    String response = post(url.build(), text);
                    final YailDictionary dictionary = new YailDictionary();
                    JSONArray array = (JSONArray) new JSONParser().parse(response);
                    JSONObject object = (JSONObject) array.get(0);
                    JSONArray array1 = (JSONArray) object.get("translations");
                    for (Object o : array1) {
                        JSONObject object1 = (JSONObject) o;
                        dictionary.put(object1.get("to"),object1.get("text"));
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GotTranslation(dictionary);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    postError("Translate",e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        });
    }
    private String post(HttpUrl url,String text) throws Exception {
        MediaType mediaType = MediaType.parse("application/json");
        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        object.put("Text",text);
        array.add(object);
        RequestBody body = RequestBody.create(mediaType,
                array.toJSONString());
        Request request = new Request.Builder().url(url).post(body)
                .addHeader("Ocp-Apim-Subscription-Key", apiKey)
                .addHeader("Ocp-Apim-Subscription-Region", resourceLocation)
                .addHeader("Content-type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
    @SimpleEvent(description = "Event raised after getting translation")
    public void GotTranslation(YailDictionary responseDictionary){
        EventDispatcher.dispatchEvent(this,"GotTranslation",responseDictionary);
    }
    @SimpleFunction(description = "Translates text with language detection")
    public void TranslateWithLangDetection(final String text,final String toLangList){
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUrl.Builder url = new HttpUrl.Builder()
                            .scheme("https")
                            .host("api.cognitive.microsofttranslator.com")
                            .addPathSegment("/translate")
                            .addQueryParameter("api-version", "3.0");
                    for (String str :
                            toLangList.split(",")) {
                        url.addQueryParameter("to", str);
                    }
                    String response = post(url.build(), text);
                    JSONArray array = (JSONArray) new JSONParser().parse(response);
                    JSONObject object = (JSONObject) array.get(0);
                    final YailDictionary dictionary = new YailDictionary();
                    JSONArray array1 = (JSONArray) object.get("translations");
                    for (Object o : array1) {
                        JSONObject object1 = (JSONObject) o;
                        dictionary.put(object1.get("to"), object1.get("text"));
                    }
                    final String[] str = new String[2];
                    JSONObject object1 = (JSONObject) object.get("detectedLanguage");
                    str[0] = (String) object1.get("language");
                    str[1] = (String) object1.get("score");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GotTranslationWithLang(str[0],str[1],dictionary);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    postError("TranslateWithLangDetection",e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        });
    }
    @SimpleEvent(description = "Event raised after getting response of 'TranslateWithLangDetection' method")
    public void GotTranslationWithLang(String language,String score,YailDictionary responseDictionary){
        EventDispatcher.dispatchEvent(this,"GotTranslationWithLang",language,score,responseDictionary);
    }
    @SimpleFunction(description = "Detects language of given text")
    public void DetectLanguage(final String text){
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUrl url = new HttpUrl.Builder()
                            .scheme("https")
                            .host("api.cognitive.microsofttranslator.com")
                            .addPathSegment("/detect")
                            .addQueryParameter("api-version", "3.0")
                            .build();
                    String response = post(url, text);
                    final String[] str = new String[3];
                    JSONArray array = (JSONArray) new JSONParser().parse(response);
                    final JSONObject object = (JSONObject) array.get(0);
                    str[0] = (String) object.get("language");
                    str[1] = (String) object.get("score");
                    final boolean bool = (boolean) object.get("isTranslationSupported");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LanguageDetected(
                                    str[0],
                                    str[1],
                                    bool);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    postError("DetectLanguage",e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        });
    }
    @SimpleEvent(description = "Event raised after detecting language ")
    public void LanguageDetected(String language,String score,boolean isTranslationSupported){
        EventDispatcher.dispatchEvent(this,"LanguageDetected",language,score,isTranslationSupported);
    }
    @SimpleFunction(description = "Gets a list of all supported languages")
    public void GetLanguagesList(){
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUrl url = new HttpUrl.Builder()
                            .scheme("https")
                            .host("api.cognitive.microsofttranslator.com")
                            .addPathSegment("/languages")
                            .addQueryParameter("api-version", "3.0")
                            .build();
                    Request request = new Request.Builder().url(url).get()
                            .build();
                    Response response = client.newCall(request).execute();
                    String res = response.body().string();
                    final ArrayList<String> namesList = new ArrayList<>();
                    final ArrayList<String> nativeNamesList = new ArrayList<>();
                    final ArrayList<String> langCodesList = new ArrayList<>();
                    JSONObject object = (JSONObject) ((JSONObject) new JSONParser().parse(res)).get("translation");
                    for (Object o : object.keySet()) {
                        String key = (String) o;
                        langCodesList.add(key);
                        JSONObject object1 = (JSONObject) object.get(key);
                        nativeNamesList.add((String) object1.get("nativeName"));
                        namesList.add((String) object1.get("name"));
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GotLanguagesList(namesList,nativeNamesList,langCodesList);
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    postError("GetLanguagesList",e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }
        });
    }
    @SimpleEvent(description = "Event raised after getting language's list")
    public void GotLanguagesList(Object namesList,Object nativeNamesList,Object langCodesList){
        EventDispatcher.dispatchEvent(this,"GotLanguagesList",namesList,nativeNamesList,langCodesList);
    }
}

package RNPasskeyModule;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import android.os.Bundle;
import android.util.Log;

import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.CreateCredentialCancellationException;
import androidx.credentials.exceptions.CreateCredentialCustomException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialInterruptedException;
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException;
import androidx.credentials.exceptions.CreateCredentialUnknownException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class PasskeyModule extends ReactContextBaseJavaModule {
    private final ReactContext mReactContext;
    private final String TAG = "KEYPASSMODULE";

    PasskeyModule(ReactApplicationContext context) {
        super(context);
        mReactContext = context;
    }

    @Override
    public String getName() {
        return "RNPasskeyModule";
    }

    @ReactMethod
    public void signIn(ReadableMap request, Promise promise) throws JSONException {
        GetPasswordOption getPasswordOption = new GetPasswordOption();
        String requestJson = toJson(request).toString();
        GetPublicKeyCredentialOption getPublicKeyCredentialOption = new GetPublicKeyCredentialOption(requestJson);
        GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(getPasswordOption)
                .addCredentialOption(getPublicKeyCredentialOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(mReactContext);
        credentialManager.getCredentialAsync(
                // Use activity based context to avoid undefined
                // system UI launching behavior
                Objects.requireNonNull(mReactContext.getCurrentActivity()),
                getCredRequest,
                null,
                Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result, promise);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "Failure"+e.getMessage());
                    }
                }
        );
    }


    public void handleSignIn(GetCredentialResponse result, Promise promise) {
        try {
            Credential credential = result.getCredential();
            if (credential instanceof PublicKeyCredential) {
                String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
                JSONObject jsonObject = new JSONObject(responseJson);
                WritableMap response = convertJsonObjectToWritableMap(jsonObject);
                promise.resolve(response);
            } else {
                promise.reject("error", "error signin");
            }
        } catch (JSONException e) {
            promise.reject("error", "error signin");
        }
    }

    @ReactMethod
    public void createCredential(ReadableMap request, Promise promise) {
        try {
            String requestJson = toJson(request).toString();
            createPasskey(requestJson, true, promise);
        } catch (JSONException e) {
            promise.reject("error", "json error");
        }
    }

    private void handlePasskeysRegister(CreateCredentialResponse result, Promise promise) {
        try {
            String registrationResponse = result.getData().getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON");
            JSONObject jsonObject = new JSONObject(registrationResponse);
            WritableMap response = convertJsonObjectToWritableMap(jsonObject);
            promise.resolve(response);
        } catch (JSONException e) {
            promise.reject("error", "errorr");
        }
    }


    public void createPasskey(String requestJson, boolean preferImmediatelyAvailableCredentials, Promise promise) {

        CredentialManager credentialManager = CredentialManager.create(mReactContext);
        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest = new CreatePublicKeyCredentialRequest(requestJson, null, preferImmediatelyAvailableCredentials);
        credentialManager.createCredentialAsync(
                Objects.requireNonNull(mReactContext.getCurrentActivity()),
                createPublicKeyCredentialRequest,
                null,
                Runnable::run,
                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                    @Override
                    public void onResult(CreateCredentialResponse result) {
                        handlePasskeysRegister(result, promise);
                    }

                    @Override
                    public void onError(CreateCredentialException e) {
                        if (e instanceof CreatePublicKeyCredentialDomException) {
                            Log.i(TAG, "Error CreatePublicKeyCredentialDomException" + ((CreatePublicKeyCredentialDomException) e).getDomError());
                            Log.i(TAG, "Error CreatePublicKeyCredentialDomException" + ((CreatePublicKeyCredentialDomException) e).getMessage());
                            promise.reject("error", "CreatePublicKeyCredentialDomException" + ((CreatePublicKeyCredentialDomException) e).getDomError().toString());
                            // Handle the passkey DOM errors thrown according to the
                            // WebAuthn spec.
                            ;
                            // handlePasskeyError(((CreatePublicKeyCredentialDomException)e).getDomError());
                        } else if (e instanceof CreateCredentialCancellationException) {
                            Log.i(TAG, "Error CreateCredentialCancellationException");
                            promise.reject("error", "CreateCredentialCancellationException");
                            // The user intentionally canceled the operation and chose not
                            // to register the credential.
                        } else if (e instanceof CreateCredentialInterruptedException) {
                            Log.i(TAG, "Error CreateCredentialInterruptedException");
                            promise.reject("error", "CreateCredentialInterruptedException");
                            // Retry-able error. Consider retrying the call.
                        } else if (e instanceof CreateCredentialProviderConfigurationException) {
                            Log.i(TAG, "Error CreateCredentialProviderConfigurationException");
                            promise.reject("error", "CreateCredentialProviderConfigurationException");
                            // Your app is missing the provider configuration dependency.
                            // Most likely, you're missing the
                            // "credentials-play-services-auth" module.
                        } else if (e instanceof CreateCredentialUnknownException) {
                            Log.i(TAG, "Error CreateCredentialUnknownException");
                            promise.reject("error", "CreateCredentialUnknownException");
                        } else if (e instanceof CreateCredentialCustomException) {
                            Log.i(TAG, "Error CreateCredentialCustomException");
                            promise.reject("error", "CreateCredentialCustomException");
                            // You have encountered an error from a 3rd-party SDK. If
                            // you make the API call with a request object that's a
                            // subclass of
                            // CreateCustomCredentialRequest using a 3rd-party SDK,
                            // then you should check for any custom exception type
                            // constants within that SDK to match with e.type.
                            // Otherwise, drop or log the exception.
                        } else {
                            Log.i(TAG, "Error Else");
                            Log.w(TAG, "Unexpected exception type " + e.getClass().getName());
                            promise.reject("error", "Unexpected exception type " + e.getClass().getName());
                        }
                    }
                }
        );
    }

    public WritableMap convertJsonObjectToWritableMap(JSONObject jsonObject) {
        WritableMap writableMap = Arguments.createMap();

        try {
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                Object value = jsonObject.get(key);

                if (value instanceof String) {
                    writableMap.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    writableMap.putInt(key, (Integer) value);
                } else if (value instanceof Double) {
                    writableMap.putDouble(key, (Double) value);
                } else if (value instanceof Boolean) {
                    writableMap.putBoolean(key, (Boolean) value);
                } else if (value instanceof JSONObject) {
                    writableMap.putMap(key, convertJsonObjectToWritableMap((JSONObject) value));
                } else if (value instanceof JSONArray) {
                    writableMap.putArray(key, convertJsonArrayToWritableArray((JSONArray) value));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return writableMap;
    }

    private WritableArray convertJsonArrayToWritableArray(JSONArray jsonArray) {
        WritableArray writableArray = Arguments.createArray();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                Object arrayItem = jsonArray.get(i);

                if (arrayItem instanceof String) {
                    writableArray.pushString((String) arrayItem);
                } else if (arrayItem instanceof Integer) {
                    writableArray.pushInt((Integer) arrayItem);
                } else if (arrayItem instanceof Double) {
                    writableArray.pushDouble((Double) arrayItem);
                } else if (arrayItem instanceof Boolean) {
                    writableArray.pushBoolean((Boolean) arrayItem);
                } else if (arrayItem instanceof JSONObject) {
                    writableArray.pushMap(convertJsonObjectToWritableMap((JSONObject) arrayItem));
                } else if (arrayItem instanceof JSONArray) {
                    writableArray.pushArray(convertJsonArrayToWritableArray((JSONArray) arrayItem));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return writableArray;
    }

    static JSONObject toJson(ReadableMap map) throws JSONException {
        JSONObject json = new JSONObject();
        ReadableMapKeySetIterator iterator = map.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();

            switch (map.getType(key)) {
                case Null:
                    json.put(key, null);
                    break;
                case Boolean:
                    json.put(key, map.getBoolean(key));
                    break;
                case Number:
                    json.put(key, map.getDouble(key));
                    break;
                case String:
                    json.put(key, map.getString(key));
                    break;
                case Map:
                    json.put(key,toJson(map.getMap(key)));
                    break;
                case Array:
                    json.put(key, toArrayList(map.getArray(key)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
            }
        }
        return json;
    }

    private static JSONArray toArrayList(ReadableArray array) throws JSONException {
        JSONArray arrayList = new JSONArray();
        for (int i = 0, size = array.size(); i < size; i++) {
            switch (array.getType(i)) {
                case Null:
                    arrayList.put(null);
                    break;
                case Boolean:
                    arrayList.put(array.getBoolean(i));
                    break;
                case Number:
                    arrayList.put(array.getDouble(i));
                    break;
                case String:
                    arrayList.put(array.getString(i));
                    break;
                case Map:
                    arrayList.put(toJson(array.getMap(i)));
                    break;
                case Array:
                    arrayList.put(toArrayList(array.getArray(i)));
                    break;
                default:
                    throw new IllegalArgumentException("Could not convert object at index: " + i + ".");
            }
        }
        return arrayList;
    }
}
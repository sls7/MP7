package edu.illinois.cs.cs125.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.Header;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.content.Intent;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech myTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button photoLibrary = findViewById(R.id.photoLibrary);
        photoLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //Log.d(TAG, "Photo library button clicked");
                startOpenFile();
            }
        });
        final Button takePhoto = findViewById(R.id.takePhoto);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //Log.d(TAG, "Take photo button clicked");
                startTakePhoto();
            }
        });
        final Button read = findViewById(R.id.read);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Log.d("fuck", "Read button clicked");
                startRead();
            }
        });
        final Button pause = findViewById(R.id.pause);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //Log.d(TAG, "Stop button clicked");
                stopReading();
            }
        });
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, 0);
    }

    private File getFile() {
        File folder = new File("sdcard/camera_app");
        if (!folder.exists()) {
            folder.mkdir();
        }
        File image_file = new File(folder, "cam_image.jpg");
        return image_file;
    }

    private void startOpenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 42);
    }

    private void startRead() {
        //String str = getAPI();
        //String textToRead = getText(getAPI());
        //Toast.makeText(getApplicationContext(), "No image selected",
                //Toast.LENGTH_LONG).show();
        myTTS.speak("hello world", TextToSpeech.QUEUE_FLUSH, null);
    }

    private void stopReading() {
        myTTS.shutdown();
    }

    String getText(String json) {
        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(json).getAsJsonObject();
        JsonObject recognitionResult = result.get("recognitionResult").getAsJsonObject();
        JsonArray lines = recognitionResult.getAsJsonArray("lines");
        JsonObject firstIndex = lines.get(0).getAsJsonObject();
        String text = firstIndex.get("text").getAsString();
        return text;
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int READ_REQUEST_CODE = 42;

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent data) {
        /*String path = "sdcard/camera_app/cam_image.jpg";
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageDrawable(Drawable.createFromPath(path));
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
        Uri currentPhotoURI = null;
        if (requestCode == READ_REQUEST_CODE) {
            currentPhotoURI = data.getData();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            currentPhotoURI = Uri.fromFile(file);
            //if (canWriteToPublicStorage) {
                addPhotoToGallery(currentPhotoURI);
            //}
        }
        loadPhoto(currentPhotoURI);*/
        super.onActivityResult(requestCode, resultCode, data);
        ImageView imageView = findViewById(R.id.imageView);
        if (resultCode == RESULT_OK) {
            try {
                final Uri imageURI = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageURI);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                System.out.println("oops");
            }
        }
        if (requestCode == 0) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, this);
            } else {
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
    }

    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            myTTS.setLanguage(Locale.US);
        }
    }

    File file = null;

    private void startTakePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = getFile();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }

    }

    private void loadPhoto(final Uri currentPhotoURI) {
        String uriScheme = currentPhotoURI.getScheme();

        byte[] imageData;
        try {
            switch (uriScheme) {
                case "file":
                    imageData = FileUtils.readFileToByteArray(new File(currentPhotoURI.getPath()));
                    break;
                case "content":
                    InputStream inputStream = getContentResolver().openInputStream(currentPhotoURI);
                    assert inputStream != null;
                    imageData = IOUtils.toByteArray(inputStream);
                    inputStream.close();
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "Unknown scheme " + uriScheme,
                            Toast.LENGTH_LONG).show();
                    return;
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error processing file",
                    Toast.LENGTH_LONG).show();
            //Log.w(TAG, "Error processing file: " + e);
        }
    }

    void addPhotoToGallery(final Uri toAdd) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(toAdd);
        this.sendBroadcast(mediaScanIntent);
        //Log.d(TAG, "Added photo to gallery: " + toAdd);
    }

    String getAPI() {
        HttpClient textClient = new DefaultHttpClient();
        HttpClient resultClient = new DefaultHttpClient();
        try {
                URI uri = new URI("https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/recognizeText?handwriting=false");
                HttpPost textRequest = new HttpPost(uri);
                textRequest.setHeader("Content-Type", "application/json");
                textRequest.setHeader("Ocp-Apim-Subscription-Key", "dc30da04031b4deaadb5a04e391ac388");
                StringEntity requestEntity = new StringEntity("{\"url\":http://htmldog.com/figures/spacingOutText.gif");
                textRequest.setEntity(requestEntity);
                HttpResponse textResponse = textClient.execute(textRequest);
                if (textResponse.getStatusLine().getStatusCode() != 202) {
                    HttpEntity entity = textResponse.getEntity();
                    String jsonString = EntityUtils.toString(entity);
                    JSONObject json = new JSONObject(jsonString);
                    System.out.println("error");
                    return "hi";
                }
                String operationLocation = null;
                Header[] responseHeaders = textResponse.getAllHeaders();
                for (Header header : responseHeaders) {
                    if (header.getName().equals("Operation-Location")) {
                        operationLocation = header.getValue();
                        break;
                    }
                }
                HttpGet resultRequest = new HttpGet(operationLocation);
                resultRequest.setHeader("Ocp-Apim-Subscription-Key", "dc30da04031b4deaadb5a04e391ac388");
                HttpResponse resultResponse = resultClient.execute(resultRequest);
                HttpEntity responseEntity = resultResponse.getEntity();

                if (responseEntity != null) {
                    try {
                        String jsonString = EntityUtils.toString(responseEntity);
                        JSONObject json = new JSONObject(jsonString);
                        System.out.println("Text recognition result response: \n");
                        System.out.println(json.toString(2));
                        Toast.makeText(getApplicationContext(), jsonString,
                                Toast.LENGTH_LONG).show();
                        return jsonString;
                    } catch (Exception e) {
                        return "error";
                    }
                }
            } catch (Exception e) {
                return "oops it didn't work";
            }
            return "i hope it doesn't reach this line";
        }
    }

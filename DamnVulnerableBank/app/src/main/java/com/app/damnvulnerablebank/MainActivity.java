package com.app.damnvulnerablebank;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MainActivity extends AppCompatActivity {

    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static String[] PERMISOS = {
            Manifest.permission.INTERNET,
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    KeyStore ks;
    private Toast toast;
    String alias = "nireGakoa";

    String apiUrl;

    private void permisos(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck!= PackageManager.PERMISSION_GRANTED){ //Ez du baimenik
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            }
            else {
                ActivityCompat.requestPermissions(this, PERMISOS, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        String mensaje = "";
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mensaje = "Baimenak eman dira";
        } else {
            mensaje = "Baimenik ez zaizu eman";

        }
        toast = Toast.makeText(this, mensaje, Toast.LENGTH_LONG);
        toast.show();
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.super.onBackPressed();
                        System.exit(0);
                    }
                }).create().show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_banklogin);
        permisos();


       boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
       FridaCheckJNI fridaCheck = new FridaCheckJNI();


       if(android.os.Debug.isDebuggerConnected()){
            Toast.makeText(getApplicationContext(), "Debug from vm",Toast.LENGTH_LONG).show();
        }

        if(EmulatorDetectortest.isEmulator()){
            Toast.makeText(getApplicationContext(), "Emulator Detected",Toast.LENGTH_LONG).show();
        }

        if(isDebuggable){
            Toast.makeText(getApplicationContext(),"Debbuger is Running", Toast.LENGTH_SHORT).show();
        }

        if(RootUtil.isDeviceRooted()) {
            Toast.makeText(getApplicationContext(), "Phone is Rooted", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Check frida

        if(fridaCheck.fridaCheck() == 1) {
            Toast.makeText(getApplicationContext(), "Frida is running", Toast.LENGTH_SHORT).show();
            Log.d("FRIDA CHECK", "FRIDA Server DETECTED");

            finish();
        } else {
            Log.d("FRIDA CHECK", "FRIDA Server NOT RUNNING");
            Toast.makeText(getApplicationContext(), "Frida is NOT running", Toast.LENGTH_SHORT).show();
        }





        SharedPreferences sharedPreferences = getSharedPreferences("jwt", Context.MODE_PRIVATE);
        boolean isloggedin=sharedPreferences.getBoolean("isloggedin", false);
        if(isloggedin)
        {
            startActivity(new Intent(getApplicationContext(), Dashboard.class));
            finish();
        }

    }



    public void loginPage(View view){
        Intent intent =new Intent(getApplicationContext(), BankLogin.class);
        startActivity(intent);
    }

    public void signupPage(View view){
        Intent intent =new Intent(getApplicationContext(), RegisterBank.class);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void gakoakSortu(String alias) {
        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 1);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder
                    (alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .build());

            KeyPair keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
    }

    public String enkriptatu(String alias) {
        try {

            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, null);
            PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();

            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, inCipher);
            cipherOutputStream.write(apiUrl.getBytes(StandardCharsets.UTF_8));
            cipherOutputStream.close();

            byte[] vals = outputStream.toByteArray();
            String encodedString = Base64.encodeToString(vals, Base64.DEFAULT);
            return encodedString;

        } catch (Exception e) {
            Toast.makeText(this, "Exceptionnnnn " + e.getMessage() + " occurred", Toast.LENGTH_LONG).show();
            return "";
        }
    }

    public void desenkriptatu(String alias) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, null);
            //RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
            PrivateKey privateKey = privateKeyEntry.getPrivateKey();

            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(apiUrl, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            apiUrl = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
    }

    public void healthCheck(View v){
        SharedPreferences pref = getApplicationContext().getSharedPreferences("apiurl", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        EditText ed=findViewById(R.id.apiurl);
        apiUrl =ed.getText().toString().trim();

        //encrypt
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                gakoakSortu(alias);
            }

            editor.putString("apiurl", enkriptatu(alias));
            editor.apply();

        }catch (Exception e){
           // http://192.168.127.17:3000
        }

        final View vButton = findViewById(R.id.healthc);
        final Button bButton = (Button) findViewById(R.id.healthc);
        RequestQueue queue = Volley.newRequestQueue(this);
        SharedPreferences sharedPreferences = getSharedPreferences("apiurl", Context.MODE_PRIVATE);
        final String url  = sharedPreferences.getString("apiurl",null);

        //decrypt
        desenkriptatu(url);



        String endpoint="/api/health/check";
        String finalurl = apiUrl+endpoint;

        try {
            // {"enc_data": "<b64>"}
            JSONObject encData = new JSONObject();
            encData.put("enc_data", "GmdBWksdEwAZFAlLVEdDX1FKS0JtQU1DHggaBkNXQQFjTkdBTUMJBgMCFQUIFA5MXUFPDxUdBg4PCkNWY05HQU1DFAYaDwgDBlhTTkUSAgwfHQcJBk9rWkkTbRw=");
//            String jsonObject = encData.get("enc_data").toString();
//            String object = jsonObject.toString();
//            Log.d("DECRYPTING: ", jsonObject);
            String decryptedString = EncryptDecrypt.decrypt(encData.get("enc_data").toString());
            JSONObject decryptedResponse = new JSONObject(decryptedString);
//            String decryptedString = EncryptData.decrypt("GmdBWksdEwAZFAlLVEdDX1FKS0JtQU1DHggaBkNXQQFjTkdBTUMJBgMCFQUIFA5MXUFPDxUdBg4PCkNWY05HQU1DFAYaDwgDBlhTTkUSAgwfHQcJBk9rWkkTbRw=");
//            JSONObject decryptedResponse = new JSONObject(decryptedString);

            Log.d("DECRYPTING: ", decryptedResponse.toString());

        } catch (JSONException e) {
            //e.printStackTrace();
        }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, finalurl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        bButton.setText("Api is Up");
                        bButton.setTextColor(Color.GREEN);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                bButton.setText("Api is Down");
                bButton.setTextColor(Color.RED);
            }
        });
        queue.add(stringRequest);
        queue.getCache().clear();

    }


}
package vn.mobileid.everification;

import vn.mobileid.everification.service.EVerification;
import vn.mobileid.everification.service.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import org.apache.commons.io.IOUtils;

public class MainClass {

    static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        boolean loop = true;
        int resultCode = 0;
        String jsonResp = null;
        JsonNode jsonNode = null;
        String accessToken = null;

        EVerification eVerification = new EVerification();
        do {
            printUsage();
            System.out.print("Choice: ");
            String choice = reader.readLine();
            if (Utils.isNumeric(choice)) {
                resultCode = Integer.parseInt(choice);
            } else {
                break;
            }
            switch (resultCode) {
                case 1:
                    jsonResp = eVerification.login();
                    System.out.println("JSON Response: " + jsonResp);
                    jsonNode = objectMapper.readTree(jsonResp);
                    accessToken = jsonNode.path("access_token").asText();
                    System.out.println("AccessToken: " + accessToken);
                    break;

                case 2:
                    byte[] fileData = IOUtils.toByteArray(new FileInputStream("file\\0.signed.file.pdf"));
                    byte[] encodedBytes = Base64.getEncoder().encode(fileData);
                    String base64document =  new String(encodedBytes);
                    
                    jsonResp = eVerification.pki_pades_Verify(base64document, accessToken);
                    
                    System.out.println("JSON Response: " + jsonResp);
                    break;
                    
                case 3:                    
                    String data = "9D23273719DFBFCD79917423AED30E58E0D61462E3ED2C3A12B75A616FE93F8B";
                    String Signature = "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";
                    
                    jsonResp = eVerification.pki_cades_Verify(false, false, data, Signature, accessToken);
                    
                    System.out.println("JSON Response: " + jsonResp);
                    break;
                    
                case 4:
                    fileData = IOUtils.toByteArray(new FileInputStream("file\\signed.sample.xml"));
                    encodedBytes = Base64.getEncoder().encode(fileData);
                    base64document =  new String(encodedBytes);
                    
                    jsonResp = eVerification.pki_xades_Verify(false, false, base64document, accessToken);
                    
                    System.out.println("JSON Response: " + jsonResp);
                    break;
                    
                case 5:                    
                    String filePath = "file\\0.signed.file.pdf";
                    jsonResp = eVerification.pki_v11_pades_Verify(false, false, filePath, accessToken);
                    
                    System.out.println("JSON Response: " + jsonResp);
                    break;
                    
                case 6:
                    filePath = "file\\signed.sample.xml";
                    jsonResp = eVerification.pki_v11_xades_Verify(false, false, filePath, accessToken);
                    
                    System.out.println("JSON Response: " + jsonResp);
                    break;

                default:
                    loop = false;
            }
        } while (loop);
        System.out.println("Exit!");
    }

    private static void printUsage() {
        System.out.println("Functions: ");
        System.out.println("1.  /oidc/token.");
        System.out.println("2.  /v1/e-verification/pades. (PDF)");
        System.out.println("3.  /v1/e-verification/cades. (CMS)");
        System.out.println("4.  /v1/e-verification/xades. (XML)");
        System.out.println("5.  /v1.1/e-verification/pades. (PDF)");
        System.out.println("6.  /v1.1/e-verification/xades. (XML)");
        System.out.println("7. quit.");

    }

}

package vn.mobileid.everification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import vn.mobileid.everification.datatypes.EverifyRequest;

public class EVerification {

    final private static String URL = "https://id-dev.mobile-id.vn/dtis";
    final private static String RELYING_PARTY = "VEIFICATION_SERVICE";
    final private static String ACCESS_KEY = "SWVDVAE8EER1OCCSS2BG";
    final private static String SECRET_KEY = "4fm8dzqdtQatfLvyWeLr9NYsvCCCgcjTk8mHxwDt";
    final private static String REGION = "vn-south-1";
    final private static String SERVICE_NAME = "dtis-20.10.05";
    final private static String XAPI_KEY = "teGPN3ewdPRGjWc7bk7VfQlTnpDfcSkAYq4uPGA5";

    //List of function
    final private static String FUNCTION_TOKEN = "/v1/e-verification/oidc/token";
    final private static String FUNCTION_PKI_PADES_VERIFY = "/v1/e-verification/pades";
    final private static String FUNCTION_PKI_CADES_VERIFY = "/v1/e-verification/cades";
    final private static String FUNCTION_PKI_XADES_VERIFY = "/v1/e-verification/xades";
    final private static String FUNCTION_PADES_1_1_VERIFY = "/v1.1/e-verification/pades";
    final private static String FUNCTION_XADES_1_1_VERIFY = "/v1.1/e-verification/xades";
    
    final private static String HTTP_METHOD_POST = "POST";
    final private static int TIMEOUT = 3000;
    final private static String CONTENT_TYPE_APP_JSON = "application/json";
    final private static String CONTENT_TYPE_APP_FORM_DATA = "multipart/form-data; boundary=";
    
    private static String BASIC_TOKEN;

    public EVerification() {
        BASIC_TOKEN = "Basic " + Base64.getEncoder().encodeToString((RELYING_PARTY + ":" + SECRET_KEY).getBytes());
    }

    public String login() throws Exception {
        String tokenUrl = URL + FUNCTION_TOKEN;
        String payload = null;
        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );

        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, BASIC_TOKEN),
                payload);

//        if (jsonResp != null) {
//            JsonNode rootNode = objectMapper.readTree(jsonResp);
//            token = rootNode.path("access_token").asText();
//        }
        return jsonResp;
    }

    public String pki_pades_Verify(String document, String accessToken) 
            throws Exception {
        
        String tokenUrl = URL + FUNCTION_PKI_PADES_VERIFY;
        String bearerToken = "Bearer " + accessToken;
        
        EverifyRequest request = new EverifyRequest();
        request.setDocument(document);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(request);
       
        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );

        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, bearerToken),
                payload);

        return jsonResp;
    }
    
    public String pki_cades_Verify(boolean signerInformation, 
            boolean certificatesInformation, 
            String data,
            String signature, 
            String accessToken) throws Exception {
        String tokenUrl = URL + FUNCTION_PKI_CADES_VERIFY;
        String bearerToken = "Bearer " + accessToken;
        
        EverifyRequest request = new EverifyRequest();
        request.setSignature(signature);
        request.setSigner_information(signerInformation);
        request.setCertificates_information(certificatesInformation);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(request);
        
        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );
        
        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, bearerToken),
                payload);

        return jsonResp;
    }
    
    public String pki_xades_Verify(boolean signerInformation, 
            boolean certificatesInformation, 
            String document, 
            String accessToken) throws Exception {
        
        String tokenUrl = URL + FUNCTION_PKI_XADES_VERIFY;
        String bearerToken = "Bearer " + accessToken;
        
        EverifyRequest request = new EverifyRequest();
        request.setDocument(document);
        request.setSigner_information(signerInformation);
        request.setCertificates_information(certificatesInformation);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(request);
        
        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_JSON,
                null
        );
        
        String jsonResp = HttpUtils.invokeHttpRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, bearerToken),
                payload);
        
        return jsonResp;
    }
    
    public String pki_v11_pades_Verify(boolean signerInformation, 
            boolean certificatesInformation, 
            String filePath, 
            String accessToken) throws Exception {
        
        String tokenUrl = URL + FUNCTION_PADES_1_1_VERIFY;
        String bearerToken = "Bearer " + accessToken;
        String boundary = "===" + System.currentTimeMillis() + "===";
        
        // Khoi tao cap <key, value> trong form-data
        File filePathPDF = new File(filePath);
        Map<String, File> fileFormData = new HashMap<>();
        fileFormData.put("document", filePathPDF);
        
        EverifyRequest request = new EverifyRequest();
        request.setLang("vn");
        request.setSigner_information(signerInformation);
        request.setCertificates_information(certificatesInformation);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(request);
        
        // Khoi tao cap <key, value> trong form-data
        Map<String, String> textFormData = new HashMap<>();
        textFormData.put("payload", payload);

        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_FORM_DATA + boundary,
                null
        );

        String jsonResp = HttpUtils.invokeHttpMutltiPartRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4AuthForFormData(payload, bearerToken, filePathPDF),
                fileFormData,
                textFormData,
                boundary);
        
        return jsonResp;
    }
    
    public String pki_v11_xades_Verify(boolean signerInformation, 
            boolean certificatesInformation, 
            String filePath, 
            String accessToken) throws Exception {
        
        String tokenUrl = URL + FUNCTION_XADES_1_1_VERIFY;
        String bearerToken = "Bearer " + accessToken;
        String boundary = "===" + System.currentTimeMillis() + "===";
        
        // Khoi tao cap <key, value> trong form-data
        File filePathPDF = new File(filePath);
        Map<String, File> fileFormData = new HashMap<>();
        fileFormData.put("document", filePathPDF);
        
        EverifyRequest request = new EverifyRequest();
        request.setLang("vn");
        request.setSigner_information(signerInformation);
        request.setCertificates_information(certificatesInformation);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String payload = objectMapper.writeValueAsString(request);
        
        // Khoi tao cap <key, value> trong form-data
        Map<String, String> textFormData = new HashMap<>();
        textFormData.put("payload", payload);

        AWSCall awsCall = new AWSCall(
                tokenUrl, //full path url
                HTTP_METHOD_POST,
                ACCESS_KEY,
                SECRET_KEY,
                REGION,
                SERVICE_NAME,
                TIMEOUT,
                XAPI_KEY,
                CONTENT_TYPE_APP_FORM_DATA + boundary,
                null
        );

        String jsonResp = HttpUtils.invokeHttpMutltiPartRequest(
                new URL(tokenUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4AuthForFormData(payload, bearerToken, filePathPDF),
                fileFormData,
                textFormData,
                boundary);
        return jsonResp;
    }
    
    
}

package vn.mobileid.everification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;

public class Session {

    static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    static ObjectMapper objectMapper = new ObjectMapper();
    
//    final private static String URL = "https://10.96.20.95/dtis";
//    final private static String RELYING_PARTY = "SMS_ATM_TEST";
//    final private static String ACCESS_KEY = "Q2H2S6YCFEL4BV4IT2LN";
//    final private static String SECRET_KEY = "ZN1t6qCWtbAosdftZbx1MiWsJ9Fcg8D5A58asJ+e";
//    final private static String REGION = "vn-south-1";
//    final private static String SERVICE_NAME = "dtis-20.10.05";
//    final private static String XAPI_KEY = "rj7JWCJUVDDMtoauNir7arZIwlvN88/rF0WtwoSr";
     
    //final private static String URL = "https://10.96.18.10/dtis";
    final private static String URL = "http://192.168.3.15/dtis";
    final private static String RELYING_PARTY = "PKI";
    final private static String ACCESS_KEY = "QN37HDCFZR3M/PWXTZRV";
    final private static String SECRET_KEY = "+VlrJMkZBov8qs9iZediSPNO1yPyqlSx/Q0CwgBg";
    final private static String REGION = "vn-south-1";
    final private static String SERVICE_NAME = "dtis-20.10.05";
    final private static String XAPI_KEY = "Q7rIkkvXpQugr34kNlTpgYPpcq1kGmOjEbOU0+Vt";
    
    final public static String CHANNEL_GROUP = "ATM";

    final private static String FUNCTION_TOKEN = "/v1/e-verification/oidc/token";
    final private static String FUNCTION_CREATE_OWNER = "/v1/owner/create";
    final private static String FUNCTION_GET_OWNER = "/v1/owner/get";
    final private static String FUNCTION_UPDATE_OWNER = "/v1/owner/update";
    final private static String FUNCTION_REGISTER_CERTIFICATE = "/v1/owner/certificate/register";
    final private static String FUNCTION_DEREGISTER_CERTIFICATE = "/v1/owner/certificate/deregister";
    final private static String FUNCTION_PKI_VERIFY = "/v1/e-verification/cades";
    final private static String FUNCTION_OTP_REQUEST = "/v1/e-verification/otp/request";
    final private static String FUNCTION_OTP_VERIFY = "/v1/e-verification/otp/verify";
    final private static String FUNCTION_CANCEL_OWNER = "/v1/owner/cancel";

    final private static String HTTP_METHOD_POST = "POST";
    final private static int TIMEOUT = 3000;
    final private static String CONTENT_TYPE_APP_JSON = "application/json";
    private static String BASIC_TOKEN;
    private static String accessToken;

    public Session() {
        BASIC_TOKEN = "Basic " + Base64.getEncoder().encodeToString((RELYING_PARTY + ":" + SECRET_KEY).getBytes());
    }

    public void createUser(int retry,
            String username,
            String group,
            String phoneNo,
            String certificate) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_CREATE_OWNER;
            String bearerToken = "Bearer " + act;

            String pyload = null;
            if (certificate != null) {
                pyload = "{\n"
                        + "    \"lang\":\"vn\",\n"
                        + "    \"username\":\"" + username + "\",\n"
                        + "    \"group\":\"" + group + "\",\n"
                        + "    \"certificate\":\"" + certificate + "\"\n"
                        + "}";
            } else {
                pyload = "{\n"
                        + "    \"lang\":\"vn\",\n"
                        + "    \"username\":\"" + username + "\",\n"
                        + "    \"group\":\"" + group + "\",\n"
                        + "    \"personal_name\":\"" + username + "\",\n"
                        + "    \"email\":\"" + "no_email@ocb.com.vn" + "\",\n"
                        + "    \"phone\":\"" + phoneNo + "\",\n"
                        + "    \"identification_type\":\"" + "PERSONAL-ID" + "\",\n"
                        + "    \"identification\":\"" + group.concat("#").concat(username) + "\"\n"
                        + "}";
            }
            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                createUser(--retry, username, group, phoneNo, certificate);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    public void updateUser(int retry,
            String username,
            String group,
            String phoneNo) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_UPDATE_OWNER;
            String bearerToken = "Bearer " + act;

            String pyload = "{\n"
                    + "   \"lang\":\"vn\",\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\",\n"
                    + "   \"phone\":\"" + phoneNo + "\"\n"
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                updateUser(--retry, username, group, phoneNo);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    public void requestOTP(int retry,
            String username,
            String group,
            String smsTemplate) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_OTP_REQUEST;
            String bearerToken = "Bearer " + act;

            String pyload = "{\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\",\n"
                    + "    \"type\":\"SMS\",\n"
                    + "    \"mobile_message\":\"" + smsTemplate + "\"\n"
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
                String transId = rootNode.path("transaction_id").asText();
                System.out.println("transaction_id: " + transId);
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                requestOTP(--retry, username, group, smsTemplate);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("19 - Unknown exception");
        }
    }

    public void verifyOTP(int retry,
            String username,
            String group,
            String otp) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_OTP_VERIFY;
            String bearerToken = "Bearer " + act;

            String pyload = "{\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\",\n"
                    + "    \"otp\":\"" + otp + "\"\n"
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
            } else if (status == 5035) {
                System.err.println("93 - OTP authentication failed");
            } else if (status == 5036) {
                System.err.println("92 - OTP authentication timeout");
            } else if (status == 5033) {
                System.err.println("60 - OTP authentication blocked");
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                verifyOTP(--retry, username, group, otp);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    public void assignCertificate(int retry,
            String username,
            String group,
            String certificate) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_REGISTER_CERTIFICATE;
            String bearerToken = "Bearer " + act;

            //process base64 cert
            certificate = certificate.replace("-----BEGIN CERTIFICATE-----", "");
            certificate = certificate.replace("-----END CERTIFICATE-----", "");
            certificate = certificate.replace("\n", "").replace("\r", "");

            String pyload = "{\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\",\n"
                    + "    \"certificate\":\"" + certificate + "\""
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                assignCertificate(--retry, username, group, certificate);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    public void deregisterCertificate(int retry,
            String username,
            String group,
            String serialNumber) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_DEREGISTER_CERTIFICATE;
            String bearerToken = "Bearer " + act;

            String pyload = "{\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\",\n"
                    + "    \"serialnumber\":\"" + serialNumber + "\""
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                deregisterCertificate(--retry, username, group, serialNumber);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    public void verifySignature(int retry,
            String username,
            String group,
            String data,
            String signature) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_PKI_VERIFY;
            String bearerToken = "Bearer " + act;

            String pyload = "{\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\",\n"
                    + "    \"data\":\"" + data + "\",\n"
                    + "    \"signature\":\"" + signature + "\""
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                JsonNode node = rootNode.path("validity_results").get(0);
                boolean signatureStatus = node.path("success").asBoolean();
                System.out.println("signature status: " + signatureStatus);
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                verifySignature(--retry, username, group, data, signature);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    public void cancelUser(int retry,
            String username,
            String group) throws Exception {
        String jsonResp;
        try {
            String act = getToken(false);
            if (Utils.isNullOrEmpty(act)) {
                throw new Exception("81 - Your login information is incorrect");
            }

            String fullUrl = URL + FUNCTION_CANCEL_OWNER;
            String bearerToken = "Bearer " + act;

            String pyload = "{\n"
                    + "    \"username\":\"" + username + "\",\n"
                    + "    \"group\":\"" + group + "\"\n"
                    + "}";

            AWSCall awsCall = new AWSCall(
                    fullUrl, //full path url
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

            jsonResp = HttpUtils.invokeHttpRequest(
                    new URL(fullUrl),
                    HTTP_METHOD_POST,
                    TIMEOUT,
                    awsCall.getAWSV4Auth(pyload, bearerToken),
                    pyload);
            if (Utils.isNullOrEmpty(jsonResp)) {
                throw new Exception("19 - Unknown exception");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResp);
            int status = rootNode.path("status").asInt();
            if (status == 0) {
                System.out.println("success");
            } else if (status == 5006
                    || status == 5007) {
                if (retry == 0) {
                    throw new Exception("81 - Your login information is incorrect");
                }
                getToken(true);
                cancelUser(--retry, username, group);
            } else {
                throw new Exception("19 - Unknown exception");
            }
        } catch (Exception e) {
            throw new Exception("19 - Unknown exception");
        }
    }

    private String token() throws Exception {
        String fullUrl = URL + FUNCTION_TOKEN;
        String payload = null;
        String token = null;
        AWSCall awsCall = new AWSCall(
                fullUrl, //full path url
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
                new URL(fullUrl),
                HTTP_METHOD_POST,
                TIMEOUT,
                awsCall.getAWSV4Auth(payload, BASIC_TOKEN),
                payload);

        if (jsonResp != null) {
            JsonNode rootNode = objectMapper.readTree(jsonResp);
            token = rootNode.path("access_token").asText();
        }
        return token;
    }

    private synchronized String getToken(boolean renewAccessToken) {
        if (renewAccessToken) {
            String act = null;
            try {
                act = token();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!Utils.isNullOrEmpty(act)) {
                accessToken = act;
            } else {
                accessToken = null;
            }
            return accessToken;
        } else {
            if (accessToken == null) {
                return getToken(true);
            } else {
                return accessToken;
            }
        }
    }
}

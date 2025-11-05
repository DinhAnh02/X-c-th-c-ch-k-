package vn.mobileid.everification.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EverifyRequest {
    
    private String lang;
    private String profile;
    private String document;
    private String signature;
    private String encoding;
    private boolean signer_information;
    private boolean certificates_information;
    private boolean signed_data_required;
    private boolean registered_constraint;

    @JsonProperty("lang")
    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @JsonProperty("profile")
    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    @JsonProperty("document")
    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    @JsonProperty("signature")
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @JsonProperty("encoding")
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @JsonProperty("signer_information")
    public boolean isSigner_information() {
        return signer_information;
    }

    public void setSigner_information(boolean signer_information) {
        this.signer_information = signer_information;
    }

    @JsonProperty("certificates_information")
    public boolean isCertificates_information() {
        return certificates_information;
    }

    public void setCertificates_information(boolean certificates_information) {
        this.certificates_information = certificates_information;
    }

    @JsonProperty("signed_data_required")
    public boolean isSigned_data_required() {
        return signed_data_required;
    }

    public void setSigned_data_required(boolean signed_data_required) {
        this.signed_data_required = signed_data_required;
    }

    @JsonProperty("registered_constraint")
    public boolean isRegistered_constraint() {
        return registered_constraint;
    }

    public void setRegistered_constraint(boolean registered_constraint) {
        this.registered_constraint = registered_constraint;
    }
    
}

package io.boomerang.model.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Features {

  @JsonProperty("notifications.enabled")
  private Boolean notificationsEnabled;

  @JsonProperty("support.enabled")
  private Boolean supportEnabled;

  @JsonProperty("docs.enabled")
  private Boolean docsEnabled;
  
  @JsonProperty("consent.enabled")
  private Boolean consentEnabled;
 
  public Boolean getConsentEnabled() {
    return consentEnabled;
  }

  public void setConsentEnabled(Boolean consentEnabled) {
    this.consentEnabled = consentEnabled;
  }

  public Boolean getNotificationsEnabled() {
    return notificationsEnabled;
  }

  public void setNotificationsEnabled(Boolean notificationsEnabled) {
    this.notificationsEnabled = notificationsEnabled;
  }

  public Boolean getSupportEnabled() {
    return supportEnabled;
  }

  public void setSupportEnabled(Boolean supportEnabled) {
    this.supportEnabled = supportEnabled;
  }

  public Boolean getDocsEnabled() {
    return docsEnabled;
  }

  public void setDocsEnabled(Boolean docsEnabled) {
    this.docsEnabled = docsEnabled;
  }

}

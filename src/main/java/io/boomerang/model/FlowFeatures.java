package io.boomerang.model;

import java.util.Map;

public class FlowFeatures {

  private Map<String, Object> features;
  private Map<String, Object> quotas;

  public Map<String, Object> getFeatures() {
    return features;
  }

  public void setFeatures(Map<String, Object> features) {
    this.features = features;
  }

  public Map<String, Object> getQuotas() {
    return quotas;
  }

  public void setQuotas(Map<String, Object> quotas) {
    this.quotas = quotas;
  }
}

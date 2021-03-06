package com.aegean.icsd.mciobjects.common.beans;

public class ProviderException extends Throwable{
  private String code;

  public ProviderException(String code, String msg) {
    super(msg);
    this.code = code;
  }

  public ProviderException(String code, String msg, Throwable cause) {
    super(msg, cause);
    this.code = code;
  }

  public String getCodeMessage() {
    return this.code + ": " + this.getMessage();
  }
}

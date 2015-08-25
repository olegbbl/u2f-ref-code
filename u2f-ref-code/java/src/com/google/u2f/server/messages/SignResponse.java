// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.messages;

import com.google.common.base.Preconditions;

import java.util.Objects;

public class SignResponse {

  /** websafe-base64(client data) */
  private final String clientData;

  /** websafe-base64(raw response from U2F device) */
  private final String signatureData;

  /** session id originally passed */
  private final String sessionId;
  
  /**
   * websafe-base64 encoding of the key handle obtained from the U2F token
   * during registration.
   */
  private final String keyHandle;

  public SignResponse(String clientData, String signatureData, String sessionId, String keyHandle) {
    this.clientData = Preconditions.checkNotNull(clientData);
    this.signatureData = Preconditions.checkNotNull(signatureData);
    this.sessionId = Preconditions.checkNotNull(sessionId);
    this.keyHandle = Preconditions.checkNotNull(keyHandle);
  }

  public String getClientData() {
    return clientData;
  }

  public String getSignatureData() {
    return signatureData;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getKeyHandle() {
    return keyHandle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientData, signatureData, sessionId, keyHandle);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SignResponse other = (SignResponse) obj;
    return Objects.equals(clientData, other.clientData)
        && Objects.equals(signatureData, other.signatureData)
        && Objects.equals(sessionId, other.sessionId)
        && Objects.equals(keyHandle, other.keyHandle);
  }
}

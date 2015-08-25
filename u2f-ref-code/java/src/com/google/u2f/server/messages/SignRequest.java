// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.messages;

import com.google.common.collect.ImmutableList;

import java.util.Objects;

public final class SignRequest {
  /**
   * Version of the protocol that the to-be-registered U2F token must speak. For
   * the version of the protocol described herein, must be "U2F_V2"
   */
  private final String version;

  /** The websafe-base64-encoded challenge. */
  private final String challenge;

  /**
   * The application id that the RP would like to assert. The U2F token will
   * enforce that the key handle provided above is associated with this
   * application id. The browser enforces that the calling origin belongs to the
   * application identified by the application id.
   */
  private final String appId;
  
  private final ImmutableList<RegisteredKey> registeredKeys;

  /**
   * A session id created by the RP. The RP can opaquely store things like
   * expiration times for the sign-in session, protocol version used, public key
   * expected to sign the identity assertion, etc. The response from the API
   * will include the sessionId. This allows the RP to fire off multiple signing
   * requests, and associate the responses with the correct request
   */
  private final String sessionId;

  public SignRequest(String version, String challenge, String appId, String sessionId,
      Iterable<RegisteredKey> registeredKeys) {
    this.version = version;
    this.challenge = challenge;
    this.appId = appId;
    this.registeredKeys = ImmutableList.copyOf(registeredKeys);
    this.sessionId = sessionId;
  }

  public String getVersion() {
    return version;
  }

  public String getChallenge() {
    return challenge;
  }

  public String getAppId() {
    return appId;
  }

  public String getSessionId() {
    return sessionId;
  }
  
  public ImmutableList<RegisteredKey> getRegisteredKeys() {
    return registeredKeys;
  }

  @Override
  public int hashCode() {
    return Objects.hash(appId, challenge, version, sessionId, registeredKeys);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SignRequest other = (SignRequest) obj;
    return Objects.equals(appId, other.appId) && Objects.equals(challenge, other.challenge)
        && Objects.equals(sessionId, other.sessionId)
        && Objects.equals(registeredKeys, other.registeredKeys);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("SignRequest [version=")
        .append(version)
        .append(", challenge=")
        .append(challenge)
        .append(", appId=")
        .append(appId)
        .append(", registeredKeys=")
        .append(registeredKeys)
        .append(", sessionId=")
        .append(sessionId)
        .append("]");
    return builder.toString();
  }
}

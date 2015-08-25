// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.u2f.TestVectors;
import com.google.u2f.U2FException;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.Crypto;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SignSessionData;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignRequest;
import com.google.u2f.server.messages.SignResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;

public class U2FServerReferenceImplTest extends TestVectors {
  @Mock ChallengeGenerator mockChallengeGenerator;
  @Mock SessionIdGenerator mockSessionIdGenerator;
  @Mock DataStore mockDataStore;
  
  private final Crypto cryto = new BouncyCastleCrypto();
  private U2FServer u2fServer;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(VENDOR_CERTIFICATE);

    when(mockChallengeGenerator.generateChallenge(ACCOUNT_NAME))
    .thenReturn(SERVER_CHALLENGE_ENROLL);
    when(mockSessionIdGenerator.generateSessionId(ACCOUNT_NAME)).thenReturn(SESSION_ID);
    when(mockDataStore.storeSessionData(Matchers.<EnrollSessionData>any())).thenReturn(SESSION_ID);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    when(mockDataStore.getSecurityKeyData(ACCOUNT_NAME)).thenReturn(
        ImmutableList.of(new SecurityKeyData(0L, KEY_HANDLE, USER_PUBLIC_KEY_SIGN_HEX, VENDOR_CERTIFICATE, 0)));
  }

  @Test
  public void testSanitizeOrigin() {
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/foo"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/foo?bar=b"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/foo#fragment"));
    assertEquals("https://example.com", U2FServerReferenceImpl.canonicalizeOrigin("https://example.com"));
    assertEquals("https://example.com", U2FServerReferenceImpl.canonicalizeOrigin("https://example.com/foo"));
  }
  
  @Test
  public void testGetRegistrationRequest() throws U2FException {
    u2fServer =
        new U2FServerReferenceImpl(mockChallengeGenerator, mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationRequest registrationRequest =
        u2fServer.getRegistrationRequest(ACCOUNT_NAME, APP_ID_ENROLL);

    assertEquals(
        new RegistrationRequest("U2F_V2", SERVER_CHALLENGE_ENROLL_BASE64, APP_ID_ENROLL, SESSION_ID,
            ImmutableList.<RegisteredKey>of(
                new RegisteredKey("U2F_V2", KEY_HANDLE_BASE64, APP_ID_ENROLL, null))),
        registrationRequest);
  }

  @Test
  public void testProcessRegistrationResponse() throws U2FException {
	when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
        new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(REGISTRATION_DATA_BASE64,
        BROWSER_DATA_ENROLL_BASE64, SESSION_ID);

    u2fServer.processRegistrationResponse(registrationResponse, 0L);

    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX, VENDOR_CERTIFICATE, 0)));
  }

  @Test
  public void testProcessRegistrationResponse2() throws U2FException {
	when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
	     new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(VENDOR_CERTIFICATE);
    trustedCertificates.add(TRUSTED_CERTIFICATE_2);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(REGISTRATION_DATA_2_BASE64,
        BROWSER_DATA_2_BASE64, SESSION_ID);

    u2fServer.processRegistrationResponse(registrationResponse, 0L);

    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, KEY_HANDLE_2, USER_PUBLIC_KEY_2, TRUSTED_CERTIFICATE_2, 0)));
  }

  @Test
  public void testGetSignRequest() throws U2FException {
    u2fServer =
        new U2FServerReferenceImpl(mockChallengeGenerator, mockDataStore, cryto, TRUSTED_DOMAINS);
    when(mockChallengeGenerator.generateChallenge(ACCOUNT_NAME)).thenReturn(SERVER_CHALLENGE_SIGN);

    SignRequest signRequest = u2fServer.getSignRequest(ACCOUNT_NAME, APP_ID_SIGN);

    assertEquals(
        new SignRequest("U2F_V2", SERVER_CHALLENGE_SIGN_BASE64, APP_ID_SIGN, SESSION_ID,
            ImmutableList.<RegisteredKey>of(
                new RegisteredKey("U2F_V2", KEY_HANDLE_BASE64, APP_ID_SIGN, null))),
        signRequest);
  }

  @Test
  public void testProcessSignResponse() throws U2FException {
    when(mockDataStore.getSignSessionData(SESSION_ID))
        .thenReturn(new SignSessionData(
            ACCOUNT_NAME, APP_ID_SIGN, SERVER_CHALLENGE_SIGN, USER_PUBLIC_KEY_SIGN_HEX));
    u2fServer =
        new U2FServerReferenceImpl(mockChallengeGenerator, mockDataStore, cryto, TRUSTED_DOMAINS);
    SignResponse signResponse = new SignResponse(
        BROWSER_DATA_SIGN_BASE64, SIGN_RESPONSE_DATA_BASE64, SESSION_ID, KEY_HANDLE_BASE64);

    u2fServer.processSignResponse(signResponse);
  }

  @Test
  public void testProcessSignResponse_badOrigin() {
    when(mockDataStore.getSignSessionData(SESSION_ID))
        .thenReturn(new SignSessionData(
            ACCOUNT_NAME, APP_ID_SIGN, SERVER_CHALLENGE_SIGN, USER_PUBLIC_KEY_SIGN_HEX));
    u2fServer = new U2FServerReferenceImpl(
        mockChallengeGenerator, mockDataStore, cryto, ImmutableSet.of("some-other-domain.com"));
    SignResponse signResponse = new SignResponse(
        BROWSER_DATA_SIGN_BASE64, SIGN_RESPONSE_DATA_BASE64, SESSION_ID, KEY_HANDLE_BASE64);

    try {
      u2fServer.processSignResponse(signResponse);
      fail("expected exception, but didn't get it");
    } catch (U2FException e) {
      assertTrue(e.getMessage().contains("is not a recognized home origin"));
    }
  }
  
  // @Test
  // TODO: put test back in once we have signature sample on a correct browserdata json
  // (currently, this test uses an enrollment browserdata during a signature)
  public void testProcessSignResponse2() throws U2FException {
    when(mockDataStore.getSignSessionData(SESSION_ID))
        .thenReturn(
            new SignSessionData(ACCOUNT_NAME, APP_ID_2, SERVER_CHALLENGE_SIGN, USER_PUBLIC_KEY_2));
    when(mockDataStore.getSecurityKeyData(ACCOUNT_NAME))
        .thenReturn(ImmutableList.of(
            new SecurityKeyData(0l, KEY_HANDLE_2, USER_PUBLIC_KEY_2, VENDOR_CERTIFICATE, 0)));
    u2fServer =
        new U2FServerReferenceImpl(mockChallengeGenerator, mockDataStore, cryto, TRUSTED_DOMAINS);
    SignResponse signResponse =
        new SignResponse(BROWSER_DATA_2_BASE64, SIGN_DATA_2_BASE64, SESSION_ID, KEY_HANDLE_BASE64);

    u2fServer.processSignResponse(signResponse);
  }
}

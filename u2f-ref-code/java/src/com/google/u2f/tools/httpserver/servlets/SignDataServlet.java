// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.tools.httpserver.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.SignRequest;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import java.io.PrintStream;

public class SignDataServlet extends JavascriptServlet {

  private final U2FServer u2fServer;

  public SignDataServlet(U2FServer u2fServer) {
    this.u2fServer = u2fServer;
  }

  @Override
  public void generateJavascript(Request req, Response resp, PrintStream body) throws Exception {
    String userName = req.getParameter("userName");
    if (userName == null) {
      resp.setStatus(Status.BAD_REQUEST);
      return;
    }

    SignRequest signRequest = u2fServer.getSignRequest(userName, "http://localhost:8080");
    JsonObject result = new JsonObject();
    result.addProperty("appId", signRequest.getAppId());
    result.addProperty("challenge", signRequest.getChallenge());
    result.addProperty("version", signRequest.getVersion());
    result.addProperty("sessionId", signRequest.getSessionId());
    JsonArray registeredKeysJson = new JsonArray();
    for(RegisteredKey registeredKey : signRequest.getRegisteredKeys()) {
      JsonObject registeredKeyJson = new JsonObject();
      registeredKeyJson.addProperty("version", registeredKey.getVersion());
      registeredKeyJson.addProperty("keyHandle", registeredKey.getKeyHandle());
      if(registeredKey.getAppId() != null) {
        registeredKeyJson.addProperty("appId", registeredKey.getAppId());
      }
      //TODO: add transports
      registeredKeysJson.add(registeredKeyJson);
    }
    result.add("registeredKeys", registeredKeysJson);
    body.println("var signData = " + result.toString() + ";");
  }
}
